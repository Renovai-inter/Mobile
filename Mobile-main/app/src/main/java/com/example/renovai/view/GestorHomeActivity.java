package com.example.renovai.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renovai.GestorBottomNav;
import com.example.renovai.GestorData;
import com.example.renovai.GestorPedidos;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.adapter.ColetaListAdapter;
import com.example.renovai.adapter.ListaAdapter;
import com.example.renovai.adapter.TriagemListAdapter;
import com.example.renovai.controller.TriagemController;
import com.example.renovai.dto.response.ColetaResponse;
import com.example.renovai.dto.response.GestorResponses;
import com.example.renovai.dto.response.TriagemResponse;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Tela 4.1 — Home do Gestor: visão geral, ações rápidas e listagens recentes (cache primeiro,
 * atualização depois).
 */
public class GestorHomeActivity extends GestorBaseActivity {

    private TextView kpiPedidos,
            kpiPedidosDet,
            kpiColetas,
            kpiColetasDet,
            kpiEstoque,
            kpiEstoqueDet,
            semPedidos,
            semColetas,
            semTriagens;
    private ListaAdapter<GestorPedidos.Vista> pedidosAdapter;
    private ColetaListAdapter coletaAdapter;
    private TriagemListAdapter triagemAdapter;

    private List<ColetaResponse> coletas = new ArrayList<>();
    private List<TriagemResponse> triagens = new ArrayList<>();
    private List<GestorResponses.Estoque> estoque = new ArrayList<>();
    private List<GestorResponses.Negociacao> negs = new ArrayList<>();
    private List<GestorResponses.PedidoCoop> pcs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_gestor_home, GestorBottomNav.Aba.HOME)) return;

        kpiPedidos = findViewById(R.id.txtKpiPedidos);
        kpiPedidosDet = findViewById(R.id.txtKpiPedidosDet);
        kpiColetas = findViewById(R.id.txtKpiColetas);
        kpiColetasDet = findViewById(R.id.txtKpiColetasDet);
        kpiEstoque = findViewById(R.id.txtKpiEstoque);
        kpiEstoqueDet = findViewById(R.id.txtKpiEstoqueDet);
        semPedidos = findViewById(R.id.txtSemPedidos);
        semColetas = findViewById(R.id.txtSemColetas);
        semTriagens = findViewById(R.id.txtSemTriagens);

        pedidosAdapter =
                new ListaAdapter<>(
                        R.layout.item_gestor_pedido,
                        (v, p, i) -> GestorPedidoCard.bind(this, v, p, this::recarregar));
        lista(R.id.recyclerHomePedidos).setAdapter(pedidosAdapter);

        coletaAdapter =
                new ColetaListAdapter(
                        c -> {
                            Intent i = new Intent(this, GestorColetaDetalheActivity.class);
                            i.putExtra(
                                    GestorColetaDetalheActivity.EXTRA_COLETA_ID, c.getColetaId());
                            startActivity(i);
                        });
        lista(R.id.recyclerHomeColetas).setAdapter(coletaAdapter);

        triagemAdapter =
                new TriagemListAdapter(
                        new TriagemListAdapter.OnTriagemClickListener() {
                            @Override
                            public void onDetalhesClick(TriagemListAdapter.Grupo g) {
                                Intent i =
                                        new Intent(
                                                GestorHomeActivity.this,
                                                GestorTriagemDetalheActivity.class);
                                i.putExtra(
                                        GestorTriagemDetalheActivity.EXTRA_COLETA_ID, g.coletaId);
                                startActivity(i);
                            }

                            @Override
                            public void onContinuarClick(TriagemListAdapter.Grupo g) {
                                Intent i =
                                        new Intent(
                                                GestorHomeActivity.this,
                                                CompletarTriagemActivity.class);
                                i.putExtra(CompletarTriagemActivity.EXTRA_COLETA_ID, g.coletaId);
                                startActivity(i);
                            }
                        });
        lista(R.id.recyclerHomeTriagens).setAdapter(triagemAdapter);

        findViewById(R.id.btnNovaTriagem)
                .setOnClickListener(
                        v -> startActivity(new Intent(this, GestorNovaTriagemActivity.class)));
        findViewById(R.id.btnNovaRota)
                .setOnClickListener(
                        v -> startActivity(new Intent(this, GestorNovaRotaActivity.class)));
        findViewById(R.id.btnNovoRateio)
                .setOnClickListener(
                        v -> startActivity(new Intent(this, GestorRateiosActivity.class)));
        findViewById(R.id.txtAtalhoRotas)
                .setOnClickListener(
                        v -> startActivity(new Intent(this, GestorRotasActivity.class)));
        findViewById(R.id.txtAtalhoRelatorios)
                .setOnClickListener(
                        v -> startActivity(new Intent(this, GestorRelatoriosActivity.class)));
        findViewById(R.id.btnPedidos)
                .setOnClickListener(
                        v -> startActivity(new Intent(this, GestorPedidosActivity.class)));
        findViewById(R.id.txtVerTodosPedidos)
                .setOnClickListener(
                        v -> startActivity(new Intent(this, GestorPedidosPendentesActivity.class)));
        findViewById(R.id.txtVerTodasColetas)
                .setOnClickListener(
                        v -> startActivity(new Intent(this, GestorColetasActivity.class)));
        findViewById(R.id.txtVerTodasTriagens)
                .setOnClickListener(
                        v -> startActivity(new Intent(this, GestorTriagensActivity.class)));
    }

    private RecyclerView lista(int id) {
        RecyclerView r = findViewById(id);
        r.setLayoutManager(new LinearLayoutManager(this));
        return r;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (vivo() && funcId() != null) recarregar();
    }

    private void recarregar() {
        GestorData.coletas(
                false,
                (l, c) -> {
                    coletas = l;
                    if (vivo()) render();
                });
        GestorData.triagens(
                false,
                (l, c) -> {
                    triagens = l;
                    if (vivo()) render();
                });
        GestorData.estoque(
                false,
                (l, c) -> {
                    estoque = l;
                    if (vivo()) render();
                });
        GestorData.negociacoes(
                false,
                (l, c) -> {
                    negs = l;
                    if (vivo()) render();
                });
        GestorData.pedidosCoop(
                false,
                (l, c) -> {
                    pcs = l;
                    if (vivo()) render();
                });
        GestorPedidos.pendentes(
                new GestorData.Ouvinte<List<GestorPedidos.Vista>>() {
                    @Override
                    public void aoReceber(List<GestorPedidos.Vista> l, boolean c) {
                        if (!vivo()) return;
                        pedidosAdapter.submit(l.subList(0, Math.min(2, l.size())));
                        semPedidos.setVisibility(l.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private static String dois(int n) {
        return String.format(GestorUi.BR, "%02d", n);
    }

    private static boolean emTransito(ColetaResponse c) {
        String s = c.getStatusAtual();
        return s == null
                || s.trim().isEmpty()
                || GestorUi.tem(s, "PENDENT", "AGEND", "TRANSIT", "ABERT", "IMEDIAT");
    }

    private void render() {
        // Pedidos pendentes = pedidos abertos (aguardando resposta) + negociações em andamento
        int abertos = 0, emNeg = 0;
        java.util.Set<String> pedidosComNegociacao = new java.util.HashSet<>();
        for (GestorResponses.Negociacao n : negs)
            if (n.pedidoId != null) pedidosComNegociacao.add(n.pedidoId);
        for (GestorResponses.PedidoCoop pc : pcs)
            if (GestorPedidos.ehPendente(pc.statusAtual)
                    && !pedidosComNegociacao.contains(pc.pedidoId)) abertos++;
        for (GestorResponses.Negociacao n : negs)
            if (com.example.renovai.EmpresaStatus.estado(n, null, null)
                    == com.example.renovai.EmpresaStatus.EM_NEGOCIACAO) emNeg++;
        kpiPedidos.setText(dois(abertos + emNeg));
        kpiPedidosDet.setText(abertos + " Abertos\n" + emNeg + " Em negociação");

        // Coletas do mês atual, separadas em externas e internas
        YearMonth mes = YearMonth.now();
        int total = 0, ext = 0;
        for (ColetaResponse c : coletas) {
            if (!GestorUi.noMes(c.getDataColeta(), mes)) continue;
            total++;
            if (GestorUi.tem(c.getTipoColeta(), "EXTERN")) ext++;
        }
        kpiColetas.setText(dois(total));
        kpiColetasDet.setText(ext + " Externas\n" + (total - ext) + " Internas");

        // Estoque total = já triado (estoque) + em trânsito (coletas ainda não processadas)
        double triados = 0, transito = 0;
        for (GestorResponses.Estoque e : estoque)
            triados += e.quantidadeKg == null ? 0 : e.quantidadeKg;
        for (ColetaResponse c : coletas)
            if (emTransito(c) && c.getQuantidadeKg() != null)
                transito += c.getQuantidadeKg().doubleValue();
        kpiEstoque.setText(GestorUi.numero(triados));
        kpiEstoqueDet.setText(
                GestorUi.numero(triados)
                        + "kg Triados\n"
                        + GestorUi.numero(transito)
                        + "kg Em transito");

        List<ColetaResponse> cs = new ArrayList<>(coletas);
        cs.sort(
                (a, c) ->
                        String.valueOf(c.getDataColeta())
                                .compareTo(String.valueOf(a.getDataColeta())));
        coletaAdapter.submitList(cs.subList(0, Math.min(2, cs.size())));
        semColetas.setVisibility(cs.isEmpty() ? View.VISIBLE : View.GONE);

        List<TriagemListAdapter.Grupo> gs =
                new ArrayList<>(TriagemController.agruparPorColeta(triagens));
        gs.sort(
                (a, c) ->
                        String.valueOf(c.dataConclusaoIso)
                                .compareTo(String.valueOf(a.dataConclusaoIso)));
        triagemAdapter.submitList(gs.subList(0, Math.min(2, gs.size())));
        semTriagens.setVisibility(gs.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
