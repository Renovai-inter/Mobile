package com.example.renovai.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.example.renovai.EmpresaBottomNav;
import com.example.renovai.EmpresaPedidos;
import com.example.renovai.GestorUi;
import com.example.renovai.R;

import java.util.ArrayList;
import java.util.List;

/** Tela 5.4 — Listagem de Pedidos da empresa, com abas Todos/Ativos/Concluídos. */
public class EmpresaPedidosActivity extends EmpresaBaseActivity {

    private List<EmpresaPedidos.Vista> todos = new ArrayList<>();
    private int aba = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_empresa_pedidos, EmpresaBottomNav.Aba.PEDIDOS)) return;
        GestorUi.segmentado(
                findViewById(R.id.trilhaPedidos),
                new String[] {
                    "Todos", "Aberto", "Em negociação", "Aceito", "Recusado", "Finalizado"
                },
                0,
                true,
                i -> {
                    aba = i;
                    render();
                });
        LinearLayout trilha = findViewById(R.id.trilhaPedidos);
        for (int i = 0; i < trilha.getChildCount(); i++) {
            View filtro = trilha.getChildAt(i);
            filtro.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
            filtro.setMinimumWidth(GestorUi.dp(this, 88));
            filtro.setPadding(
                    GestorUi.dp(this, 12),
                    GestorUi.dp(this, 12),
                    GestorUi.dp(this, 12),
                    GestorUi.dp(this, 12));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!vivo()) return;
        com.example.renovai.EmpresaData.meuPerfil(
                false,
                (p, c) -> {
                    if (vivo()) preencherHeader(p.nomeEmpresa);
                });
        EmpresaPedidos.listar(
                new com.example.renovai.GestorData.Ouvinte<List<EmpresaPedidos.Vista>>() {
                    @Override
                    public void aoReceber(List<EmpresaPedidos.Vista> l, boolean c) {
                        todos = l;
                        if (vivo()) render();
                    }

                    @Override
                    public void aoErro(String m) {
                        if (vivo()) toast(m);
                    }
                });
    }

    private void render() {
        List<EmpresaPedidos.Vista> f = new ArrayList<>();
        for (EmpresaPedidos.Vista v : todos) {
            boolean ativo = v.estado == GestorUi.EM_NEGOCIACAO || v.estado == GestorUi.OUTRO;
            if (aba == 0
                    || (aba == 1 && v.estado == GestorUi.OUTRO)
                    || (aba == 2 && v.estado == GestorUi.EM_NEGOCIACAO)
                    || (aba == 3 && v.estado == GestorUi.CONCLUIDO)
                    || (aba == 4 && v.estado == GestorUi.RECUSADO)
                    || (aba == 5 && v.estado == com.example.renovai.EmpresaStatus.FINALIZADO))
                f.add(v);
        }
        double total = 0;
        for (EmpresaPedidos.Vista v : todos)
            if (com.example.renovai.EmpresaStatus.aprovado(v.estado)) total += v.valor;
        ((android.widget.TextView) findViewById(R.id.txtTotalAprovadoE))
                .setText(GestorUi.dinheiro(total));
        LinearLayout raiz = findViewById(R.id.listaPedidos);
        raiz.removeAllViews();
        findViewById(R.id.txtVazioPedidos).setVisibility(f.isEmpty() ? View.VISIBLE : View.GONE);
        for (EmpresaPedidos.Vista v : f) {
            View item =
                    LayoutInflater.from(this).inflate(R.layout.item_empresa_pedido, raiz, false);
            EmpresaPedidoCard.bind(this, item, v);
            raiz.addView(item);
        }
    }
}
