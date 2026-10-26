package com.example.renovai.view;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renovai.GestorData;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.adapter.ListaAdapter;
import com.example.renovai.dto.response.GestorResponses;

import java.util.ArrayList;
import java.util.List;

/** Tela 4.3 — Estoque atual da cooperativa: material, peso e data da última atualização. */
public class GestorEstoqueFragment extends GestorAbaFragment {

    private ListaAdapter<GestorResponses.Estoque> adapter;

    public GestorEstoqueFragment() {
        super(R.layout.activity_gestor_estoque);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle b) {
        super.onViewCreated(view, b);
        adapter =
                new ListaAdapter<>(
                        R.layout.item_gestor_estoque,
                        (v, e, i) -> {
                            ((TextView) v.findViewById(R.id.txtEstoqueNome))
                                    .setText(
                                            e.materialCategoria == null
                                                    ? "Material"
                                                    : e.materialCategoria);
                            ((TextView) v.findViewById(R.id.txtEstoquePeso))
                                    .setText("Peso: " + GestorUi.kg(e.quantidadeKg));
                            ((TextView) v.findViewById(R.id.txtEstoqueData))
                                    .setText("Atualizado em: " + GestorUi.data(e.dataAtualizacao));
                        });
        RecyclerView r = findViewById(R.id.recyclerEstoque);
        float larguraDp =
                getResources().getDisplayMetrics().widthPixels
                        / getResources().getDisplayMetrics().density;
        int colunas = larguraDp < 360 || getResources().getConfiguration().fontScale > 1.2f ? 1 : 2;
        r.setLayoutManager(new GridLayoutManager(tela(), colunas));
        r.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!vivo() || funcId() == null) return;
        GestorData.estoque(
                false,
                new GestorData.Ouvinte<List<GestorResponses.Estoque>>() {
                    @Override
                    public void aoReceber(List<GestorResponses.Estoque> l, boolean c) {
                        if (!vivo()) return;
                        List<GestorResponses.Estoque> s = new ArrayList<>(l);
                        s.sort(
                                (a, x) ->
                                        String.valueOf(a.materialCategoria)
                                                .compareToIgnoreCase(
                                                        String.valueOf(x.materialCategoria)));
                        double total = 0;
                        for (GestorResponses.Estoque e : s)
                            total += e.quantidadeKg == null ? 0 : e.quantidadeKg;
                        ((TextView) findViewById(R.id.txtEstoqueTotal))
                                .setText(
                                        s.isEmpty()
                                                ? ""
                                                : "Total em estoque: " + GestorUi.kg(total));
                        findViewById(R.id.txtVazio)
                                .setVisibility(s.isEmpty() ? View.VISIBLE : View.GONE);
                        adapter.submit(s);
                    }

                    @Override
                    public void aoErro(String m) {
                        if (vivo()) toast(m);
                    }
                });
    }
}
