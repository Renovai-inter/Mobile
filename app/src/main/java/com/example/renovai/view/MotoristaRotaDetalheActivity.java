package com.example.renovai.view;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.renovai.GestorUi;
import com.example.renovai.MotoristaBottomNav;
import com.example.renovai.MotoristaData;
import com.example.renovai.MotoristaLocal;
import com.example.renovai.R;
import com.example.renovai.RotaEstimador;
import com.example.renovai.RotaOtimizadorIaService;
import com.example.renovai.RotaViagemApiService;
import com.example.renovai.controller.ViagemRota;
import com.example.renovai.dto.response.GestorResponses;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tela 3.2 — Rota selecionada: trajeto com progresso por parada, mapa e Iniciar/Finalizar.
 * A rota vem da Spring API; o andamento da viagem (iniciada, coletas concluídas, finalizada) fica na
 * API-Redis, pelo ViagemRota — o mesmo da tela do mapa (MotoristaNavegacaoActivity).
 */
public class MotoristaRotaDetalheActivity extends MotoristaBaseActivity {

    public static final String EXTRA_ROTA_ID = "rotaId";

    private String rotaId;
    private GestorResponses.Rota rota;
    private List<GestorResponses.RotaEnd> ends = new ArrayList<>();

    private ViagemRota viagem;

    /** Distância e tempo já formatados (da IA ou da estimativa local) e a dica de ordem da IA. */
    private String distancia, tempo, sugestaoIa;
    private boolean estimou;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_motorista_rota_detalhe, MotoristaBottomNav.Aba.NENHUMA))
            return;
        titulo("Rota");
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
                        if (vivo() && rota != null) render();
                    }

                    @Override
                    public void aviso(String m) {
                        if (vivo()) toast(m);
                    }

                    @Override
                    public void finalizou() {
                        MotoristaLocal.marcarFinalizada(MotoristaRotaDetalheActivity.this, rotaId);
                        toast("Rota finalizada.");
                    }
                });
        findViewById(R.id.btnIniciarRotaM).setEnabled(false);
        findViewById(R.id.btnFinalizarM).setEnabled(false);
        findViewById(R.id.btnGpsM).setOnClickListener(
                v -> MotoristaNavegacaoActivity.abrir(this, rotaId));
        findViewById(R.id.btnIniciarRotaM).setOnClickListener(v -> viagem.iniciar());
        findViewById(R.id.btnFinalizarM).setOnClickListener(v -> confirmarFinalizar());
    }

    @Override
    protected void onDestroy() {
        if (viagem != null) viagem.encerrar();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!vivo() || rotaId == null) return;
        MotoristaData.api()
                .rota(rotaId)
                .enqueue(
                        new Callback<GestorResponses.Rota>() {
                            public void onResponse(
                                    Call<GestorResponses.Rota> call,
                                    Response<GestorResponses.Rota> r) {
                                if (!vivo()) return;
                                if (!r.isSuccessful() || r.body() == null) {
                                    toast(com.example.renovai.EmpresaData.erro(r));
                                    return;
                                }
                                if (coopId() == null
                                        || !coopId().equalsIgnoreCase(r.body().cooperativaId)) {
                                    toast("Rota de outra cooperativa.");
                                    finish();
                                    return;
                                }
                                rota = r.body();
                                ordenarParadas();
                                List<ViagemRota.Coleta> lista = new ArrayList<>();
                                for (GestorResponses.RotaEnd e : coletas())
                                    lista.add(new ViagemRota.Coleta(
                                            GestorRotaTexto.endereco(e), e.enderecoId, e.ordem));
                                viagem.setColetas(lista);
                                render();
                                viagem.carregar();
                                estimar();
                            }

                            public void onFailure(Call<GestorResponses.Rota> c, Throwable t) {
                                if (vivo()) toast("Não foi possível atualizar a rota.");
                            }
                        });
    }

    private void ordenarParadas() {
        ends = new ArrayList<>(rota.enderecos == null ? new ArrayList<>() : rota.enderecos);
        ends.sort(
                (a, b) ->
                        Integer.compare(
                                a.ordem == null ? Integer.MAX_VALUE : a.ordem,
                                b.ordem == null ? Integer.MAX_VALUE : b.ordem));
    }

    private boolean ehExtremo(GestorResponses.RotaEnd e) {
        return "INICIO".equalsIgnoreCase(e.tipoLocal) || "FIM".equalsIgnoreCase(e.tipoLocal);
    }

    /** Paradas de coleta (sem partida e retorno), na ordem da rota: o índice é o da API-Redis. */
    private List<GestorResponses.RotaEnd> coletas() {
        List<GestorResponses.RotaEnd> l = new ArrayList<>();
        for (GestorResponses.RotaEnd e : ends) if (!ehExtremo(e)) l.add(e);
        return l;
    }

    private void confirmarFinalizar() {
        if (!viagem.podeMexer()) return;
        if (viagem.pendentesDeEnvio() > 0) {
            viagem.finalizar(); // tenta enviar o que ficou guardado e avisa
            return;
        }
        int pendentes = coletas().size() - viagem.concluidas();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Finalizar rota")
                .setMessage(
                        pendentes > 0
                                ? "Ainda há " + pendentes
                                        + (pendentes == 1 ? " coleta pendente" : " coletas pendentes")
                                        + ". Finalizar mesmo assim?"
                                : "Todas as coletas foram concluídas. Finalizar a rota?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Finalizar", (d, w) -> viagem.finalizar())
                .show();
    }

    // ───────────────────────────── tela ─────────────────────────────

    /** "2026-10-29T08:15:03.12" → "08:15". */
    private static String hora(String iso) {
        return iso != null && iso.length() >= 16 ? iso.substring(11, 16) : null;
    }

    private void render() {
        boolean ativa = !Boolean.FALSE.equals(rota.estaAtiva);
        boolean andamento = viagem.emAndamento();
        boolean outroMotorista = viagem.outroMotorista();
        boolean ocupado = viagem.ocupado();
        boolean finalizada =
                viagem.finalizada()
                        || (viagem.carregou() && viagem.status() == null
                                && MotoristaLocal.finalizadaHoje(this, rotaId));

        titulo(rota.nome == null ? "Rota" : rota.nome);
        ((TextView) findViewById(R.id.txtRotaMTitulo)).setText(rota.nome);
        ((TextView) findViewById(R.id.txtRotaMCoop))
                .setText(rota.cooperativaNome == null ? "—" : rota.cooperativaNome);
        ((com.example.renovai.view.RotaMapView) findViewById(R.id.mapaRotaM))
                .setTotalParadas(ends.size());
        ((TextView) findViewById(R.id.txtResumoRotaM))
                .setText(ends.size() + (ends.size() == 1 ? " parada" : " paradas"));

        TextView status = findViewById(R.id.txtRotaMStatus);
        int totalColetas = coletas().size();
        // finalizada, a API ainda guarda as coletas até a sincronização com o Postgres
        boolean comColetas = andamento || viagem.finalizada();
        int feitas = comColetas ? viagem.concluidas() : 0;
        if (andamento) {
            GestorUi.badge(status, GestorUi.EM_NEGOCIACAO);
            status.setText("Em andamento");
        } else if (finalizada) {
            GestorUi.badge(status, GestorUi.CONCLUIDO);
            status.setText("Finalizada hoje");
        } else {
            GestorUi.badge(status, ativa ? GestorUi.EM_NEGOCIACAO : GestorUi.OUTRO);
            status.setText(ativa ? "Disponível" : "Inativa");
        }
        status.setCompoundDrawables(null, null, null, null);

        String inicio = hora(viagem.inicio());
        String fim = hora(MotoristaLocal.finalizadaEm(this, rotaId));
        ((TextView) findViewById(R.id.txtRotaMIniciada))
                .setText(
                        andamento && outroMotorista
                                ? "Em andamento com outro motorista"
                                        + (inicio != null ? " desde " + inicio : "") + "."
                                : andamento
                                        ? "Iniciada às " + (inicio != null ? inicio : "—") + "."
                                        : finalizada
                                                ? "Finalizada"
                                                        + (fim != null ? " às " + fim : "")
                                                        + ". Os dados foram enviados à cooperativa."
                                                : viagem.semConexao()
                                                        ? "Sem conexão com o acompanhamento da viagem."
                                                        : "Viagem não iniciada.");
        ((TextView) findViewById(R.id.txtRotaMProgressoTxt))
                .setText(
                        andamento
                                ? feitas + " de " + totalColetas + " coletas concluídas"
                                        + (viagem.pendentesDeEnvio() > 0
                                                ? " · " + viagem.pendentesDeEnvio() + " aguardando envio"
                                                : outroMotorista ? "" : " · toque numa coleta para marcar")
                                : comColetas
                                        ? feitas + " de " + totalColetas + " coletas concluídas"
                                        : finalizada
                                                ? "Rota finalizada"
                                                : "Inicie a rota para registrar as coletas");
        ((TextView) findViewById(R.id.txtParadasConcM))
                .setText((comColetas ? String.valueOf(feitas) : "—") + " / " + totalColetas);

        View btnIniciar = findViewById(R.id.btnIniciarRotaM);
        btnIniciar.setVisibility(
                ativa && !andamento && !finalizada && viagem.carregou() && !viagem.semConexao()
                        ? View.VISIBLE : View.GONE);
        btnIniciar.setEnabled(!ocupado);
        btnIniciar.setAlpha(ocupado ? 0.5f : 1f);
        View btnFinalizar = findViewById(R.id.btnFinalizarM);
        btnFinalizar.setVisibility(andamento && !outroMotorista ? View.VISIBLE : View.GONE);
        btnFinalizar.setEnabled(!ocupado);
        findViewById(R.id.boxIniciadaM).setVisibility(View.VISIBLE);
        findViewById(R.id.btnGpsM).setEnabled(!ends.isEmpty());

        String prox = null;
        int indiceColeta = 0;
        LinearLayout raiz = findViewById(R.id.listaParadasM);
        raiz.removeAllViews();
        for (int i = 0; i < ends.size(); i++) {
            GestorResponses.RotaEnd e = ends.get(i);
            boolean extremo = ehExtremo(e);
            int indice = extremo ? -1 : indiceColeta++;
            boolean feita =
                    comColetas
                            && !extremo
                            && viagem.concluida(indice);
            boolean ehFim = "FIM".equalsIgnoreCase(e.tipoLocal);
            if (prox == null && !extremo && !feita && andamento) prox = GestorRotaTexto.curto(e);

            View v = LayoutInflater.from(this).inflate(R.layout.item_motorista_parada, raiz, false);
            TextView num = v.findViewById(R.id.txtParadaMNumero);
            num.setText(String.valueOf(i + 1));
            num.setBackgroundResource(
                    feita || (ehFim && finalizada)
                            ? R.drawable.avatar_green_background
                            : ehFim
                                    ? R.drawable.avatar_maroon_background
                                    : R.drawable.stop_outline_green);
            if (!feita && !ehFim) num.setTextColor(Color.parseColor("#519059"));
            String rotulo =
                    extremo
                            ? ("INICIO".equalsIgnoreCase(e.tipoLocal)
                                    ? "Ponto de Partida"
                                    : "Retorno à Cooperativa")
                            : "Coleta";
            String situacao = !extremo && comColetas
                    ? (feita ? " - Concluída" : " - Pendente")
                            + (viagem.aguardandoEnvio(indice) ? " (aguardando envio)" : "")
                    : "";
            ((TextView) v.findViewById(R.id.txtParadaMTipo)).setText(rotulo + situacao);
            ((TextView) v.findViewById(R.id.txtParadaMEndereco))
                    .setText(GestorRotaTexto.endereco(e));
            v.findViewById(R.id.linhaParadaM)
                    .setVisibility(i == ends.size() - 1 ? View.INVISIBLE : View.VISIBLE);
            if (!extremo && andamento && !outroMotorista) {
                v.setOnClickListener(x -> viagem.alternar(indice));
            }

            raiz.addView(v);
        }
        ((TextView) findViewById(R.id.txtProxRotaM)).setText(prox != null ? "Próx: " + prox : "—");

        if (distancia != null)
            ((TextView) findViewById(R.id.txtDistanciaM)).setText(distancia);
        if (tempo != null) ((TextView) findViewById(R.id.txtTempoM)).setText(tempo);
        if (sugestaoIa != null)
            ((TextView) findViewById(R.id.txtResumoRotaM))
                    .setText(ends.size() + (ends.size() == 1 ? " parada" : " paradas")
                            + " · " + sugestaoIa);
    }

    // ───────────────────────────── distância e tempo ─────────────────────────────

    /**
     * Uma vez por tela: pede o cálculo à IA (modelo treinado com as rotas do Neo4j) e, se ela não
     * responder, usa a estimativa local em linha reta.
     */
    private void estimar() {
        if (estimou || ends.isEmpty()) return;
        estimou = true;
        List<GestorResponses.RotaEnd> coletas = coletas();
        if (coletas.isEmpty()) {
            estimarLocal();
            return;
        }
        GestorResponses.RotaEnd partida = ends.get(0);
        if (!ehExtremo(partida)) partida = coletas.get(0);
        List<RotaOtimizadorIaService.Ponto> paradas = new ArrayList<>();
        for (GestorResponses.RotaEnd e : coletas) if (e != partida) paradas.add(ponto(e));
        if (paradas.isEmpty()) {
            estimarLocal();
            return;
        }
        RotaOtimizadorIaService.criar()
                .otimizar(new RotaOtimizadorIaService.Pedido(
                        rota.cooperativaId, rotaId, ponto(partida), paradas,
                        java.time.LocalTime.now().getHour()))
                .enqueue(
                        new Callback<RotaOtimizadorIaService.Resultado>() {
                            @Override
                            public void onResponse(
                                    Call<RotaOtimizadorIaService.Resultado> c,
                                    Response<RotaOtimizadorIaService.Resultado> r) {
                                if (!vivo()) return;
                                RotaOtimizadorIaService.Resultado res = r.body();
                                if (!r.isSuccessful() || res == null
                                        || res.tempoOrdemAtualMin == null
                                        || res.distanciaKm == null) {
                                    estimarLocal();
                                    return;
                                }
                                distancia = RotaEstimador.textoKm(res.distanciaKm) + " (IA)";
                                tempo = RotaEstimador.textoTempo(
                                        (int) Math.round(res.tempoOrdemAtualMin)) + " (IA)";
                                long economia =
                                        res.economiaMin == null ? 0 : Math.round(res.economiaMin);
                                sugestaoIa = economia >= 5
                                        ? "a IA sugere outra ordem (−" + economia + " min)"
                                        : null;
                                render();
                            }

                            @Override
                            public void onFailure(
                                    Call<RotaOtimizadorIaService.Resultado> c, Throwable t) {
                                if (vivo()) estimarLocal();
                            }
                        });
    }

    private static RotaOtimizadorIaService.Ponto ponto(GestorResponses.RotaEnd e) {
        return new RotaOtimizadorIaService.Ponto(
                e.enderecoId, GestorRotaTexto.consulta(e), e.bairro);
    }

    private void estimarLocal() {
        List<String> qs = new ArrayList<>();
        for (GestorResponses.RotaEnd e : ends) qs.add(GestorRotaTexto.consulta(e));
        RotaEstimador.estimar(
                this,
                qs,
                new RotaEstimador.Callback() {
                    @Override
                    public void resultado(double km, int min) {
                        if (!vivo()) return;
                        distancia = RotaEstimador.textoKm(km) + " (estimado)";
                        tempo = RotaEstimador.textoTempo(min) + " (estimado)";
                        render();
                    }

                    @Override
                    public void indisponivel() {}
                });
    }
}
