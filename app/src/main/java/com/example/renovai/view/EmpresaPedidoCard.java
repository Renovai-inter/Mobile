package com.example.renovai.view;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.example.renovai.EmpresaPedidos;
import com.example.renovai.GestorUi;
import com.example.renovai.R;

/** Preenche o cartão de pedido (item_empresa_pedido) usado nas telas Pedidos e Home. */
final class EmpresaPedidoCard {

    private EmpresaPedidoCard() {}

    static void abrirDetalhe(Activity a, String pedidoId, String negociacaoId) {
        Intent i = new Intent(a, EmpresaPedidoDetalheActivity.class);
        i.putExtra(EmpresaPedidoDetalheActivity.EXTRA_PEDIDO_ID, pedidoId);
        i.putExtra(EmpresaPedidoDetalheActivity.EXTRA_NEG_ID, negociacaoId);
        a.startActivity(i);
    }

    static void bind(Activity a, View v, EmpresaPedidos.Vista p) {
        ((TextView) v.findViewById(R.id.txtPedidoIdE)).setText(GestorUi.idCurto(p.pedidoId));
        ((TextView) v.findViewById(R.id.txtPedidoDataE)).setText(GestorUi.data(p.data));
        ((TextView) v.findViewById(R.id.txtPedidoPesoE)).setText(GestorUi.kg(p.peso));
        ((TextView) v.findViewById(R.id.txtPedidoMateriaisE)).setText(p.materiais);
        TextView badge = v.findViewById(R.id.txtPedidoBadgeE);
        badge.setText(EmpresaPedidos.rotulo(p.estado));
        GestorUi.badge(badge, com.example.renovai.EmpresaStatus.cor(p.estado));
        badge.setCompoundDrawables(null, null, null, null);

        View.OnClickListener abrir = x -> abrirDetalhe(a, p.pedidoId, p.negociacaoId);
        v.setOnClickListener(abrir);
        v.findViewById(R.id.btnDetalhesPedidoE).setOnClickListener(abrir);
        View msgs = v.findViewById(R.id.btnMensagensPedidoE);
        boolean conversa = p.negociacaoId != null;
        msgs.setVisibility(conversa ? View.VISIBLE : View.GONE);
        msgs.setOnClickListener(
                x -> {
                    Intent i = new Intent(a, EmpresaChatActivity.class);
                    i.putExtra(EmpresaChatActivity.EXTRA_NEG_ID, p.negociacaoId);
                    i.putExtra(EmpresaChatActivity.EXTRA_COOPERATIVA_ID, p.cooperativaId);
                    a.startActivity(i);
                });
    }
}
