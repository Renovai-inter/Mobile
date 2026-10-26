package com.example.renovai.view;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.renovai.EmpresaData;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.dto.response.EmpresaResponses;
import com.example.renovai.dto.response.PedidoResponse;

import java.util.ArrayList;
import java.util.List;

/** Tela 5.1 — Home (Dashboard) da Empresa. */
public class EmpresaHomeFragment extends EmpresaAbaFragment {

    public EmpresaHomeFragment() {
        super(R.layout.activity_empresa_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle b) {
        super.onViewCreated(view, b);
        findViewById(R.id.btnBuscarMateriaisE)
                .setOnClickListener(
                        v -> abrirAba(R.id.nav_cooperativas));
        findViewById(R.id.btnMeusPedidosE)
                .setOnClickListener(
                        v -> abrirAba(R.id.nav_pedidos));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!vivo()) return;
        EmpresaData.meuPerfil(
                false,
                (p, c) -> {
                    if (vivo()) preencherHeader(p.nomeEmpresa);
                });
        EmpresaData.dashboard(
                false,
                new com.example.renovai.GestorData.Ouvinte<EmpresaResponses.Dashboard>() {
                    @Override
                    public void aoReceber(EmpresaResponses.Dashboard d, boolean c) {
                        if (vivo()) renderDashboard(d);
                    }

                    @Override
                    public void aoErro(String m) {}
                });
        EmpresaData.pedidos(
                false,
                new com.example.renovai.GestorData.Ouvinte<List<PedidoResponse>>() {
                    @Override
                    public void aoReceber(List<PedidoResponse> l, boolean c) {
                        if (vivo()) renderPedidos(l);
                    }

                    @Override
                    public void aoErro(String m) {
                        if (vivo()) toast(m);
                    }
                });
        EmpresaData.favoritos(
                false,
                new com.example.renovai.GestorData.Ouvinte<List<EmpresaResponses.Favorito>>() {
                    @Override
                    public void aoReceber(List<EmpresaResponses.Favorito> l, boolean c) {
                        if (vivo()) renderFavoritas(l);
                    }

                    @Override
                    public void aoErro(String m) {}
                });
    }

    private void renderDashboard(EmpresaResponses.Dashboard d) {
        ((TextView) findViewById(R.id.txtPedidosMes))
                .setText(
                        String.valueOf(
                                d.totalPedidosEnviados == null ? 0 : d.totalPedidosEnviados));
        ((TextView) findViewById(R.id.txtEmNegociacao))
                .setText(String.valueOf(d.totalPedidosAceitos == null ? 0 : d.totalPedidosAceitos));
        ((TextView) findViewById(R.id.txtFinalizados))
                .setText(GestorUi.dinheiro(d.valorTotalNegociado));
    }

    private void renderPedidos(List<PedidoResponse> lista) {
        List<PedidoResponse> ordenada = new ArrayList<>(lista);
        ordenada.sort(
                (a, b) ->
                        String.valueOf(b.getDataPedido())
                                .compareTo(String.valueOf(a.getDataPedido())));
        LinearLayout raiz = findViewById(R.id.listaPedidosRecentes);
        raiz.removeAllViews();
        findViewById(R.id.txtSemPedidos)
                .setVisibility(ordenada.isEmpty() ? View.VISIBLE : View.GONE);
        for (PedidoResponse p : ordenada.subList(0, Math.min(3, ordenada.size()))) {
            View v =
                    LayoutInflater.from(tela())
                            .inflate(R.layout.item_empresa_pedido_recente, raiz, false);
            ((TextView) v.findViewById(R.id.txtPedidoRecenteSub))
                    .setText(
                            GestorUi.idCurto(p.getPedidoId())
                                    + (p.getValorTotal() != null
                                            ? " — " + GestorUi.dinheiro(p.getValorTotal())
                                            : ""));
            ((TextView) v.findViewById(R.id.txtPedidoRecenteData))
                    .setText(GestorUi.data(p.getDataPedido()));
            TextView badge = v.findViewById(R.id.txtPedidoRecenteBadge);
            int estado =
                    com.example.renovai.EmpresaStatus.estado(
                            null, p.getStatusAtual(), p.getDataConclusao());
            badge.setText(com.example.renovai.EmpresaPedidos.rotulo(estado));
            GestorUi.badge(badge, com.example.renovai.EmpresaStatus.cor(estado));
            badge.setCompoundDrawables(null, null, null, null);
            v.setOnClickListener(x -> EmpresaPedidoCard.abrirDetalhe(tela(), p.getPedidoId(), null));
            raiz.addView(v);
        }
    }

    private void renderFavoritas(List<EmpresaResponses.Favorito> lista) {
        ((TextView) findViewById(R.id.txtTotalFavoritasE)).setText(String.valueOf(lista.size()));
        LinearLayout raiz = findViewById(R.id.linhaCooperativasRecentes);
        raiz.removeAllViews();
        findViewById(R.id.txtSemFavoritas)
                .setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
        for (EmpresaResponses.Favorito f : lista.subList(0, Math.min(2, lista.size()))) {
            View v = LayoutInflater.from(tela()).inflate(R.layout.item_cooperativa_card, raiz, false);
            ((TextView) v.findViewById(R.id.txtNome))
                    .setText(f.cooperativaNome == null ? "Cooperativa" : f.cooperativaNome);
            v.findViewById(R.id.txtAvaliacao).setVisibility(View.GONE);
            v.setOnClickListener(
                    x -> EmpresaCooperativaCard.abrirPerfilPublico(tela(), f.cooperativaId));
            raiz.addView(v);
        }
    }
}
