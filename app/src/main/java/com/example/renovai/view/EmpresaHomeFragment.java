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
import com.example.renovai.EmpresaPedidos;
import com.example.renovai.EmpresaStatus;
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
        findViewById(R.id.cardPedidosE).setOnClickListener(v -> abrirAba(R.id.nav_pedidos));
        findViewById(R.id.btnVerFavoritasE).setOnClickListener(v -> abrirAba(R.id.nav_favoritas));
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
        EmpresaPedidos.listar(
                new com.example.renovai.GestorData.Ouvinte<List<EmpresaPedidos.Vista>>() {
                    @Override
                    public void aoReceber(List<EmpresaPedidos.Vista> l, boolean c) {
                        if (vivo()) renderResumo(l);
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

    /**
     * Card "Pedidos enviados": total e situação com as mesmas regras da aba Pedidos (Em andamento =
     * aberto ou em negociação), e o valor aprovado (aceitos + finalizados).
     */
    private void renderResumo(List<EmpresaPedidos.Vista> pedidos) {
        int andamento = 0, aceitos = 0, finalizados = 0;
        double valor = 0;
        for (EmpresaPedidos.Vista v : pedidos) {
            if (v.estado == EmpresaStatus.ABERTO || v.estado == EmpresaStatus.EM_NEGOCIACAO) andamento++;
            else if (v.estado == EmpresaStatus.ACEITO) aceitos++;
            else if (v.estado == EmpresaStatus.FINALIZADO) finalizados++;
            if (EmpresaStatus.aprovado(v.estado)) valor += v.valor;
        }
        ((TextView) findViewById(R.id.txtPedidosMes)).setText(String.valueOf(pedidos.size()));
        ((TextView) findViewById(R.id.txtKpiAndamentoE)).setText(String.valueOf(andamento));
        ((TextView) findViewById(R.id.txtKpiAceitosE)).setText(String.valueOf(aceitos));
        ((TextView) findViewById(R.id.txtKpiFinalizadosE)).setText(String.valueOf(finalizados));
        ((TextView) findViewById(R.id.txtValorAprovadoE)).setText(GestorUi.dinheiro(valor));
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
            android.widget.ImageView foto = v.findViewById(R.id.imgCooperativa);
            EmpresaCooperativaCard.foto(foto, f.cooperativaImagem);
            if (f.cooperativaImagem == null) fotoDoPerfil(foto, f.cooperativaId);
            v.findViewById(R.id.txtAvaliacao).setVisibility(View.GONE);
            v.setOnClickListener(
                    x -> EmpresaCooperativaCard.abrirPerfilPublico(tela(), f.cooperativaId));
            raiz.addView(v);
        }
    }

    /** Favorito sem foto: usa a foto do perfil público da cooperativa (Cloudinary). */
    private void fotoDoPerfil(android.widget.ImageView foto, String cooperativaId) {
        EmpresaData.api()
                .perfilPublico(cooperativaId)
                .enqueue(
                        new retrofit2.Callback<EmpresaResponses.CooperativaPerfilPublico>() {
                            @Override
                            public void onResponse(
                                    retrofit2.Call<EmpresaResponses.CooperativaPerfilPublico> c,
                                    retrofit2.Response<EmpresaResponses.CooperativaPerfilPublico> r) {
                                if (vivo() && r.isSuccessful() && r.body() != null && r.body().imagemUrl != null)
                                    EmpresaCooperativaCard.foto(foto, r.body().imagemUrl);
                            }

                            @Override
                            public void onFailure(
                                    retrofit2.Call<EmpresaResponses.CooperativaPerfilPublico> c, Throwable t) {}
                        });
    }
}
