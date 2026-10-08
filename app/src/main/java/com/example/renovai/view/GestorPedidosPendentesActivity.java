package com.example.renovai.view;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renovai.GestorBottomNav;
import com.example.renovai.GestorData;
import com.example.renovai.GestorPedidos;
import com.example.renovai.R;
import com.example.renovai.adapter.ListaAdapter;

import java.util.List;

/** Pedidos pendentes: pedidos enviados à cooperativa aguardando Aceitar ou Recusar. */
public class GestorPedidosPendentesActivity extends GestorBaseActivity {

    private ListaAdapter<GestorPedidos.Vista> adapter;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_gestor_lista, GestorBottomNav.Aba.NENHUMA)) return;
        titulo("Pedidos pendentes");
        ((TextView) findViewById(R.id.txtTituloLista)).setText("Pedidos Pendentes");
        ((TextView) findViewById(R.id.txtSubtituloLista)).setText("Gerencie os pedidos enviados a sua cooperativa");
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
        GestorPedidos.pendentes(new GestorData.Ouvinte<List<GestorPedidos.Vista>>() {
            @Override public void aoReceber(List<GestorPedidos.Vista> l, boolean c) {
                if (!vivo()) return;
                TextView vazio = findViewById(R.id.txtVazio);
                vazio.setText("Nenhum pedido pendente.");
                vazio.setVisibility(l.isEmpty() ? View.VISIBLE : View.GONE);
                adapter.submit(l);
            }
            @Override public void aoErro(String m) { if (vivo()) toast(m); }
        });
    }
}
