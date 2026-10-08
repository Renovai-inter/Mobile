package com.example.renovai.view;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.renovai.GestorBottomNav;
import com.example.renovai.GestorData;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.RotaEstimador;
import com.example.renovai.dto.response.GestorResponses;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Detalhe da rota: mapa esquemático, motorista, trajeto em ordem e resumo (distância/tempo
 * estimados).
 */
public class GestorRotaDetalheActivity extends GestorBaseActivity {

    public static final String EXTRA_ROTA_ID = "rotaId";

    private String rotaId;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_gestor_rota_detalhe, GestorBottomNav.Aba.NENHUMA)) return;
        rotaId = getIntent().getStringExtra(EXTRA_ROTA_ID);
        titulo("Rota");
        if (rotaId == null) {
            finish();
            return;
        }
        GestorData.rotas(
                false,
                (l, c) -> {
                    for (GestorResponses.Rota r : l)
                        if (r.rotaId != null && r.rotaId.equalsIgnoreCase(rotaId)) {
                            if (vivo()) render(r);
                            return;
                        }
                    GestorData.api()
                            .rota(rotaId)
                            .enqueue(
                                    new Callback<GestorResponses.Rota>() {
                                        @Override
                                        public void onResponse(
                                                Call<GestorResponses.Rota> c2,
                                                Response<GestorResponses.Rota> r2) {
                                            if (r2.isSuccessful() && r2.body() != null && vivo())
                                                render(r2.body());
                                        }

                                        @Override
                                        public void onFailure(
                                                Call<GestorResponses.Rota> c2, Throwable t) {}
                                    });
                });
        ((TextView) findViewById(R.id.txtRotaMotorista)).setText("Atribuição ainda indisponível");
    }

    private void render(GestorResponses.Rota r) {
        List<GestorResponses.RotaEnd> ends =
                r.enderecos == null ? new ArrayList<>() : new ArrayList<>(r.enderecos);
        ends.sort((a, x) -> (a.ordem == null ? 0 : a.ordem) - (x.ordem == null ? 0 : x.ordem));
        titulo(r.nome == null ? "Rota" : r.nome);
        ((TextView) findViewById(R.id.txtRotaTitulo)).setText(r.nome);
        ((TextView) findViewById(R.id.txtRotaCriada))
                .setText("Criada em " + GestorRotaLocal.criada(this, r.rotaId));
        boolean ativa = !Boolean.FALSE.equals(r.estaAtiva);
        TextView st = findViewById(R.id.txtRotaStatus);
        st.setText(ativa ? "Disponível" : "Inativa");
        GestorUi.badge(st, ativa ? GestorUi.EM_NEGOCIACAO : GestorUi.CONCLUIDO);
        st.setCompoundDrawables(null, null, null, null);

        ((RotaMapView) findViewById(R.id.mapaRota)).setTotalParadas(ends.size());
        ((TextView) findViewById(R.id.txtResParadas)).setText(String.valueOf(ends.size()));

        LinearLayout raiz = findViewById(R.id.listaParadas);
        raiz.removeAllViews();
        int n = ends.size();
        List<String> qs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            GestorResponses.RotaEnd e = ends.get(i);
            qs.add(GestorRotaTexto.consulta(e));
            boolean ini = i == 0, fim = i == n - 1 && n > 1;
            View v = LayoutInflater.from(this).inflate(R.layout.item_gestor_parada, raiz, false);
            TextView num = v.findViewById(R.id.txtParadaNumero);
            num.setText(String.valueOf(i + 1));
            num.setBackgroundResource(
                    ini
                            ? R.drawable.stop_filled_green
                            : fim
                                    ? R.drawable.avatar_maroon_background
                                    : R.drawable.stop_outline_green);
            num.setTextColor(ini || fim ? Color.WHITE : Color.parseColor("#519059"));
            ((TextView) v.findViewById(R.id.txtParadaTipo))
                    .setText(ini ? "Ponto de Partida" : fim ? "Fim" : "Coleta");
            ((TextView) v.findViewById(R.id.txtParadaEndereco))
                    .setText(GestorRotaTexto.endereco(e));
            TextView tag = v.findViewById(R.id.txtParadaTag);
            tag.setText(ini ? "Início" : fim ? "Fim" : "Coleta");
            tag.setBackgroundResource(
                    ini
                            ? R.drawable.btn_green_background
                            : fim
                                    ? R.drawable.card_maroon_background
                                    : R.drawable.pill_light_green);
            tag.setTextColor(ini || fim ? Color.WHITE : Color.parseColor("#3F6B3F"));
            v.findViewById(R.id.linhaParada)
                    .setVisibility(i == n - 1 ? View.INVISIBLE : View.VISIBLE);
            raiz.addView(v);
        }

        TextView dist = findViewById(R.id.txtResDistancia),
                tempo = findViewById(R.id.txtResTempo),
                mapa = findViewById(R.id.txtMapaResumo);
        mapa.setText(n + " paradas");
        RotaEstimador.estimar(
                this,
                qs,
                new RotaEstimador.Callback() {
                    @Override
                    public void resultado(double km, int min) {
                        if (!vivo()) return;
                        dist.setText(RotaEstimador.textoKm(km) + " (estimado)");
                        tempo.setText(RotaEstimador.textoTempo(min) + " (estimado)");
                        mapa.setText(
                                RotaEstimador.textoKm(km) + " • " + RotaEstimador.textoTempo(min));
                    }

                    @Override
                    public void indisponivel() {}
                });
    }
}
