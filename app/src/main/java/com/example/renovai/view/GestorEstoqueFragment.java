package com.example.renovai.view;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renovai.ApiClient;
import com.example.renovai.GestorData;
import com.example.renovai.GestorUi;
import com.example.renovai.MaterialApiService;
import com.example.renovai.R;
import com.example.renovai.adapter.ListaAdapter;
import com.example.renovai.dto.response.CategoriaMaterialResponse;
import com.example.renovai.dto.response.GestorResponses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tela 4.3 — Estoque atual da cooperativa: tipo do material (categoria-pai, ex.: PET → Plástico),
 * material, peso e data da última atualização, com busca por material ou tipo.
 */
public class GestorEstoqueFragment extends GestorAbaFragment {

    private ListaAdapter<GestorResponses.Estoque> adapter;
    private EditText edtBusca;
    private boolean carregado;
    private final List<GestorResponses.Estoque> estoque = new ArrayList<>();
    /** Nome da categoria (normalizado) → nome da categoria-pai, que é o tipo do material. */
    private final Map<String, String> tipoPorCategoria = new HashMap<>();

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
                            String tipo = tipo(e);
                            TextView txtTipo = v.findViewById(R.id.txtEstoqueTipo);
                            txtTipo.setVisibility(tipo == null ? View.GONE : View.VISIBLE);
                            if (tipo != null) txtTipo.setText(tipo);
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
        edtBusca = findViewById(R.id.edtBuscaEstoque);
        edtBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void afterTextChanged(Editable s) { render(); }
        });
        carregarTipos();
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
                        carregado = true;
                        estoque.clear();
                        estoque.addAll(l);
                        estoque.sort(
                                (a, x) ->
                                        String.valueOf(a.materialCategoria)
                                                .compareToIgnoreCase(
                                                        String.valueOf(x.materialCategoria)));
                        double total = 0;
                        for (GestorResponses.Estoque e : estoque)
                            total += e.quantidadeKg == null ? 0 : e.quantidadeKg;
                        ((TextView) findViewById(R.id.txtEstoqueTotal))
                                .setText(
                                        estoque.isEmpty()
                                                ? ""
                                                : "Total em estoque: " + GestorUi.kg(total));
                        render();
                    }

                    @Override
                    public void aoErro(String m) {
                        if (vivo()) toast(m);
                    }
                });
    }

    /** O estoque só traz o nome da categoria; o tipo vem da categoria-pai (PET → Plástico). */
    private void carregarTipos() {
        ApiClient.createService(MaterialApiService.class)
                .categorias()
                .enqueue(
                        new Callback<List<CategoriaMaterialResponse>>() {
                            @Override
                            public void onResponse(
                                    Call<List<CategoriaMaterialResponse>> c,
                                    Response<List<CategoriaMaterialResponse>> r) {
                                if (!vivo() || !r.isSuccessful() || r.body() == null) return;
                                tipoPorCategoria.clear();
                                for (CategoriaMaterialResponse cat : r.body())
                                    if (cat.getNomeCategoria() != null
                                            && cat.getCategoriaPaiNome() != null
                                            && !cat.getCategoriaPaiNome().isEmpty())
                                        tipoPorCategoria.put(
                                                GestorUi.norm(cat.getNomeCategoria()),
                                                cat.getCategoriaPaiNome());
                                render();
                            }

                            @Override
                            public void onFailure(
                                    Call<List<CategoriaMaterialResponse>> c, Throwable t) {}
                        });
    }

    @Nullable
    private String tipo(GestorResponses.Estoque e) {
        return e.materialCategoria == null
                ? null
                : tipoPorCategoria.get(GestorUi.norm(e.materialCategoria));
    }

    /** Filtra pelo texto da busca, comparando sem acento com o material e o tipo. */
    private void render() {
        if (!vivo() || adapter == null) return;
        String busca = GestorUi.norm(edtBusca.getText().toString().trim());
        List<GestorResponses.Estoque> s = new ArrayList<>();
        for (GestorResponses.Estoque e : estoque)
            if (busca.isEmpty()
                    || GestorUi.norm(e.materialCategoria).contains(busca)
                    || GestorUi.norm(tipo(e)).contains(busca)) s.add(e);
        TextView vazio = findViewById(R.id.txtVazio);
        vazio.setText(
                estoque.isEmpty()
                        ? "Nenhum material em estoque."
                        : "Nenhum material encontrado para \"" + edtBusca.getText().toString().trim() + "\".");
        vazio.setVisibility(carregado && s.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.submit(s);
    }
}
