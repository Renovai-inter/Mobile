package com.example.renovai.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.renovai.BuildConfig;
import com.example.renovai.MotoristaBottomNav;
import com.example.renovai.MotoristaData;
import com.example.renovai.MotoristaLocal;
import com.example.renovai.R;
import com.example.renovai.RotaEstimador;
import com.example.renovai.RotaRuas;
import com.example.renovai.RotaViagemApiService;
import com.example.renovai.controller.ChegadaParada;
import com.example.renovai.controller.ViagemRota;
import com.example.renovai.dto.response.GestorResponses;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tela 3.3 — Execução da rota no mapa. Mostra a posição do motorista, as paradas na ordem da rota
 * (concluídas, próxima e pendentes) ligadas pelo trajeto nas ruas, a distância até a próxima e
 * permite concluir cada coleta, navegar até ela (Google Maps, curva a curva) e finalizar a rota.
 * Ao chegar à próxima parada ({@link ChegadaParada}), o app a conclui sozinho, com "Desfazer".
 *
 * <p>O andamento é o da API-Redis, pelo {@link ViagemRota} — o mesmo da tela de detalhe. O mapa é o
 * Google Maps (maps.apiKey, guardada fora do Git); sem chave ou Play Services, ou se o Google não
 * carregar o mapa (chave sem o Maps SDK liberado), a tela usa o trajeto esquemático — o GPS e a
 * conclusão automática continuam valendo. O trajeto nas ruas vem do OSRM ({@link RotaRuas}); sem
 * ele, as paradas são ligadas em linha reta. (Mapas sem chave não servem de reserva: os servidores
 * do OpenStreetMap bloqueiam apps e os gratuitos alternativos exigem chave.)
 */
public class MotoristaNavegacaoActivity extends MotoristaBaseActivity {

    public static final String EXTRA_ROTA_ID = "rotaId";

    /** Tempo para o Google Maps carregar o mapa antes de trocar pelo trajeto esquemático. */
    private static final long ESPERA_GOOGLE_MS = 12_000;

    static final int VERDE = Color.parseColor("#519059");
    private static final int VINHO = Color.parseColor("#882B4E");
    static final int CINZA = Color.parseColor("#8A8A8A");

    private String rotaId;
    private GestorResponses.Rota rota;
    private final List<GestorResponses.RotaEnd> ends = new ArrayList<>();
    /** Posição de cada parada de {@link #ends} (null = endereço não localizado). */
    private List<double[]> coords;
    private boolean geocodificando;

    private ViagemRota viagem;

    /** null até o mapa ficar pronto (e sem mapa: trajeto esquemático). */
    private MapaRota mapa;
    private boolean googleCarregou;
    /** Trajeto nas ruas entre paradas localizadas consecutivas (null = linha reta). */
    private List<List<double[]>> trechos;
    private boolean buscandoTrechos;
    private String desenhado; // assinatura do último desenho: evita redesenhar à toa
    private boolean enquadrou;
    private final ChegadaParada chegada = new ChegadaParada();

    private FusedLocationProviderClient localizacao;
    private double[] posicao;
    private float precisao;
    private boolean permissaoNegada, recebendoPosicao;
    private ConnectivityManager.NetworkCallback rede;

    private final LocationCallback aoMover =
            new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult r) {
                    if (r.getLastLocation() == null || !vivo()) return;
                    novaPosicao(r.getLastLocation());
                }
            };

    private final ActivityResultLauncher<String[]> pedirLocalizacao =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    r -> {
                        permissaoNegada = !temPermissao();
                        if (permissaoNegada)
                            toast("Sem a localização o mapa mostra só as paradas. Você ainda pode"
                                    + " cumprir a rota.");
                        ligarLocalizacao();
                        render();
                    });

    public static void abrir(Context c, String rotaId) {
        c.startActivity(new Intent(c, MotoristaNavegacaoActivity.class).putExtra(EXTRA_ROTA_ID, rotaId));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_motorista_navegacao, MotoristaBottomNav.Aba.NENHUMA)) return;
        titulo("Rota no mapa");
        rotaId = getIntent().getStringExtra(EXTRA_ROTA_ID);
        if (rotaId == null || rotaId.trim().isEmpty()) {
            toast("Rota não informada.");
            finish();
            return;
        }
        viagem = new ViagemRota(RotaViagemApiService.criar(), rotaId, funcId(),
                MotoristaLocal.pendencias(this), new ViagemRota.Ouvinte() {
                    @Override
                    public void mudou() {
                        if (vivo()) render();
                    }

                    @Override
                    public void aviso(String m) {
                        if (vivo()) toast(m);
                    }

                    @Override
                    public void finalizou() {
                        MotoristaLocal.marcarFinalizada(MotoristaNavegacaoActivity.this, rotaId);
                        toast("Rota finalizada.");
                    }
                });
        localizacao = LocationServices.getFusedLocationProviderClient(this);
        prepararMapa(b);

        findViewById(R.id.btnCentralizarNav).setOnClickListener(v -> enquadrar(true));
        findViewById(R.id.btnParadasNav).setOnClickListener(v -> mostrarParadas());
        findViewById(R.id.btnNavegarNav).setOnClickListener(v -> navegar(viagem.proxima()));
        findViewById(R.id.btnConcluirNav).setOnClickListener(v -> concluirProxima());
        findViewById(R.id.btnIniciarNav).setOnClickListener(v -> viagem.iniciar());
        findViewById(R.id.btnFinalizarNav).setOnClickListener(v -> confirmarFinalizar());
        render();
    }

    /** Google Maps quando há chave e Play Services; senão (ou se ele não carregar) o trajeto esquemático. */
    private void prepararMapa(Bundle b) {
        boolean temChave = !BuildConfig.MAPS_API_KEY.trim().isEmpty();
        boolean temPlay = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
                == ConnectionResult.SUCCESS;
        if (!temChave || !temPlay) {
            usarEsquema();
            return;
        }
        SupportMapFragment f = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.containerMapaNav);
        if (f == null) {
            f = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().replace(R.id.containerMapaNav, f).commitNow();
        }
        f.getMapAsync(m -> {
            if (!vivo() || mapa != null) return;
            // chave sem o Maps SDK liberado: o SDK só registra "Authorization failure" e o mapa
            // nunca termina de carregar — passado o prazo, usa o trajeto esquemático
            m.setOnMapLoadedCallback(() -> googleCarregou = true);
            mapaPronto(new MapaGoogle(m, findViewById(R.id.containerMapaNav), this::detalhe));
            findViewById(R.id.containerMapaNav).postDelayed(() -> {
                if (vivo() && !googleCarregou && mapa != null) trocarParaEsquema();
            }, ESPERA_GOOGLE_MS);
        });
    }

    private void trocarParaEsquema() {
        SupportMapFragment f = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.containerMapaNav);
        if (f != null) getSupportFragmentManager().beginTransaction().remove(f).commitNowAllowingStateLoss();
        mapa.destruir();
        mapa = null;
        usarEsquema();
    }

    /** Sem mapa: trajeto esquemático; a rota continua podendo ser cumprida (e concluída sozinha). */
    private void usarEsquema() {
        findViewById(R.id.containerMapaNav).setVisibility(View.GONE);
        findViewById(R.id.btnCentralizarNav).setVisibility(View.GONE);
        findViewById(R.id.mapaEsquemaNav).setVisibility(View.VISIBLE);
        render();
    }

    private void mapaPronto(MapaRota m) {
        mapa = m;
        mapa.retomar();
        ligarPontoAzul();
        if (posicao != null) mapa.posicao(posicao);
        desenhado = null;
        enquadrou = false;
        render();
        enquadrar(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viagem == null) return;
        if (mapa != null) mapa.retomar();
        MotoristaData.api().rota(rotaId).enqueue(new Callback<GestorResponses.Rota>() {
            @Override
            public void onResponse(Call<GestorResponses.Rota> c, Response<GestorResponses.Rota> r) {
                if (!vivo()) return;
                if (!r.isSuccessful() || r.body() == null) {
                    toast(com.example.renovai.EmpresaData.erro(r));
                    return;
                }
                if (coopId() == null || !coopId().equalsIgnoreCase(r.body().cooperativaId)) {
                    toast("Rota de outra cooperativa.");
                    finish();
                    return;
                }
                receberRota(r.body());
            }

            @Override
            public void onFailure(Call<GestorResponses.Rota> c, Throwable t) {
                if (vivo()) toast("Sem conexão: não foi possível atualizar a rota.");
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (viagem == null) return;
        if (!temPermissao() && !permissaoNegada) {
            pedirLocalizacao.launch(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            ligarLocalizacao();
        }
        // ao voltar a internet, manda as coletas que ficaram guardadas
        ConnectivityManager cm = getSystemService(ConnectivityManager.class);
        if (cm != null && rede == null) {
            rede = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network n) {
                    runOnUiThread(() -> {
                        if (vivo() && viagem.pendentesDeEnvio() > 0) viagem.carregar();
                    });
                }
            };
            cm.registerDefaultNetworkCallback(rede);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (localizacao != null && recebendoPosicao) {
            localizacao.removeLocationUpdates(aoMover);
            recebendoPosicao = false;
        }
        ConnectivityManager cm = getSystemService(ConnectivityManager.class);
        if (cm != null && rede != null) {
            cm.unregisterNetworkCallback(rede);
            rede = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapa != null) mapa.pausar();
    }

    @Override
    protected void onDestroy() {
        if (viagem != null) viagem.encerrar();
        if (mapa != null) mapa.destruir();
        super.onDestroy();
    }

    // ───────────────────────────── rota e localização ─────────────────────────────

    private static boolean ehExtremo(GestorResponses.RotaEnd e) {
        return "INICIO".equalsIgnoreCase(e.tipoLocal) || "FIM".equalsIgnoreCase(e.tipoLocal);
    }

    private void receberRota(GestorResponses.Rota r) {
        rota = r;
        List<GestorResponses.RotaEnd> lista =
                new ArrayList<>(r.enderecos == null ? new ArrayList<>() : r.enderecos);
        lista.sort((a, b) -> Integer.compare(
                a.ordem == null ? Integer.MAX_VALUE : a.ordem,
                b.ordem == null ? Integer.MAX_VALUE : b.ordem));
        boolean mudouParadas = !mesmaLista(lista);
        ends.clear();
        ends.addAll(lista);
        List<ViagemRota.Coleta> coletas = new ArrayList<>();
        for (GestorResponses.RotaEnd e : ends)
            if (!ehExtremo(e))
                coletas.add(new ViagemRota.Coleta(GestorRotaTexto.endereco(e), e.enderecoId, e.ordem));
        viagem.setColetas(coletas);
        titulo(r.nome == null ? "Rota no mapa" : r.nome);
        if (mudouParadas) {
            coords = null;
            trechos = null;
            desenhado = null;
            enquadrou = false;
            geocodificar();
        }
        viagem.carregar();
        render();
    }

    private boolean mesmaLista(List<GestorResponses.RotaEnd> nova) {
        if (nova.size() != ends.size()) return false;
        for (int i = 0; i < nova.size(); i++)
            if (!GestorRotaTexto.consulta(nova.get(i)).equals(GestorRotaTexto.consulta(ends.get(i))))
                return false;
        return true;
    }

    private void geocodificar() {
        if (geocodificando || ends.isEmpty()) return;
        geocodificando = true;
        List<String> qs = new ArrayList<>();
        for (GestorResponses.RotaEnd e : ends) qs.add(GestorRotaTexto.consulta(e));
        RotaEstimador.coordenadas(this, qs, pontos -> {
            geocodificando = false;
            if (!vivo() || pontos.size() != ends.size()) return;
            coords = pontos;
            desenhado = null;
            render();
            enquadrar(false);
            buscarTrechos();
        });
    }

    /** Paradas localizadas, na ordem da rota. */
    private List<double[]> localizadas() {
        List<double[]> l = new ArrayList<>();
        if (coords != null) for (double[] c : coords) if (c != null) l.add(c);
        return l;
    }

    private void buscarTrechos() {
        List<double[]> pontos = localizadas();
        if (buscandoTrechos || pontos.size() < 2) return;
        buscandoTrechos = true;
        RotaRuas.buscar(pontos, t -> {
            buscandoTrechos = false;
            if (!vivo() || coords == null || localizadas().size() != pontos.size()) return;
            trechos = t;
            desenhado = null;
            render();
        });
    }

    private boolean temPermissao() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private void ligarPontoAzul() {
        if (mapa != null && temPermissao()) mapa.localizacaoPermitida();
    }

    /**
     * A cada 5 s, mesmo parado (a chegada à parada precisa de leituras seguidas): distância até a
     * próxima parada e conclusão automática. Desligado fora da tela.
     */
    @SuppressLint("MissingPermission")
    private void ligarLocalizacao() {
        ligarPontoAzul();
        if (!temPermissao() || recebendoPosicao) return;
        LocationRequest pedido = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build();
        localizacao.requestLocationUpdates(pedido, aoMover, Looper.getMainLooper());
        recebendoPosicao = true;
        localizacao.getLastLocation().addOnSuccessListener(l -> {
            if (l == null || !vivo() || posicao != null) return;
            posicao = new double[] {l.getLatitude(), l.getLongitude()};
            if (mapa != null) mapa.posicao(posicao);
            atualizarProxima(); // posição antiga: não conta para a chegada
        });
    }

    private void novaPosicao(Location l) {
        posicao = new double[] {l.getLatitude(), l.getLongitude()};
        precisao = l.hasAccuracy() ? l.getAccuracy() : 0;
        if (mapa != null) mapa.posicao(posicao);
        verificarChegada();
        atualizarProxima();
    }

    /** Chegou à próxima parada: conclui sozinho e oferece "Desfazer". */
    private void verificarChegada() {
        int k = viagem.podeMexer() ? viagem.proxima() : -1;
        double[] c = k < 0 ? null : coord(posicaoNaRota(k));
        if (c == null) {
            chegada.posicao(-1, 0, 0);
            return;
        }
        if (!chegada.posicao(k, RotaEstimador.metros(posicao, c), precisao)) return;
        int numero = posicaoNaRota(k) + 1;
        viagem.definir(k, RotaViagemApiService.CONCLUIDA);
        Snackbar.make(findViewById(android.R.id.content),
                        "Você chegou à parada " + numero + ": coleta concluída.", 8000)
                .setAction("Desfazer", v -> {
                    chegada.desfeita(k);
                    if (viagem.podeMexer()) viagem.definir(k, RotaViagemApiService.PENDENTE);
                })
                .show();
    }

    /** Índice em {@link #ends} da coleta {@code indiceColeta} (índice da API-Redis). */
    private int posicaoNaRota(int indiceColeta) {
        int k = 0;
        for (int i = 0; i < ends.size(); i++) {
            if (ehExtremo(ends.get(i))) continue;
            if (k++ == indiceColeta) return i;
        }
        return -1;
    }

    /** Índice da API-Redis da parada {@code i} de {@link #ends} (-1 para partida e retorno). */
    private int indiceColeta(int i) {
        if (i < 0 || i >= ends.size() || ehExtremo(ends.get(i))) return -1;
        int k = 0;
        for (int j = 0; j < i; j++) if (!ehExtremo(ends.get(j))) k++;
        return k;
    }

    private double[] coord(int i) {
        return coords == null || i < 0 || i >= coords.size() ? null : coords.get(i);
    }

    // ───────────────────────────── ações ─────────────────────────────

    private void concluirProxima() {
        int prox = viagem.proxima();
        if (prox < 0 || !viagem.podeMexer()) return;
        viagem.definir(prox, RotaViagemApiService.CONCLUIDA);
        int seguinte = viagem.proxima();
        double[] c = coord(posicaoNaRota(seguinte));
        if (mapa != null && c != null) mapa.centralizar(c);
    }

    /** Navegação curva a curva do Google Maps até a parada (coordenada ou, sem ela, o endereço). */
    private void navegar(int indiceColeta) {
        int i = posicaoNaRota(indiceColeta);
        if (i < 0) {
            // tudo concluído: navega para o retorno, se houver
            i = ends.isEmpty() ? -1 : ends.size() - 1;
            if (i < 0) {
                toast("Rota sem paradas cadastradas.");
                return;
            }
        }
        double[] c = coord(i);
        String destino = c != null ? c[0] + "," + c[1] : GestorRotaTexto.consulta(ends.get(i));
        Intent nav = new Intent(Intent.ACTION_VIEW,
                Uri.parse("google.navigation:q=" + Uri.encode(destino) + "&mode=d"))
                .setPackage("com.google.android.apps.maps");
        try {
            startActivity(nav);
        } catch (Exception semApp) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                        "https://www.google.com/maps/dir/?api=1&travelmode=driving&destination="
                                + Uri.encode(destino))));
            } catch (Exception e) {
                toast("Não foi possível abrir o app de mapas.");
            }
        }
    }

    private void confirmarFinalizar() {
        if (!viagem.podeMexer()) return;
        if (viagem.pendentesDeEnvio() > 0) {
            viagem.finalizar(); // tenta enviar e avisa
            return;
        }
        int pendentes = viagem.totalColetas() - viagem.concluidas();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Finalizar rota")
                .setMessage(pendentes > 0
                        ? "Ainda há " + pendentes
                                + (pendentes == 1 ? " coleta pendente" : " coletas pendentes")
                                + ". Finalizar mesmo assim?"
                        : "Todas as coletas foram concluídas. Finalizar a rota?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Finalizar", (d, w) -> viagem.finalizar())
                .show();
    }

    /** Informações da parada {@code i} de {@link #ends}, com as ações possíveis. */
    private void detalhe(int i) {
        if (i < 0 || i >= ends.size()) return;
        GestorResponses.RotaEnd e = ends.get(i);
        int k = indiceColeta(i);
        StringBuilder sb = new StringBuilder();
        sb.append(GestorRotaTexto.endereco(e));
        String cidade = e.cidade == null || e.cidade.isEmpty() ? "" : e.cidade;
        if (!cidade.isEmpty()) sb.append("\n").append(cidade);
        sb.append("\n\nTipo: ").append(k < 0
                ? ("INICIO".equalsIgnoreCase(e.tipoLocal) ? "Ponto de partida" : "Retorno à cooperativa")
                : "Coleta");
        sb.append("\nOrdem na rota: ").append(i + 1).append(" de ").append(ends.size());
        if (k >= 0 && comColetas()) {
            sb.append("\nSituação: ").append(situacao(k));
        }
        double[] c = coord(i);
        if (coords != null && c == null) sb.append("\n\nEndereço não localizado no mapa.");
        else if (c != null && posicao != null)
            sb.append("\nDistância: ").append(RotaEstimador.textoDistancia(RotaEstimador.metros(posicao, c)));

        MaterialAlertDialogBuilder d = new MaterialAlertDialogBuilder(this)
                .setTitle("Parada " + (i + 1))
                .setMessage(sb.toString())
                .setNeutralButton("Fechar", null)
                .setNegativeButton("Navegar", (x, w) -> {
                    if (k >= 0) navegar(k);
                    else navegarPara(i);
                });
        if (k >= 0 && viagem.podeMexer()) {
            boolean feita = viagem.concluida(k);
            d.setPositiveButton(feita ? "Desmarcar" : "Concluir", (x, w) -> viagem.alternar(k));
        }
        d.show();
    }

    private void navegarPara(int i) {
        double[] c = coord(i);
        String destino = c != null ? c[0] + "," + c[1] : GestorRotaTexto.consulta(ends.get(i));
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://www.google.com/maps/dir/?api=1&travelmode=driving&destination="
                            + Uri.encode(destino))));
        } catch (Exception e) {
            toast("Não foi possível abrir o app de mapas.");
        }
    }

    private String situacao(int k) {
        if (viagem.aguardandoEnvio(k))
            return (viagem.concluida(k) ? "Concluída" : "Pendente") + " (aguardando envio)";
        if (viagem.concluida(k)) {
            String h = hora(viagem.conclusao(k));
            return "Concluída" + (h != null ? " às " + h : "");
        }
        return "Pendente";
    }

    /** Lista de todas as paradas (toque abre o detalhe). */
    private void mostrarParadas() {
        if (ends.isEmpty()) {
            toast("Rota sem paradas cadastradas.");
            return;
        }
        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        raiz.setPadding(pad, pad / 2, pad, 0);
        androidx.appcompat.app.AlertDialog[] dialogo = new androidx.appcompat.app.AlertDialog[1];
        int prox = posicaoNaRota(viagem.proxima());
        for (int i = 0; i < ends.size(); i++) {
            GestorResponses.RotaEnd e = ends.get(i);
            int k = indiceColeta(i);
            View v = LayoutInflater.from(this).inflate(R.layout.item_motorista_parada, raiz, false);
            TextView num = v.findViewById(R.id.txtParadaMNumero);
            num.setText(String.valueOf(i + 1));
            boolean feita = k >= 0 && comColetas() && viagem.concluida(k);
            num.setBackgroundResource(feita ? R.drawable.avatar_green_background
                    : i == prox && viagem.emAndamento() ? R.drawable.avatar_maroon_background
                    : R.drawable.stop_outline_green);
            if (!feita && !(i == prox && viagem.emAndamento())) num.setTextColor(VERDE);
            String rotulo = k < 0
                    ? ("INICIO".equalsIgnoreCase(e.tipoLocal) ? "Ponto de Partida" : "Retorno à Cooperativa")
                    : "Coleta" + (comColetas() ? " - " + situacao(k) : "");
            if (k >= 0 && coords != null && coord(i) == null) rotulo += " - sem localização";
            ((TextView) v.findViewById(R.id.txtParadaMTipo)).setText(rotulo);
            ((TextView) v.findViewById(R.id.txtParadaMEndereco)).setText(GestorRotaTexto.endereco(e));
            v.findViewById(R.id.linhaParadaM)
                    .setVisibility(i == ends.size() - 1 ? View.INVISIBLE : View.VISIBLE);
            final int pos = i;
            v.setOnClickListener(x -> {
                if (dialogo[0] != null) dialogo[0].dismiss();
                detalhe(pos);
            });
            raiz.addView(v);
        }
        ScrollView sv = new ScrollView(this);
        sv.addView(raiz);
        dialogo[0] = new MaterialAlertDialogBuilder(this)
                .setTitle(rota != null && rota.nome != null ? rota.nome : "Paradas")
                .setView(sv)
                .setPositiveButton("Fechar", null)
                .show();
    }

    // ───────────────────────────── tela ─────────────────────────────

    private static String hora(String iso) {
        return iso != null && iso.length() >= 16 ? iso.substring(11, 16) : null;
    }

    /** Com a viagem em andamento ou já finalizada, a situação de cada coleta vale para a tela. */
    private boolean comColetas() {
        return viagem.emAndamento() || viagem.finalizada();
    }

    private void render() {
        if (viagem == null) return;
        boolean andamento = viagem.emAndamento();
        boolean meu = andamento && !viagem.outroMotorista();
        boolean finalizada = viagem.finalizada()
                || (viagem.carregou() && viagem.status() == null
                        && MotoristaLocal.finalizadaHoje(this, rotaId));
        boolean ativa = rota == null || !Boolean.FALSE.equals(rota.estaAtiva);
        int total = viagem.totalColetas(), feitas = comColetas() ? viagem.concluidas() : 0;

        TextView progresso = findViewById(R.id.txtProgressoNav);
        if (rota == null) progresso.setText("Carregando a rota…");
        else if (andamento)
            progresso.setText(feitas + " de " + total + (total == 1 ? " coleta concluída" : " coletas concluídas")
                    + (viagem.outroMotorista() ? " · outro motorista" : ""));
        else if (finalizada)
            progresso.setText("Rota finalizada"
                    + (viagem.finalizada() && total > 0 ? " · " + feitas + " de " + total + " coletas concluídas" : ""));
        else progresso.setText(total + (total == 1 ? " coleta" : " coletas") + " · viagem não iniciada");
        ProgressBar barra = findViewById(R.id.barraProgressoNav);
        barra.setProgress(total == 0 ? 0 : Math.round(100f * feitas / total));

        findViewById(R.id.boxAcoesNav).setVisibility(meu ? View.VISIBLE : View.GONE);
        View iniciar = findViewById(R.id.btnIniciarNav);
        iniciar.setVisibility(rota != null && viagem.carregou() && !andamento && !finalizada && ativa
                && !viagem.semConexao() ? View.VISIBLE : View.GONE);
        iniciar.setEnabled(!viagem.ocupado());
        iniciar.setAlpha(viagem.ocupado() ? 0.5f : 1f);
        View finalizar = findViewById(R.id.btnFinalizarNav);
        finalizar.setVisibility(meu ? View.VISIBLE : View.GONE);
        finalizar.setEnabled(!viagem.ocupado());
        View concluir = findViewById(R.id.btnConcluirNav);
        concluir.setEnabled(viagem.podeMexer() && viagem.proxima() >= 0);
        concluir.setAlpha(concluir.isEnabled() ? 1f : 0.5f);
        // dirigindo com o mapa aberto: a tela não apaga (e a chegada às paradas continua sendo medida)
        if (meu) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        atualizarAviso();
        atualizarProxima();
        desenhar();
    }

    private void atualizarAviso() {
        TextView aviso = findViewById(R.id.txtAvisoMapaNav);
        String texto = null;
        int pendentes = viagem.pendentesDeEnvio();
        if (viagem.semConexao())
            texto = "Sem conexão com o acompanhamento da viagem."
                    + (pendentes > 0 ? " " + pendentes + (pendentes == 1 ? " coleta aguardando" : " coletas aguardando") + " envio." : "");
        else if (pendentes > 0)
            texto = pendentes + (pendentes == 1 ? " coleta aguardando" : " coletas aguardando") + " envio.";
        else if (findViewById(R.id.mapaEsquemaNav).getVisibility() == View.VISIBLE)
            texto = "Mapa indisponível neste aparelho: o trajeto abaixo é ilustrativo. Use \"Navegar\""
                    + " para o GPS.";
        else if (permissaoNegada) texto = "Localização desativada: sua posição não aparece no mapa.";
        else if (coords != null && validas() < ends.size())
            texto = (ends.size() - validas()) + " endereço(s) não localizado(s) no mapa.";
        aviso.setVisibility(texto == null ? View.GONE : View.VISIBLE);
        if (texto != null) aviso.setText(texto);
    }

    private int validas() {
        int n = 0;
        if (coords != null) for (double[] c : coords) if (c != null) n++;
        return n;
    }

    /** Textos da próxima parada e distância até ela (barato: roda a cada posição nova). */
    private void atualizarProxima() {
        TextView rotulo = findViewById(R.id.txtProximaRotuloNav);
        TextView endereco = findViewById(R.id.txtProximaEnderecoNav);
        TextView dist = findViewById(R.id.txtProximaDistNav);
        if (ends.isEmpty()) {
            rotulo.setText("Próxima parada");
            endereco.setText(rota == null ? "—" : "Rota sem paradas cadastradas.");
            dist.setText("");
            return;
        }
        if (viagem.finalizada()
                || (viagem.carregou() && viagem.status() == null && MotoristaLocal.finalizadaHoje(this, rotaId))) {
            rotulo.setText("Rota finalizada");
            endereco.setText("Os dados da viagem foram enviados à cooperativa.");
            dist.setText("");
            return;
        }
        if (!viagem.emAndamento()) {
            rotulo.setText("Primeira parada");
            int i = posicaoNaRota(0);
            endereco.setText(GestorRotaTexto.endereco(ends.get(i < 0 ? 0 : i)));
            dist.setText("");
            return;
        }
        int k = viagem.proxima();
        if (k < 0) {
            rotulo.setText("Todas as coletas concluídas");
            GestorResponses.RotaEnd fim = ends.get(ends.size() - 1);
            endereco.setText(ehExtremo(fim) ? "Retorno: " + GestorRotaTexto.endereco(fim) : "Finalize a rota.");
            dist.setText("");
            return;
        }
        int i = posicaoNaRota(k);
        rotulo.setText("Próxima parada · " + (i + 1) + " de " + ends.size());
        endereco.setText(GestorRotaTexto.endereco(ends.get(i)));
        double[] c = coord(i);
        if (c != null && posicao != null) {
            double m = RotaEstimador.metros(posicao, c);
            dist.setText(m <= ChegadaParada.RAIO_METROS
                    ? "Você está na parada: ela será concluída automaticamente."
                    : "A " + RotaEstimador.textoDistancia(m) + " em linha reta"
                            + " · conclui sozinha ao chegar");
        } else if (coords != null && c == null) {
            dist.setText("Endereço não localizado: use \"Navegar\" pelo endereço.");
        } else {
            dist.setText("");
        }
    }

    /** Pinos e linha do trajeto; só redesenha quando paradas, posições, trajeto ou status mudam. */
    private void desenhar() {
        if (mapa == null) {
            ((RotaMapView) findViewById(R.id.mapaEsquemaNav)).setTotalParadas(ends.size());
            return;
        }
        StringBuilder ass = new StringBuilder().append(ends.size()).append('|').append(validas())
                .append('|').append(trechos != null);
        int prox = viagem.emAndamento() ? posicaoNaRota(viagem.proxima()) : -1;
        ass.append('|').append(prox);
        for (int k = 0; k < viagem.totalColetas(); k++) ass.append(viagem.concluida(k) ? '1' : '0');
        if (ass.toString().equals(desenhado)) return;
        desenhado = ass.toString();
        if (coords == null) {
            mapa.desenhar(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            return;
        }

        List<MapaRota.Pino> pinos = new ArrayList<>();
        // feitoAte[i]: a parada i e todas antes dela foram feitas (a linha verde vai até ela)
        boolean[] feitoAte = new boolean[ends.size()];
        boolean emSequencia = viagem.emAndamento();
        for (int i = 0; i < ends.size(); i++) {
            int k = indiceColeta(i);
            boolean feita = k >= 0 && comColetas() && viagem.concluida(k);
            if (k >= 0 && !feita) emSequencia = false;
            feitoAte[i] = emSequencia;
            double[] c = coord(i);
            if (c == null) continue;
            int fundo, texto;
            boolean borda = false;
            if (i == 0 && ehExtremo(ends.get(i)) || feita) {
                fundo = VERDE;
                texto = Color.WHITE;
            } else if (i == prox || ("FIM".equalsIgnoreCase(ends.get(i).tipoLocal))) {
                fundo = VINHO;
                texto = Color.WHITE;
            } else {
                fundo = Color.WHITE;
                texto = VERDE;
                borda = true;
            }
            pinos.add(new MapaRota.Pino(c, i, numero(i + 1, fundo, texto, borda), i == prox));
        }

        // trecho j: da j-ésima à (j+1)-ésima parada localizada; verde se chega a uma parada feita
        List<double[]> feito = new ArrayList<>(), resto = new ArrayList<>();
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < ends.size(); i++) if (coord(i) != null) idx.add(i);
        for (int j = 0; j + 1 < idx.size(); j++) {
            List<double[]> trecho = trechos != null && j < trechos.size() ? trechos.get(j)
                    : java.util.Arrays.asList(coord(idx.get(j)), coord(idx.get(j + 1)));
            List<double[]> destino = feitoAte[idx.get(j + 1)] ? feito : resto;
            for (double[] p : trecho) {
                double[] ultimo = destino.isEmpty() ? null : destino.get(destino.size() - 1);
                if (ultimo == null || ultimo[0] != p[0] || ultimo[1] != p[1]) destino.add(p);
            }
        }
        mapa.desenhar(pinos, feito, resto);
    }

    /** Mostra todas as paradas (e, a pedido, o motorista) na tela. */
    private void enquadrar(boolean pedido) {
        if (mapa == null || coords == null || (enquadrou && !pedido)) return;
        List<double[]> pontos = localizadas();
        if (pontos.isEmpty()) {
            if (pedido) toast("Nenhum endereço da rota foi localizado no mapa.");
            return;
        }
        if (posicao != null && pedido) pontos.add(posicao);
        enquadrou = true;
        mapa.enquadrar(pontos);
    }

    /** Pino redondo numerado, nas cores do trajeto esquemático (RotaMapView). */
    private Bitmap numero(int n, int fundo, int texto, boolean borda) {
        float d = getResources().getDisplayMetrics().density;
        int tam = (int) (30 * d);
        Bitmap bmp = Bitmap.createBitmap(tam, tam, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float r = tam / 2f;
        p.setColor(Color.WHITE);
        c.drawCircle(r, r, r, p);
        p.setColor(fundo);
        c.drawCircle(r, r, r - 2 * d, p);
        if (borda) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2 * d);
            p.setColor(VERDE);
            c.drawCircle(r, r, r - 2 * d, p);
            p.setStyle(Paint.Style.FILL);
        }
        p.setColor(texto);
        p.setTextAlign(Paint.Align.CENTER);
        p.setFakeBoldText(true);
        p.setTextSize(13 * d);
        c.drawText(String.valueOf(n), r, r - (p.descent() + p.ascent()) / 2, p);
        return bmp;
    }
}
