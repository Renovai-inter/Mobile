package com.example.renovai.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.example.renovai.EmpresaBottomNav;
import com.example.renovai.EmpresaData;
import com.example.renovai.R;
import com.example.renovai.dto.response.EmpresaResponses;

import java.util.List;

/** Tela 5.5 — Cooperativas Favoritas. */
public class EmpresaFavoritasActivity extends EmpresaBaseActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        preparar(R.layout.activity_empresa_favoritas, EmpresaBottomNav.Aba.FAVORITAS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!vivo()) return;
        EmpresaData.meuPerfil(
                false,
                (p, c) -> {
                    if (vivo()) preencherHeader(p.nomeEmpresa);
                });
        carregar();
    }

    private void carregar() {
        EmpresaData.favoritos(
                false,
                new com.example.renovai.GestorData.Ouvinte<List<EmpresaResponses.Favorito>>() {
                    @Override
                    public void aoReceber(List<EmpresaResponses.Favorito> l, boolean c) {
                        if (vivo()) render(l);
                    }

                    @Override
                    public void aoErro(String m) {
                        if (vivo()) toast(m);
                    }
                });
    }

    private void render(List<EmpresaResponses.Favorito> lista) {
        LinearLayout raiz = findViewById(R.id.listaFavoritas);
        raiz.removeAllViews();
        findViewById(R.id.txtVazioFav).setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
        for (EmpresaResponses.Favorito f : lista) {
            View v =
                    LayoutInflater.from(this)
                            .inflate(R.layout.item_empresa_cooperativa_busca, raiz, false);
            EmpresaCooperativaCard.bind(
                    this,
                    v,
                    f.cooperativaId,
                    f.cooperativaNome,
                    null,
                    true,
                    () -> {
                        EmpresaData.invalidar("empresa:favoritos");
                        carregar();
                    });
            EmpresaData.api()
                    .perfilPublico(f.cooperativaId)
                    .enqueue(
                            new retrofit2.Callback<EmpresaResponses.CooperativaPerfilPublico>() {
                                @Override
                                public void onResponse(
                                        retrofit2.Call<EmpresaResponses.CooperativaPerfilPublico> c,
                                        retrofit2.Response<
                                                        EmpresaResponses.CooperativaPerfilPublico>
                                                r) {
                                    if (!vivo() || !r.isSuccessful() || r.body() == null) return;
                                    StringBuilder materiais = new StringBuilder();
                                    if (r.body().materiaisDisponiveis != null)
                                        for (com.example.renovai.dto.response.GestorResponses
                                                        .Estoque
                                                e : r.body().materiaisDisponiveis) {
                                            if (materiais.length() > 0) materiais.append(" · ");
                                            materiais.append(e.materialCategoria);
                                        }
                                    ((android.widget.TextView)
                                                    v.findViewById(R.id.txtCoopMateriaisE))
                                            .setText(
                                                    materiais.length() == 0
                                                            ? "Sem estoque disponível"
                                                            : materiais.toString());
                                }

                                @Override
                                public void onFailure(
                                        retrofit2.Call<EmpresaResponses.CooperativaPerfilPublico> c,
                                        Throwable t) {}
                            });
            raiz.addView(v);
        }
    }
}
