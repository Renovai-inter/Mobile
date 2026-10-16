package com.example.renovai.view;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.renovai.GestorPedidos;
import com.example.renovai.GestorUi;
import com.example.renovai.R;

/** Preenche o cartão de pedido (item_gestor_pedido) nos modos pendente e histórico. */
final class GestorPedidoCard {

    private GestorPedidoCard() {}

    static void abrirDetalhe(Activity a, GestorPedidos.Vista p) {
        Intent i = new Intent(a, GestorPedidoDetalheActivity.class);
        i.putExtra(GestorPedidoDetalheActivity.EXTRA_PEDIDO_ID, p.pedidoId);
        i.putExtra(GestorPedidoDetalheActivity.EXTRA_NEG_ID, p.negociacaoId);
        i.putExtra(GestorPedidoDetalheActivity.EXTRA_PEDIDO_COOP_ID, p.pedidoCoopId);
        i.putExtra(GestorPedidoDetalheActivity.EXTRA_PENDENTE, p.pendente);
        a.startActivity(i);
    }

    static void abrirChat(Activity a, GestorPedidos.Vista p) {
        Intent i = new Intent(a, GestorChatActivity.class);
        i.putExtra(GestorChatActivity.EXTRA_NEG_ID, p.negociacaoId);
        a.startActivity(i);
    }

    static void bind(Activity a, View v, GestorPedidos.Vista p, Runnable recarregar) {
        ((TextView) v.findViewById(R.id.txtPedidoId)).setText(GestorUi.idCurto(p.pedidoId));
        ((TextView) v.findViewById(R.id.txtPedidoData)).setText(GestorUi.data(p.data));
        ((TextView) v.findViewById(R.id.txtPedidoPeso)).setText(GestorUi.kg(p.peso));
        GestorUi.borda(v, Color.parseColor("#222222"));

        TextView badge = v.findViewById(R.id.txtPedidoBadge);
        View topoDet = v.findViewById(R.id.txtPedidoDetalhesTopo);
        View boxPend = v.findViewById(R.id.boxPendentes);
        View rodape = v.findViewById(R.id.boxRodape);
        View msgs = v.findViewById(R.id.btnPedidoMensagens);
        View det = v.findViewById(R.id.btnPedidoDetalhes);

        if (p.pendente) {
            badge.setVisibility(View.GONE);
            topoDet.setVisibility(View.VISIBLE);
            boxPend.setVisibility(View.VISIBLE);
            rodape.setVisibility(View.GONE);
            topoDet.setOnClickListener(x -> abrirDetalhe(a, p));
            ((TextView) v.findViewById(R.id.btnAceitar)).setText("Negociar");
            v.findViewById(R.id.btnAceitar)
                    .setOnClickListener(
                            x ->
                                    GestorUi.confirmar(
                                            a,
                                            "Abrir negociação",
                                            "Abrir negociação do pedido "
                                                    + GestorUi.idCurto(p.pedidoId)
                                                    + " de "
                                                    + (p.empresaNome == null
                                                            ? "empresa"
                                                            : p.empresaNome)
                                                    + "? A empresa confirmará o aceite do acordo.",
                                            "Abrir",
                                            () -> aceitar(a, p, recarregar)));
            v.findViewById(R.id.btnRecusar)
                    .setOnClickListener(
                            x ->
                                    GestorUi.confirmar(
                                            a,
                                            "Recusar pedido",
                                            "Recusar o pedido "
                                                    + GestorUi.idCurto(p.pedidoId)
                                                    + "?",
                                            "Recusar",
                                            () -> recusar(a, p, recarregar)));
        } else {
            topoDet.setVisibility(View.GONE);
            boxPend.setVisibility(View.GONE);
            rodape.setVisibility(View.VISIBLE);
            badge.setVisibility(View.VISIBLE);
            badge.setText(p.aceito ? "Aceito" : GestorUi.rotuloNeg(p.estado));
            GestorUi.badge(badge, p.estado);
            det.setOnClickListener(x -> abrirDetalhe(a, p));
            boolean conversa = p.estado == GestorUi.EM_NEGOCIACAO && p.negociacaoId != null;
            msgs.setVisibility(conversa ? View.VISIBLE : View.GONE);
            msgs.setOnClickListener(x -> abrirChat(a, p));
        }
    }

    static void aceitar(Activity a, GestorPedidos.Vista p, Runnable recarregar) {
        GestorPedidos.aceitar(
                p,
                new GestorPedidos.Retorno() {
                    @Override
                    public void ok() {
                        Toast.makeText(
                                        a,
                                        "Negociação aberta. Aguarde o aceite da empresa.",
                                        Toast.LENGTH_LONG)
                                .show();
                        recarregar.run();
                    }

                    @Override
                    public void erro(String m) {
                        Toast.makeText(a, m, Toast.LENGTH_LONG).show();
                    }
                });
    }

    static void recusar(Activity a, GestorPedidos.Vista p, Runnable recarregar) {
        GestorPedidos.recusar(
                p,
                new GestorPedidos.Retorno() {
                    @Override
                    public void ok() {
                        Toast.makeText(a, "Pedido recusado.", Toast.LENGTH_LONG).show();
                        recarregar.run();
                    }

                    @Override
                    public void erro(String m) {
                        Toast.makeText(a, m, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
