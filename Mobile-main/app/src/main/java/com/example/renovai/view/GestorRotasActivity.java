package com.example.renovai.view;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renovai.GestorBottomNav;
import com.example.renovai.GestorData;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.RotaEstimador;
import com.example.renovai.adapter.ListaAdapter;
import com.example.renovai.dto.response.GestorResponses;

import java.util.ArrayList;
import java.util.List;

/**
 * Rotas da cooperativa: Todas / Em Andamento (ativas) / Concluídas (inativas), com resumo estimado.
 */
public class GestorRotasActivity extends GestorBaseActivity {

    private ListaAdapter<GestorResponses.Rota> adapter;
    private List<GestorResponses.Rota> todas = new ArrayList<>();
    private int filtro = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_gestor_lista, GestorBottomNav.Aba.NENHUMA)) return;
        titulo("Rotas");
        ((TextView) findViewById(R.id.txtTituloLista)).setText("Rotas");
        ((TextView) findViewById(R.id.txtSubtituloLista))
                .setText("Gerencie as rotas de coleta da cooperativa");
        TextView acao = findViewById(R.id.btnAcao);
        acao.setText("+  Registrar");
        acao.setVisibility(View.VISIBLE);
        acao.setOnClickListener(v -> startActivity(new Intent(this, GestorNovaRotaActivity.class)));
        View trilha = findViewById(R.id.trilhaLista);
        trilha.setVisibility(View.VISIBLE);
        GestorUi.segmentado(
                (LinearLayout) trilha,
                new String[] {"Todas", "Disponíveis", "Inativas"},
                0,
                false,
                i -> {
                    filtro = i;
                    render();
                });

        adapter = new ListaAdapter<>(R.layout.item_gestor_rota, this::bind);
        RecyclerView r = findViewById(R.id.recyclerLista);
        r.setLayoutManager(new LinearLayoutManager(this));
        r.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!vivo() || funcId() == null) return;
        GestorData.rotas(
                false,
                new GestorData.Ouvinte<List<GestorResponses.Rota>>() {
                    @Override
                    public void aoReceber(List<GestorResponses.Rota> l, boolean c) {
                        todas = l;
                        if (vivo()) render();
                    }

                    @Override
                    public void aoErro(String m) {
                        if (vivo()) toast(m);
                    }
                });
    }

    private void render() {
        List<GestorResponses.Rota> f = new ArrayList<>();
        for (GestorResponses.Rota r : todas) {
            boolean ativa = !Boolean.FALSE.equals(r.estaAtiva);
            if (filtro == 0 || (filtro == 1 && ativa) || (filtro == 2 && !ativa)) f.add(r);
        }
        TextView vazio = findViewById(R.id.txtVazio);
        vazio.setText("Nenhuma rota encontrada.");
        vazio.setVisibility(f.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.submit(f);
    }

    private static List<GestorResponses.RotaEnd> ordenadas(GestorResponses.Rota r) {
        List<GestorResponses.RotaEnd> l =
                r.enderecos == null ? new ArrayList<>() : new ArrayList<>(r.enderecos);
        l.sort((a, b) -> (a.ordem == null ? 0 : a.ordem) - (b.ordem == null ? 0 : b.ordem));
        return l;
    }

    private void bind(View v, GestorResponses.Rota r, int pos) {
        boolean ativa = !Boolean.FALSE.equals(r.estaAtiva);
        List<GestorResponses.RotaEnd> ends = ordenadas(r);
        TextView nome = v.findViewById(R.id.txtRotaNome);
        nome.setText(r.nome);
        nome.setPaintFlags(nome.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        ((TextView) v.findViewById(R.id.txtRotaCriada))
                .setText("Criada em " + GestorRotaLocal.criada(this, r.rotaId));
        TextView badge = v.findViewById(R.id.txtRotaBadge);
        badge.setText(ativa ? "Disponível" : "Inativa");
        GestorUi.badge(badge, ativa ? GestorUi.EM_NEGOCIACAO : GestorUi.CONCLUIDO);
        badge.setCompoundDrawables(null, null, null, null);
        ((TextView) v.findViewById(R.id.txtRotaOrigem))
                .setText(ends.isEmpty() ? "—" : GestorRotaTexto.curto(ends.get(0)));
        ((TextView) v.findViewById(R.id.txtRotaDestino))
                .setText(ends.size() < 2 ? "—" : GestorRotaTexto.curto(ends.get(ends.size() - 1)));
        GestorUi.borda(
                v.findViewById(R.id.cardRota), Color.parseColor(ativa ? "#6FAE76" : "#CCCCCC"));

        TextView resumo = v.findViewById(R.id.txtRotaResumo);
        final String base = ends.size() + (ends.size() == 1 ? " parada" : " paradas");
        resumo.setText(base);
        v.setTag(r.rotaId);
        List<String> qs = new ArrayList<>();
        for (GestorResponses.RotaEnd e : ends) qs.add(GestorRotaTexto.consulta(e));
        RotaEstimador.estimar(
                this,
                qs,
                new RotaEstimador.Callback() {
                    @Override
                    public void resultado(double km, int min) {
                        if (r.rotaId.equals(v.getTag()))
                            resumo.setText(
                                    base
                                            + " · "
                                            + RotaEstimador.textoKm(km)
                                            + " · "
                                            + RotaEstimador.textoTempo(min)
                                            + " (estimado)");
                    }

                    @Override
                    public void indisponivel() {}
                });
        v.setOnClickListener(
                x -> {
                    Intent i = new Intent(this, GestorRotaDetalheActivity.class);
                    i.putExtra(GestorRotaDetalheActivity.EXTRA_ROTA_ID, r.rotaId);
                    startActivity(i);
                });
    }
}
