package com.example.renovai.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.renovai.EmpresaPedidos;
import com.example.renovai.EmpresaStatus;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.dto.response.PedidoResponse;
import com.example.renovai.dto.response.PerfilResponse;

import java.util.ArrayList;
import java.util.List;

/** Home do Admin do site: totais de cooperativas, empresas, contas e pedidos de todo o sistema. */
public class AdminHomeFragment extends AdminAbaFragment {

    private static final Runnable NADA = () -> {};

    public AdminHomeFragment() {
        super(R.layout.fragment_admin_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle b) {
        super.onViewCreated(view, b);
        findViewById(R.id.cardKpiCooperativas).setOnClickListener(v -> abrirAba(R.id.nav_a_cooperativas));
        findViewById(R.id.cardKpiEmpresas).setOnClickListener(v -> abrirAba(R.id.nav_a_empresas));
        findViewById(R.id.cardKpiContas).setOnClickListener(v -> abrirAba(R.id.nav_a_contas));
    }

    @Override
    public void onResume() {
        super.onResume();
        chamar(api().cooperativas(), l -> numero(R.id.txtKpiCooperativas, l == null ? 0 : l.size()), NADA);
        chamar(api().empresas(), l -> numero(R.id.txtKpiEmpresas, l == null ? 0 : l.size()), NADA);
        chamar(api().perfis(), this::mostrarContas, NADA);
        chamar(api().pedidos(), this::mostrarPedidos, NADA);
    }

    private void numero(int id, int n) {
        ((TextView) findViewById(id)).setText(String.format(GestorUi.BR, "%02d", n));
    }

    private void mostrarContas(List<PerfilResponse> perfis) {
        int ativas = 0, total = perfis == null ? 0 : perfis.size();
        if (perfis != null) for (PerfilResponse p : perfis) if (Boolean.TRUE.equals(p.getEstaAtivo())) ativas++;
        numero(R.id.txtKpiContas, total);
        ((TextView) findViewById(R.id.txtKpiContasDet))
                .setText(ativas + " Ativas\n" + (total - ativas) + " Inativas");
    }

    private void mostrarPedidos(List<PedidoResponse> pedidos) {
        List<PedidoResponse> lista = pedidos == null ? new ArrayList<>() : new ArrayList<>(pedidos);
        int concluidos = 0;
        for (PedidoResponse p : lista) if (p.getDataConclusao() != null) concluidos++;
        numero(R.id.txtKpiPedidos, lista.size());
        ((TextView) findViewById(R.id.txtKpiPedidosDet))
                .setText((lista.size() - concluidos) + " Em aberto\n" + concluidos + " Concluídos");

        lista.sort((a, c) -> String.valueOf(c.getDataPedido()).compareTo(String.valueOf(a.getDataPedido())));
        LinearLayout raiz = findViewById(R.id.listaPedidosRecentes);
        raiz.removeAllViews();
        findViewById(R.id.txtSemPedidos).setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
        for (PedidoResponse p : lista.subList(0, Math.min(5, lista.size()))) {
            View v = LayoutInflater.from(requireContext()).inflate(R.layout.item_empresa_pedido_recente, raiz, false);
            String empresa = p.getEmpresaNome() == null ? "Empresa" : p.getEmpresaNome();
            ((TextView) v.findViewById(R.id.txtPedidoRecenteSub))
                    .setText(empresa + " — " + GestorUi.idCurto(p.getPedidoId())
                            + (p.getValorTotal() != null ? " — " + GestorUi.dinheiro(p.getValorTotal()) : ""));
            ((TextView) v.findViewById(R.id.txtPedidoRecenteData)).setText(GestorUi.data(p.getDataPedido()));
            TextView badge = v.findViewById(R.id.txtPedidoRecenteBadge);
            int estado = EmpresaStatus.estado(null, p.getStatusAtual(), p.getDataConclusao());
            badge.setText(EmpresaPedidos.rotulo(estado));
            GestorUi.badge(badge, EmpresaStatus.cor(estado));
            badge.setCompoundDrawables(null, null, null, null);
            raiz.addView(v);
        }
    }
}
