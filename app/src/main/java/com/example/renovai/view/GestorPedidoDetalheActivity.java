package com.example.renovai.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.renovai.GestorBottomNav;
import com.example.renovai.GestorCache;
import com.example.renovai.GestorData;
import com.example.renovai.GestorPedidos;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.dto.response.GestorResponses;

import java.util.ArrayList;
import java.util.List;

/** Tela 4.5.1 — Detalhamento do pedido, com acesso ao chat e ao fechamento da negociação. */
public class GestorPedidoDetalheActivity extends GestorBaseActivity {

    public static final String EXTRA_PEDIDO_ID = "pedidoId",
            EXTRA_NEG_ID = "negociacaoId",
            EXTRA_PEDIDO_COOP_ID = "pedidoCoopId",
            EXTRA_PENDENTE = "pendente";
    private String pedidoId, negId, pedidoCoopId;
    private boolean pendente, concluindo;
    private GestorResponses.Negociacao neg;
    private GestorPedidos.Vista ped;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_gestor_pedido_detalhe, GestorBottomNav.Aba.NENHUMA)) return;
        titulo("Detalhes do pedido");
        pedidoId = getIntent().getStringExtra(EXTRA_PEDIDO_ID);
        negId = getIntent().getStringExtra(EXTRA_NEG_ID);
        pedidoCoopId = getIntent().getStringExtra(EXTRA_PEDIDO_COOP_ID);
        pendente = getIntent().getBooleanExtra(EXTRA_PENDENTE, false);
        if (pedidoId != null) {
            GestorPedidos.detalhe(
                    pedidoId,
                    new GestorData.Ouvinte<GestorPedidos.Vista>() {
                        @Override
                        public void aoReceber(GestorPedidos.Vista v, boolean c) {
                            ped = v;
                            if (vivo()) render();
                        }

                        @Override
                        public void aoErro(String m) {
                            if (vivo()) toast(m);
                        }
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!vivo() || funcId() == null) return;
        GestorData.negociacoes(
                true,
                (l, c) -> {
                    for (GestorResponses.Negociacao n : l) {
                        if ((negId != null && negId.equalsIgnoreCase(n.negociacaoId))
                                || (negId == null
                                        && pedidoId != null
                                        && pedidoId.equalsIgnoreCase(n.pedidoId))) neg = n;
                    }
                    if (neg != null) negId = neg.negociacaoId;
                    if (vivo()) render();
                });
    }

    private void render() {
        if (ped == null && neg == null) return;
        String pid = pedidoId != null ? pedidoId : neg.pedidoId;
        ((TextView) findViewById(R.id.txtPedidoTitulo)).setText("Pedido " + GestorUi.idCurto(pid));
        String data = neg != null ? neg.dataInicio : ped.data;
        ((TextView) findViewById(R.id.txtPedidoIniciado))
                .setText("Iniciado em " + GestorUi.data(data));
        ((TextView) findViewById(R.id.txtPedidoEmpresa))
                .setText(
                        neg != null && neg.empresaNome != null
                                ? neg.empresaNome
                                : (ped != null && ped.empresaNome != null ? ped.empresaNome : "—"));

        int estado = neg != null ? GestorUi.estadoNeg(neg) : GestorUi.EM_NEGOCIACAO;
        if (neg != null && estado == GestorUi.OUTRO) estado = GestorUi.EM_NEGOCIACAO;
        TextView badge = findViewById(R.id.txtPedidoStatus);
        badge.setText(
                neg == null
                        ? (pendente ? "Aberto" : "Recusada")
                        : com.example.renovai.EmpresaStatus.rotulo(
                                com.example.renovai.EmpresaStatus.estado(neg, null, null)));
        GestorUi.badge(badge, neg == null && !pendente ? GestorUi.RECUSADO : estado);

        LinearLayout lista = findViewById(R.id.listaItensPedido);
        lista.removeAllViews();
        double total = 0;
        List<GestorResponses.NegItem> itensNeg =
                neg != null && neg.itens != null ? neg.itens : new ArrayList<>();
        if (!itensNeg.isEmpty()) {
            for (GestorResponses.NegItem i : itensNeg) {
                linha(lista, i.materialCategoria, i.quantidadeKg, i.precoUnitario);
                total += mult(i.quantidadeKg, i.precoUnitario);
            }
        } else if (ped != null) {
            for (GestorResponses.Item i : ped.itens) {
                linha(lista, i.materialCategoria, i.quantidadeKg, i.precoUnitario);
                total += mult(i.quantidadeKg, i.precoUnitario);
            }
        }
        if (neg != null && neg.valorTotal != null && neg.valorTotal > 0) total = neg.valorTotal;
        else if (total == 0 && ped != null) total = ped.valor;
        ((TextView) findViewById(R.id.txtPedidoTotal)).setText(GestorUi.dinheiro(total));

        GestorResponses.Pedido raw = GestorCache.obter("pedido:" + pid);
        ((TextView) findViewById(R.id.txtPedidoComplemento))
                .setText(
                        raw != null && raw.observacao != null && !raw.observacao.trim().isEmpty()
                                ? raw.observacao
                                : "Nenhum");

        acoes(estado, total);
    }

    private static double mult(Double a, Double b) {
        return (a == null ? 0 : a) * (b == null ? 0 : b);
    }

    private void linha(LinearLayout raiz, String nome, Double kg, Double preco) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_gestor_item_pedido, raiz, false);
        ((TextView) v.findViewById(R.id.txtItemNome)).setText(nome == null ? "Material" : nome);
        ((TextView) v.findViewById(R.id.txtItemKg)).setText(GestorUi.kg(kg));
        ((TextView) v.findViewById(R.id.txtItemPreco)).setText(GestorUi.dinheiro(preco) + " / Kg");
        raiz.addView(v);
    }

    private void acoes(int estado, double total) {
        View box = findViewById(R.id.boxAcoesPedido);
        TextView a = findViewById(R.id.btnConversar), c = findViewById(R.id.btnConcluir);
        if (neg == null && pendente && pedidoCoopId != null) {
            box.setVisibility(View.VISIBLE);
            a.setText("Recusar");
            c.setText("Abrir negociação");
            a.setOnClickListener(
                    v ->
                            GestorUi.confirmar(
                                    this,
                                    "Recusar pedido",
                                    "Recusar este pedido?",
                                    "Recusar",
                                    () -> {
                                        GestorPedidos.Vista vis = vista();
                                        GestorPedidoCard.recusar(this, vis, this::finish);
                                    }));
            c.setOnClickListener(
                    v ->
                            GestorUi.confirmar(
                                    this,
                                    "Abrir negociação",
                                    "Abrir a negociação deste pedido com a empresa?",
                                    "Abrir",
                                    () -> {
                                        GestorPedidos.Vista vis = vista();
                                        GestorPedidoCard.aceitar(this, vis, this::finish);
                                    }));
        } else if (neg != null) {
            box.setVisibility(View.VISIBLE);
            a.setText("Conversar com a empresa");
            a.setOnClickListener(
                    v -> {
                        Intent i = new Intent(this, GestorChatActivity.class);
                        i.putExtra(GestorChatActivity.EXTRA_NEG_ID, negId);
                        startActivity(i);
                    });
            boolean aceito =
                    com.example.renovai.EmpresaStatus.estado(neg, null, null)
                            == com.example.renovai.EmpresaStatus.ACEITO;
            c.setVisibility(aceito ? View.VISIBLE : View.GONE);
            c.setEnabled(!concluindo);
            c.setText("Marcar como concluído");
            c.setOnClickListener(
                    v ->
                            GestorUi.confirmar(
                                    this,
                                    "Concluir pedido",
                                    "Confirmar a conclusão pelo valor aceito de "
                                            + GestorUi.dinheiro(neg.valorTotal)
                                            + "?",
                                    "Concluir",
                                    () -> {
                                        if (concluindo
                                                || neg.valorTotal == null
                                                || !Double.isFinite(neg.valorTotal)
                                                || neg.valorTotal < 0) return;
                                        concluindo = true;
                                        c.setEnabled(false);
                                        GestorPedidos.concluir(
                                                negId,
                                                neg.valorTotal,
                                                new GestorPedidos.Retorno() {
                                                    @Override
                                                    public void ok() {
                                                        if (vivo()) {
                                                            toast("Pedido concluído.");
                                                            finish();
                                                        }
                                                    }

                                                    @Override
                                                    public void erro(String m) {
                                                        concluindo = false;
                                                        if (vivo()) {
                                                            c.setEnabled(true);
                                                            toast(m);
                                                        }
                                                    }
                                                });
                                    }));
        } else {
            box.setVisibility(View.GONE);
        }
    }

    private GestorPedidos.Vista vista() {
        GestorPedidos.Vista v = ped != null ? ped : new GestorPedidos.Vista();
        v.pedidoCoopId = pedidoCoopId;
        if (v.pedidoId == null) v.pedidoId = pedidoId;
        return v;
    }
}
