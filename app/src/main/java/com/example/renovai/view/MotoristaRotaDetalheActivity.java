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
import com.example.renovai.R;
import com.example.renovai.RotaEstimador;
import com.example.renovai.dto.response.GestorResponses;

import java.util.ArrayList;
import java.util.List;

/** Tela 3.2 — Rota selecionada: mapa, trajeto com progresso por parada, GPS e Iniciar/Finalizar. */
public class MotoristaRotaDetalheActivity extends MotoristaBaseActivity {

    public static final String EXTRA_ROTA_ID = "rotaId";

    private String rotaId;
    private GestorResponses.Rota rota;
    private List<GestorResponses.RotaEnd> ends = new ArrayList<>();

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!vivo() || rotaId == null) return;
        MotoristaData.api()
                .rota(rotaId)
                .enqueue(
                        new retrofit2.Callback<GestorResponses.Rota>() {
                            public void onResponse(
                                    retrofit2.Call<GestorResponses.Rota> call,
                                    retrofit2.Response<GestorResponses.Rota> r) {
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
                                render();
                            }

                            public void onFailure(
                                    retrofit2.Call<GestorResponses.Rota> c, Throwable t) {
                                if (vivo()) toast("Não foi possível atualizar a rota.");
                            }
                        });
    }

    private List<Integer> ordensColeta() {
        List<Integer> l = new ArrayList<>();
        for (GestorResponses.RotaEnd e : ends) if (!ehExtremo(e)) l.add(e.ordem);
        return l;
    }

    private boolean ehExtremo(GestorResponses.RotaEnd e) {
        return "INICIO".equalsIgnoreCase(e.tipoLocal) || "FIM".equalsIgnoreCase(e.tipoLocal);
    }

    private void render() {
        ends = new ArrayList<>(rota.enderecos == null ? new ArrayList<>() : rota.enderecos);
        ends.sort(
                (a, b) ->
                        Integer.compare(
                                a.ordem == null ? Integer.MAX_VALUE : a.ordem,
                                b.ordem == null ? Integer.MAX_VALUE : b.ordem));

        boolean ativa = !Boolean.FALSE.equals(rota.estaAtiva);
        boolean iniciada = false;

        titulo(rota.nome == null ? "Rota" : rota.nome);
        ((TextView) findViewById(R.id.txtRotaMTitulo)).setText(rota.nome);
        ((TextView) findViewById(R.id.txtRotaMCoop))
                .setText(rota.cooperativaNome == null ? "—" : rota.cooperativaNome);
        ((com.example.renovai.view.RotaMapView) findViewById(R.id.mapaRotaM))
                .setTotalParadas(ends.size());
        ((TextView) findViewById(R.id.txtResumoRotaM))
                .setText(ends.size() + (ends.size() == 1 ? " parada" : " paradas"));

        TextView status = findViewById(R.id.txtRotaMStatus);
        List<Integer> ordensColeta = ordensColeta();
        int feitas = 0;
        GestorUi.badge(status, ativa ? GestorUi.EM_NEGOCIACAO : GestorUi.OUTRO);
        status.setText(ativa ? "Disponível" : "Inativa");
        status.setCompoundDrawables(null, null, null, null);
        ((TextView) findViewById(R.id.txtRotaMIniciada))
                .setText("Acompanhamento da viagem disponível em breve.");
        ((TextView) findViewById(R.id.txtRotaMProgressoTxt))
                .setText("Andamento ainda indisponível");
        ((TextView) findViewById(R.id.txtParadasConcM)).setText("— / " + ordensColeta.size());
        findViewById(R.id.btnIniciarRotaM).setVisibility(ativa ? View.VISIBLE : View.GONE);
        findViewById(R.id.boxIniciadaM).setVisibility(View.VISIBLE);
        findViewById(R.id.btnFinalizarM).setEnabled(false);
        findViewById(R.id.btnGpsM).setEnabled(!ends.isEmpty());

        String prox = null;
        LinearLayout raiz = findViewById(R.id.listaParadasM);
        raiz.removeAllViews();
        for (int i = 0; i < ends.size(); i++) {
            GestorResponses.RotaEnd e = ends.get(i);
            boolean extremo = ehExtremo(e);
            boolean feita = false;
            boolean ehFim = "FIM".equalsIgnoreCase(e.tipoLocal);
            if (prox == null && !extremo && !feita && iniciada) prox = GestorRotaTexto.curto(e);

            View v = LayoutInflater.from(this).inflate(R.layout.item_motorista_parada, raiz, false);
            TextView num = v.findViewById(R.id.txtParadaMNumero);
            num.setText(String.valueOf(i + 1));
            num.setBackgroundResource(
                    feita || (ehFim && !ativa)
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
            String situacao = "";
            ((TextView) v.findViewById(R.id.txtParadaMTipo)).setText(rotulo + situacao);
            ((TextView) v.findViewById(R.id.txtParadaMEndereco))
                    .setText(GestorRotaTexto.endereco(e));
            v.findViewById(R.id.linhaParadaM)
                    .setVisibility(i == ends.size() - 1 ? View.INVISIBLE : View.VISIBLE);

            raiz.addView(v);
        }
        ((TextView) findViewById(R.id.txtProxRotaM)).setText(prox != null ? "Próx: " + prox : "—");

        List<String> qs = new ArrayList<>();
        for (GestorResponses.RotaEnd e : ends) qs.add(GestorRotaTexto.consulta(e));
        RotaEstimador.estimar(
                this,
                qs,
                new RotaEstimador.Callback() {
                    @Override
                    public void resultado(double km, int min) {
                        if (!vivo()) return;
                        ((TextView) findViewById(R.id.txtDistanciaM))
                                .setText(RotaEstimador.textoKm(km) + " (estimado)");
                        ((TextView) findViewById(R.id.txtTempoM))
                                .setText(RotaEstimador.textoTempo(min) + " (estimado)");
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
