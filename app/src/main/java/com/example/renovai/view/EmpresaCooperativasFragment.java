package com.example.renovai.view;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.renovai.EmpresaData;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.dto.response.CategoriaMaterialResponse;
import com.example.renovai.dto.response.CooperativaResponse;
import com.example.renovai.dto.response.EmpresaResponses;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.*;

/** Busca por categoria, estoque mínimo e localização, com interesses pré-selecionados. */
public class EmpresaCooperativasFragment extends EmpresaAbaFragment {
    private List<CategoriaMaterialResponse> categorias = new ArrayList<>();
    private final Set<String> selecionadas = new LinkedHashSet<>();
    private final Map<String, CooperativaResponse> resultados = new LinkedHashMap<>();
    private final Map<String, EmpresaResponses.CooperativaPerfilPublico> perfis = new HashMap<>();
    /** Perfis (com a nota) pedidos e que ainda não voltaram. */
    private final Set<String> perfisPendentes = new HashSet<>();
    private List<EmpresaResponses.Favorito> favoritos = new ArrayList<>();
    private boolean apenas4Mais, editaramFiltros;
    private int geracao;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable pesquisa = this::buscar;
    private final List<Call<?>> chamadas = new ArrayList<>();

    public EmpresaCooperativasFragment() {
        super(R.layout.activity_empresa_cooperativas);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle b) {
        super.onViewCreated(view, b);
        TextWatcher watcher =
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int a, int c, int d) {}

                    @Override
                    public void onTextChanged(CharSequence s, int a, int c, int d) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        editaramFiltros = true;
                        handler.removeCallbacks(pesquisa);
                        handler.postDelayed(pesquisa, 400);
                    }
                };
        ((EditText) findViewById(R.id.edtBuscaMaterial)).addTextChangedListener(watcher);
        ((EditText) findViewById(R.id.edtBuscaCidadeE)).addTextChangedListener(watcher);
        ((EditText) findViewById(R.id.edtBuscaQuantidadeE)).addTextChangedListener(watcher);
        findViewById(R.id.chipMelhorPreco).setOnClickListener(v -> escolherCategorias());
        findViewById(R.id.chipNota4)
                .setOnClickListener(
                        v -> {
                            apenas4Mais = !apenas4Mais;
                            render();
                        });
        EmpresaData.categorias(
                new com.example.renovai.GestorData.Ouvinte<List<CategoriaMaterialResponse>>() {
                    @Override
                    public void aoReceber(List<CategoriaMaterialResponse> l, boolean c) {
                        categorias = l;
                        buscar();
                    }

                    @Override
                    public void aoErro(String m) {
                        if (vivo()) toast(m);
                    }
                });
        EmpresaData.materiaisInteresse(
                false,
                new com.example.renovai.GestorData.Ouvinte<
                        List<EmpresaResponses.MaterialInteresse>>() {
                    @Override
                    public void aoReceber(List<EmpresaResponses.MaterialInteresse> l, boolean c) {
                        if (!editaramFiltros) {
                            selecionadas.clear();
                            for (EmpresaResponses.MaterialInteresse i : l)
                                selecionadas.add(i.categoriaId);
                            buscar();
                        }
                    }

                    @Override
                    public void aoErro(String m) {
                        if (vivo()) toast(m);
                    }
                });
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
        carregarFavoritos();
    }

    private void carregarFavoritos() {
        EmpresaData.favoritos(
                false,
                (l, c) -> {
                    favoritos = l;
                    if (vivo()) render();
                });
    }

    private void escolherCategorias() {
        if (categorias.isEmpty()) {
            toast("Aguarde as categorias.");
            return;
        }
        String[] nomes = new String[categorias.size()];
        boolean[] marcados = new boolean[nomes.length];
        Set<String> novos = new LinkedHashSet<>(selecionadas);
        for (int i = 0; i < nomes.length; i++) {
            nomes[i] = categorias.get(i).getNomeCategoria();
            marcados[i] = novos.contains(categorias.get(i).getCategoriaId());
        }
        new MaterialAlertDialogBuilder(tela())
                .setTitle("Categorias de material")
                .setMultiChoiceItems(
                        nomes,
                        marcados,
                        (d, i, checked) -> {
                            if (checked) novos.add(categorias.get(i).getCategoriaId());
                            else novos.remove(categorias.get(i).getCategoriaId());
                        })
                .setPositiveButton(
                        "Buscar",
                        (d, w) -> {
                            editaramFiltros = true;
                            selecionadas.clear();
                            selecionadas.addAll(novos);
                            buscar();
                        })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void buscar() {
        if (!vivo()) return;
        final int versao = ++geracao;
        for (Call<?> c : chamadas) c.cancel();
        chamadas.clear();
        String texto = ((EditText) findViewById(R.id.edtBuscaMaterial)).getText().toString().trim();
        String cidade = ((EditText) findViewById(R.id.edtBuscaCidadeE)).getText().toString().trim();
        String quantidade =
                ((EditText) findViewById(R.id.edtBuscaQuantidadeE)).getText().toString().trim();
        Double peso = quantidade.isEmpty() ? null : GestorUi.parseValor(quantidade);
        if (!quantidade.isEmpty() && (peso == null || !Double.isFinite(peso) || peso < 0)) {
            toast("Informe uma quantidade mínima válida.");
            return;
        }
        Set<String> filtros = new LinkedHashSet<>(selecionadas);
        if (!texto.isEmpty()) {
            filtros.clear();
            for (CategoriaMaterialResponse c : categorias)
                if (GestorUi.norm(c.getNomeCategoria()).contains(GestorUi.norm(texto)))
                    filtros.add(c.getCategoriaId());
            if (filtros.isEmpty()) {
                resultados.clear();
                render();
                return;
            }
        }
        if (filtros.isEmpty()) filtros.add(null);
        resultados.clear();
        perfis.clear();
        perfisPendentes.clear();
        ((TextView) findViewById(R.id.chipMelhorPreco))
                .setText("Categorias (" + selecionadas.size() + ")");
        findViewById(R.id.progressoBuscaE).setVisibility(View.VISIBLE);
        java.util.concurrent.atomic.AtomicInteger pendentes =
                new java.util.concurrent.atomic.AtomicInteger(filtros.size());
        for (String categoria : filtros) {
            Call<List<CooperativaResponse>> call =
                    EmpresaData.api()
                            .buscarCooperativas(
                                    categoria,
                                    cidade.isEmpty() ? null : cidade,
                                    peso == null ? null : String.valueOf(peso));
            chamadas.add(call);
            call.enqueue(
                    new Callback<List<CooperativaResponse>>() {
                        @Override
                        public void onResponse(
                                Call<List<CooperativaResponse>> c,
                                Response<List<CooperativaResponse>> r) {
                            if (!vivo() || versao != geracao) return;
                            if (r.isSuccessful() && r.body() != null)
                                for (CooperativaResponse coop : r.body()) {
                                    if (resultados.put(coop.getCooperativaId(), coop) == null)
                                        carregarPerfil(coop.getCooperativaId(), versao);
                                }
                            else toast(EmpresaData.erro(r));
                            fim();
                        }

                        @Override
                        public void onFailure(Call<List<CooperativaResponse>> c, Throwable t) {
                            if (!vivo() || versao != geracao || c.isCanceled()) return;
                            toast("Sem conexão com o servidor.");
                            fim();
                        }

                        private void fim() {
                            if (pendentes.decrementAndGet() == 0)
                                findViewById(R.id.progressoBuscaE).setVisibility(View.GONE);
                            render();
                        }
                    });
        }
    }

    private void carregarPerfil(String id, int versao) {
        perfisPendentes.add(id);
        Call<EmpresaResponses.CooperativaPerfilPublico> call = EmpresaData.api().perfilPublico(id);
        chamadas.add(call);
        call.enqueue(
                new Callback<EmpresaResponses.CooperativaPerfilPublico>() {
                    @Override
                    public void onResponse(
                            Call<EmpresaResponses.CooperativaPerfilPublico> c,
                            Response<EmpresaResponses.CooperativaPerfilPublico> r) {
                        if (!vivo() || versao != geracao) return;
                        perfisPendentes.remove(id);
                        if (r.isSuccessful() && r.body() != null) perfis.put(id, r.body());
                        render();
                    }

                    @Override
                    public void onFailure(
                            Call<EmpresaResponses.CooperativaPerfilPublico> c, Throwable t) {
                        if (!vivo() || versao != geracao || c.isCanceled()) return;
                        perfisPendentes.remove(id); // sem nota: não entra no 4+, mas não trava
                        render();
                    }
                });
    }

    private void render() {
        if (!vivo()) return;
        // o botão mostra se o filtro está ligado: verde com texto branco, ou claro com texto escuro
        TextView chip = findViewById(R.id.chipNota4);
        chip.setBackgroundResource(
                apenas4Mais ? R.drawable.btn_green_background : R.drawable.card_unselected_background);
        chip.setTextColor(apenas4Mais ? Color.WHITE : Color.parseColor("#444444"));

        List<CooperativaResponse> lista = new ArrayList<>();
        int esperandoNota = 0;
        for (CooperativaResponse coop : resultados.values()) {
            EmpresaResponses.CooperativaPerfilPublico p = perfis.get(coop.getCooperativaId());
            Double nota = p == null ? null : p.mediaAvaliacoes;
            if (apenas4Mais && (nota == null || nota < 4)) {
                if (p == null && perfisPendentes.contains(coop.getCooperativaId())) esperandoNota++;
                continue;
            }
            lista.add(coop);
        }
        // com o 4+ ligado, as mais bem avaliadas primeiro
        if (apenas4Mais) lista.sort((a, b) -> Double.compare(nota(b), nota(a)));

        LinearLayout raiz = findViewById(R.id.listaBusca);
        raiz.removeAllViews();
        for (CooperativaResponse coop : lista) {
            EmpresaResponses.CooperativaPerfilPublico p = perfis.get(coop.getCooperativaId());
            View v =
                    LayoutInflater.from(tela())
                            .inflate(R.layout.item_empresa_cooperativa_busca, raiz, false);
            EmpresaCooperativaCard.bind(
                    tela(),
                    v,
                    coop.getCooperativaId(),
                    coop.getNome(),
                    coop.getImagemUrl() != null ? coop.getImagemUrl() : p == null ? null : p.imagemUrl,
                    p == null ? null : p.mediaAvaliacoes,
                    EmpresaCooperativaCard.estaFavoritada(favoritos, coop.getCooperativaId()),
                    this::carregarFavoritos);
            EmpresaCooperativaCard.detalhes(
                    v,
                    primeiro(coop.getDescricao(), p == null ? null : p.descricao),
                    primeiro(coop.getCidade(), p == null ? null : p.cidade),
                    primeiro(coop.getHorarioFuncionamento(), p == null ? null : p.horarioFuncionamento),
                    primeiro(coop.getContatoPreferencial(), p == null ? null : p.contatoPreferencial),
                    coop.getNumeroCooperados());
            raiz.addView(v);
        }
        TextView vazio = findViewById(R.id.txtVazioBusca);
        boolean carregandoNotas = lista.isEmpty() && esperandoNota > 0;
        vazio.setText(carregandoNotas
                ? "Carregando as avaliações das cooperativas…"
                : apenas4Mais
                        ? "Nenhuma cooperativa com nota 4 ou mais nesta busca."
                        : "Nenhuma cooperativa encontrada com esses filtros.");
        vazio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private double nota(CooperativaResponse coop) {
        EmpresaResponses.CooperativaPerfilPublico p = perfis.get(coop.getCooperativaId());
        return p == null || p.mediaAvaliacoes == null ? 0 : p.mediaAvaliacoes;
    }

    private static String primeiro(String a, String b) {
        return a != null && !a.trim().isEmpty() ? a : b;
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(pesquisa);
        for (Call<?> c : chamadas) c.cancel();
        super.onDestroyView();
    }
}
