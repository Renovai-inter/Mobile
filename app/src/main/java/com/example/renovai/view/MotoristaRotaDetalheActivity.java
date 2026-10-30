package com.example.renovai.view;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
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
import com.example.renovai.dto.response.GestorResponses;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tela 3.2 — Rota selecionada: mapa, trajeto com progresso por parada, GPS e Iniciar/Finalizar.
 * A rota vem da Spring API; o andamento da viagem (iniciada, coletas concluídas, finalizada) fica na
 * API-Redis (ver RotaViagemApiService).
 */
public class MotoristaRotaDetalheActivity extends MotoristaBaseActivity {

    public static final String EXTRA_ROTA_ID = "rotaId";

    private String rotaId;
    private GestorResponses.Rota rota;
    private List<GestorResponses.RotaEnd> ends = new ArrayList<>();

    /** null = viagem não iniciada; senão INICIADA ou FINALIZADA (status da API-Redis). */
    private String viagem;
    private String viagemInicio;
    private boolean outroMotorista;
    /** Índice da parada de coleta (0, 1, 2… na ordem da rota) → status na API-Redis. */
    private final Map<Integer, String> statusColetas = new HashMap<>();
    private boolean ocupado, reenviouParadas;

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
        findViewById(R.id.btnIniciarRotaM).setEnabled(false);
        findViewById(R.id.btnFinalizarM).setEnabled(false);
        findViewById(R.id.btnGpsM).setOnClickListener(v -> abrirGps());
        findViewById(R.id.btnIniciarRotaM).setOnClickListener(v -> iniciar());
        findViewById(R.id.btnFinalizarM).setOnClickListener(v -> confirmarFinalizar());
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
                                render();
                                carregarViagem();
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

    private boolean emAndamento() {
        return RotaViagemApiService.INICIADA.equals(viagem);
    }

    private boolean podeMexer() {
        return emAndamento() && !outroMotorista && !ocupado;
    }

    // ───────────────────────────── andamento (API-Redis) ─────────────────────────────

    private void carregarViagem() {
        RotaViagemApiService.criar()
                .sessao(rotaId)
                .enqueue(
                        new Callback<RotaViagemApiService.Sessao>() {
                            @Override
                            public void onResponse(
                                    Call<RotaViagemApiService.Sessao> c,
                                    Response<RotaViagemApiService.Sessao> r) {
                                if (!vivo()) return;
                                statusColetas.clear();
                                if (r.code() == 404 || r.body() == null) {
                                    // não iniciada, ou finalizada e já gravada no Postgres
                                    viagem = null;
                                    viagemInicio = null;
                                    outroMotorista = false;
                                    render();
                                    return;
                                }
                                RotaViagemApiService.Sessao s = r.body();
                                viagem = s.status;
                                viagemInicio = s.inicio;
                                outroMotorista =
                                        funcId() != null
                                                && s.motoristaId != null
                                                && !funcId().equalsIgnoreCase(s.motoristaId);
                                render();
                                if (emAndamento()) carregarParadas();
                            }

                            @Override
                            public void onFailure(Call<RotaViagemApiService.Sessao> c, Throwable t) {
                                if (!vivo()) return;
                                ((TextView) findViewById(R.id.txtRotaMIniciada))
                                        .setText("Sem conexão com o acompanhamento da viagem.");
                            }
                        });
    }

    private void carregarParadas() {
        RotaViagemApiService.criar()
                .paradas(rotaId)
                .enqueue(
                        new Callback<RotaViagemApiService.Paradas>() {
                            @Override
                            public void onResponse(
                                    Call<RotaViagemApiService.Paradas> c,
                                    Response<RotaViagemApiService.Paradas> r) {
                                if (!vivo() || !r.isSuccessful() || r.body() == null) return;
                                List<RotaViagemApiService.Parada> lista = r.body().paradas;
                                if ((lista == null || lista.isEmpty())
                                        && !outroMotorista
                                        && !reenviouParadas
                                        && !coletas().isEmpty()) {
                                    // o envio das paradas falhou ao iniciar: manda de novo (1 vez)
                                    reenviouParadas = true;
                                    enviarParadas(null);
                                    return;
                                }
                                statusColetas.clear();
                                if (lista != null) {
                                    for (RotaViagemApiService.Parada p : lista) {
                                        try {
                                            statusColetas.put(Integer.parseInt(p.id), p.status);
                                        } catch (NumberFormatException ignorado) {
                                            // parada fora do formato da API: fica pendente
                                        }
                                    }
                                }
                                render();
                            }

                            @Override
                            public void onFailure(Call<RotaViagemApiService.Paradas> c, Throwable t) {}
                        });
    }

    private void iniciar() {
        if (ocupado) return;
        if (funcId() == null) {
            toast("Não foi possível identificar o motorista. Faça login novamente.");
            return;
        }
        ocupar(true);
        RotaViagemApiService.criar()
                .iniciar(new RotaViagemApiService.Inicio(rotaId, funcId()))
                .enqueue(
                        new Callback<Map<String, Object>>() {
                            @Override
                            public void onResponse(
                                    Call<Map<String, Object>> c, Response<Map<String, Object>> r) {
                                if (!vivo()) return;
                                if (!r.isSuccessful()) {
                                    ocupar(false);
                                    toast("Não foi possível iniciar a rota (" + r.code() + ").");
                                    return;
                                }
                                MotoristaLocal.marcarIniciada(
                                        MotoristaRotaDetalheActivity.this, rotaId);
                                enviarParadas(() -> toast("Rota iniciada. Boa viagem!"));
                            }

                            @Override
                            public void onFailure(Call<Map<String, Object>> c, Throwable t) {
                                if (!vivo()) return;
                                ocupar(false);
                                toast("Sem conexão: a rota não foi iniciada.");
                            }
                        });
    }

    /** Registra as paradas de coleta na API-Redis e recarrega o andamento. */
    private void enviarParadas(Runnable depois) {
        List<RotaViagemApiService.Parada> lista = new ArrayList<>();
        for (GestorResponses.RotaEnd e : coletas())
            lista.add(new RotaViagemApiService.Parada(GestorRotaTexto.endereco(e)));
        if (lista.isEmpty()) {
            ocupar(false);
            if (depois != null) depois.run();
            carregarViagem();
            return;
        }
        ocupar(true);
        RotaViagemApiService.criar()
                .adicionarParadas(rotaId, new RotaViagemApiService.Paradas(lista))
                .enqueue(
                        new Callback<Map<String, Object>>() {
                            @Override
                            public void onResponse(
                                    Call<Map<String, Object>> c, Response<Map<String, Object>> r) {
                                if (!vivo()) return;
                                ocupar(false);
                                if (r.isSuccessful() && depois != null) depois.run();
                                carregarViagem();
                            }

                            @Override
                            public void onFailure(Call<Map<String, Object>> c, Throwable t) {
                                if (!vivo()) return;
                                ocupar(false);
                                carregarViagem();
                            }
                        });
    }

    /** Toque numa coleta durante a viagem: marca como concluída ou volta para pendente. */
    private void alternarColeta(int indice) {
        if (!podeMexer()) return;
        boolean feita = RotaViagemApiService.CONCLUIDA.equals(statusColetas.get(indice));
        String novo = feita ? RotaViagemApiService.PENDENTE : RotaViagemApiService.CONCLUIDA;
        statusColetas.put(indice, novo); // otimista; desfaz se a API recusar
        render();
        ocupar(true);
        RotaViagemApiService.criar()
                .atualizarParada(rotaId, indice, new RotaViagemApiService.NovoStatus(novo))
                .enqueue(
                        new Callback<RotaViagemApiService.Parada>() {
                            @Override
                            public void onResponse(
                                    Call<RotaViagemApiService.Parada> c,
                                    Response<RotaViagemApiService.Parada> r) {
                                if (!vivo()) return;
                                ocupar(false);
                                if (!r.isSuccessful()) {
                                    toast("Não foi possível atualizar a coleta.");
                                    carregarViagem();
                                }
                            }

                            @Override
                            public void onFailure(Call<RotaViagemApiService.Parada> c, Throwable t) {
                                if (!vivo()) return;
                                ocupar(false);
                                toast("Sem conexão: a coleta não foi atualizada.");
                                carregarViagem();
                            }
                        });
    }

    private void confirmarFinalizar() {
        if (!podeMexer()) return;
        int pendentes = coletas().size() - concluidas();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Finalizar rota")
                .setMessage(
                        pendentes > 0
                                ? "Ainda há " + pendentes
                                        + (pendentes == 1 ? " coleta pendente" : " coletas pendentes")
                                        + ". Finalizar mesmo assim?"
                                : "Todas as coletas foram concluídas. Finalizar a rota?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Finalizar", (d, w) -> finalizar())
                .show();
    }

    private void finalizar() {
        ocupar(true);
        RotaViagemApiService.criar()
                .finalizar(rotaId)
                .enqueue(
                        new Callback<RotaViagemApiService.Sessao>() {
                            @Override
                            public void onResponse(
                                    Call<RotaViagemApiService.Sessao> c,
                                    Response<RotaViagemApiService.Sessao> r) {
                                if (!vivo()) return;
                                ocupar(false);
                                if (!r.isSuccessful()) {
                                    toast("Não foi possível finalizar a rota (" + r.code() + ").");
                                    carregarViagem();
                                    return;
                                }
                                MotoristaLocal.marcarFinalizada(
                                        MotoristaRotaDetalheActivity.this, rotaId);
                                viagem = RotaViagemApiService.FINALIZADA;
                                render();
                                toast("Rota finalizada.");
                            }

                            @Override
                            public void onFailure(Call<RotaViagemApiService.Sessao> c, Throwable t) {
                                if (!vivo()) return;
                                ocupar(false);
                                toast("Sem conexão: a rota não foi finalizada.");
                            }
                        });
    }

    private void ocupar(boolean sim) {
        ocupado = sim;
        if (rota != null) render();
    }

    private int concluidas() {
        int n = 0;
        for (String s : statusColetas.values())
            if (RotaViagemApiService.CONCLUIDA.equals(s)) n++;
        return n;
    }

    // ───────────────────────────── tela ─────────────────────────────

    /** "2026-10-29T08:15:03.12" → "08:15". */
    private static String hora(String iso) {
        return iso != null && iso.length() >= 16 ? iso.substring(11, 16) : null;
    }

    private void render() {
        boolean ativa = !Boolean.FALSE.equals(rota.estaAtiva);
        boolean andamento = emAndamento();
        boolean finalizada =
                RotaViagemApiService.FINALIZADA.equals(viagem)
                        || (viagem == null && MotoristaLocal.finalizadaHoje(this, rotaId));

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
        int feitas = andamento ? concluidas() : 0;
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

        String inicio = hora(viagemInicio);
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
                                                : "Viagem não iniciada.");
        ((TextView) findViewById(R.id.txtRotaMProgressoTxt))
                .setText(
                        andamento
                                ? feitas + " de " + totalColetas + " coletas concluídas"
                                        + (outroMotorista ? "" : " · toque numa coleta para marcar")
                                : "Inicie a rota para registrar as coletas");
        ((TextView) findViewById(R.id.txtParadasConcM))
                .setText((andamento ? String.valueOf(feitas) : "—") + " / " + totalColetas);

        View btnIniciar = findViewById(R.id.btnIniciarRotaM);
        btnIniciar.setVisibility(ativa && !andamento ? View.VISIBLE : View.GONE);
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
                    andamento
                            && !extremo
                            && RotaViagemApiService.CONCLUIDA.equals(statusColetas.get(indice));
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
            String situacao = !extremo && andamento ? (feita ? " - Concluída" : " - Pendente") : "";
            ((TextView) v.findViewById(R.id.txtParadaMTipo)).setText(rotulo + situacao);
            ((TextView) v.findViewById(R.id.txtParadaMEndereco))
                    .setText(GestorRotaTexto.endereco(e));
            v.findViewById(R.id.linhaParadaM)
                    .setVisibility(i == ends.size() - 1 ? View.INVISIBLE : View.VISIBLE);
            if (!extremo && andamento && !outroMotorista) {
                v.setOnClickListener(x -> alternarColeta(indice));
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

    private void abrirGps() {
        if (ends.isEmpty()) {
            toast("Rota sem paradas cadastradas.");
            return;
        }
        StringBuilder url = new StringBuilder("https://www.google.com/maps/dir/?api=1");
        url.append("&origin=").append(Uri.encode(GestorRotaTexto.consulta(ends.get(0))));
        url.append("&destination=")
                .append(Uri.encode(GestorRotaTexto.consulta(ends.get(ends.size() - 1))));
        if (ends.size() > 2) {
            StringBuilder wp = new StringBuilder();
            for (int i = 1; i < ends.size() - 1; i++) {
                if (wp.length() > 0) wp.append("|");
                wp.append(GestorRotaTexto.consulta(ends.get(i)));
            }
            url.append("&waypoints=").append(Uri.encode(wp.toString()));
        }
        url.append("&travelmode=driving");
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url.toString())));
        } catch (Exception e) {
            toast("Não foi possível abrir o app de mapas.");
        }
    }
}
