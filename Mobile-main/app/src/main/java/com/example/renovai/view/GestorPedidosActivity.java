package com.example.renovai.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renovai.GestorBottomNav;
import com.example.renovai.GestorData;
import com.example.renovai.GestorPedidos;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.adapter.ListaAdapter;
import com.example.renovai.dto.response.GestorResponses;

import java.time.YearMonth;
import java.util.List;

/** Tela 4.5 — Pedidos: total arrecadado no mês e a lista de pedidos (em negociação, concluídos, recusados). */
public class GestorPedidosActivity extends GestorBaseActivity {

    private ListaAdapter<GestorPedidos.Vista> adapter;
    private View banner;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_gestor_lista, GestorBottomNav.Aba.NENHUMA)) return;
        titulo("Pedidos");
        ((TextView) findViewById(R.id.txtTituloLista)).setText("Pedidos");
        ((TextView) findViewById(R.id.txtSubtituloLista)).setText("Gerencie os pedidos feitos à sua cooperativa");
        TextView acao = findViewById(R.id.btnAcao);
        acao.setText("Ver Pendentes");
        acao.setVisibility(View.VISIBLE);
        acao.setOnClickListener(v -> startActivity(new Intent(this, GestorPedidosPendentesActivity.class)));

        FrameLayout box = findViewById(R.id.bannerContainer);
        banner = LayoutInflater.from(this).inflate(R.layout.view_gestor_banner_total, box, false);
        box.addView(banner);
        box.setVisibility(View.VISIBLE);

        adapter = new ListaAdapter<>(R.layout.item_gestor_pedido, (v, p, i) -> GestorPedidoCard.bind(this, v, p, this::carregar));
        RecyclerView r = findViewById(R.id.recyclerLista);
        r.setLayoutManager(new LinearLayoutManager(this));
        r.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (vivo() && funcId() != null) carregar();
    }

    private void carregar() {
        GestorData.negociacoes(false, (l, c) -> { if (vivo()) banner(l); });
        GestorPedidos.historico(new GestorData.Ouvinte<List<GestorPedidos.Vista>>() {
            @Override public void aoReceber(List<GestorPedidos.Vista> l, boolean c) {
                if (!vivo()) return;
                findViewById(R.id.txtVazio).setVisibility(l.isEmpty() ? View.VISIBLE : View.GONE);
                ((TextView) findViewById(R.id.txtVazio)).setText("Nenhum pedido recebido ainda.");
                adapter.submit(l);
            }
            @Override public void aoErro(String m) { if (vivo()) toast(m); }
        });
    }

    private void banner(List<GestorResponses.Negociacao> negs) {
        YearMonth agora = YearMonth.now();
        double atual = GestorUi.somaConcluidas(negs, agora), anterior = GestorUi.somaConcluidas(negs, agora.minusMonths(1));
        ((TextView) banner.findViewById(R.id.txtBannerValor)).setText(GestorUi.dinheiro(atual).replace("R$", "").trim());
        String cmp;
        if (anterior <= 0) cmp = atual > 0 ? "Sem arrecadação no mês anterior para comparar." : "Ainda não há pedidos concluídos neste mês.";
        else {
            double pct = (atual - anterior) / anterior * 100;
            cmp = "A cooperativa arrecadou " + String.format(GestorUi.BR, "%.1f%%", Math.abs(pct)) + (pct >= 0 ? " a mais" : " a menos") + " desde o último mês";
        }
        ((TextView) banner.findViewById(R.id.txtBannerComparacao)).setText(cmp);
    }
}
