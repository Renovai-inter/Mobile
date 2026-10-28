package com.example.renovai.view;


import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.renovai.EmpresaBottomNav;
import com.example.renovai.EmpresaData;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.dto.request.EmpresaRequests;
import com.example.renovai.dto.response.EmpresaResponses;
import com.example.renovai.dto.response.GestorResponses;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

/** Tela 5.2.1 — Perfil Público da Cooperativa (abas Sobre / Materiais / Comentários). */
public class EmpresaPerfilPublicoActivity extends EmpresaBaseActivity {

    public static final String EXTRA_COOPERATIVA_ID = "cooperativaId";

    private String cooperativaId;
    private EmpresaResponses.CooperativaPerfilPublico perfil;
    private List<EmpresaResponses.Favorito> favoritos = new java.util.ArrayList<>();
    private boolean favoritada = false;
    private int aba = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_empresa_perfil_publico, EmpresaBottomNav.Aba.NENHUMA))
            return;
        titulo("Perfil");
        cooperativaId = getIntent().getStringExtra(EXTRA_COOPERATIVA_ID);

        if (cooperativaId == null) {
            toast("Cooperativa não informada.");
            finish();
            return;
        }
        GestorUi.segmentado(
                findViewById(R.id.trilhaPP),
                new String[] {"Sobre", "Materiais", "Comentários"},
                0,
                true,
                i -> {
                    aba = i;
                    mostrarAba();
                });
        findViewById(R.id.btnFazerPedidoPP).setOnClickListener(v -> abrirEnviarPedido());
        findViewById(R.id.btnComentarPP).setOnClickListener(v -> abrirComentar());

        EmpresaData.api()
                .perfilPublico(cooperativaId)
                .enqueue(
                        new Callback<EmpresaResponses.CooperativaPerfilPublico>() {
                            @Override
                            public void onResponse(
                                    Call<EmpresaResponses.CooperativaPerfilPublico> c,
                                    Response<EmpresaResponses.CooperativaPerfilPublico> r) {
                                if (r.isSuccessful() && r.body() != null && vivo()) {
                                    perfil = r.body();
                                    render();
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<EmpresaResponses.CooperativaPerfilPublico> c,
                                    Throwable t) {
                                if (vivo()) toast("Sem conexão com o servidor.");
                            }
                        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!vivo()) return;
        EmpresaData.favoritos(
                false,
                (l, c) -> {
                    favoritos = l;
                    favoritada = EmpresaCooperativaCard.estaFavoritada(l, cooperativaId);
                    // estrela cheia = favoritada; cada toque põe ou tira dos favoritos
                    if (vivo())
                        EmpresaCooperativaCard.estrela(
                                findViewById(R.id.imgFavPP), cooperativaId, favoritada, null);
                });
    }

    private void abrirEnviarPedido() {
        Intent i = new Intent(this, EmpresaEnviarPedidoActivity.class);
        i.putExtra(EmpresaEnviarPedidoActivity.EXTRA_COOPERATIVA_ID, cooperativaId);
        i.putExtra(
                EmpresaEnviarPedidoActivity.EXTRA_COOPERATIVA_NOME,
                perfil != null ? perfil.nome : null);
        startActivity(i);
    }

    private void render() {
        titulo("Perfil");
        ((TextView) findViewById(R.id.txtNomePP))
                .setText(perfil.nome == null ? "Cooperativa" : perfil.nome);
        ((TextView) findViewById(R.id.txtIniciaisPP)).setText(GestorUi.iniciais(perfil.nome));
        // foto da cooperativa (Cloudinary) por cima das iniciais, quando existe
        android.widget.ImageView foto = findViewById(R.id.imgFotoPP);
        boolean temFoto = com.example.renovai.ImagemUrl.normalizar(perfil.imagemUrl) != null;
        foto.setVisibility(temFoto ? View.VISIBLE : View.GONE);
        if (temFoto) EmpresaCooperativaCard.foto(foto, perfil.imagemUrl);
        ((TextView) findViewById(R.id.txtNotaPP))
                .setText(
                        (perfil.mediaAvaliacoes == null
                                        ? "Sem nota"
                                        : String.format(
                                                GestorUi.BR, "%.1f", perfil.mediaAvaliacoes))
                                + " ("
                                + (perfil.totalAvaliacoes == null ? 0 : perfil.totalAvaliacoes)
                                + " avaliações)");
        ((TextView) findViewById(R.id.txtDescricaoPP))
                .setText(
                        perfil.descricao == null || perfil.descricao.isBlank()
                                ? "Sem descrição."
                                : perfil.descricao);
        ((TextView) findViewById(R.id.txtEnderecoPP))
                .setText(
                        perfil.endereco == null
                                ? (perfil.cidade == null ? "Não informado" : perfil.cidade)
                                : perfil.endereco);
        ((TextView) findViewById(R.id.txtHorarioPP))
                .setText(
                        perfil.horarioFuncionamento == null || perfil.horarioFuncionamento.isBlank()
                                ? "Não informado"
                                : perfil.horarioFuncionamento);
        ((TextView) findViewById(R.id.txtContatoPP))
                .setText(perfil.contato() == null ? "Não informado" : perfil.contato());

        findViewById(R.id.txtContatoPP)
                .setOnClickListener(
                        v -> {
                            android.net.Uri uri =
                                    perfil.telefone != null && !perfil.telefone.isBlank()
                                            ? android.net.Uri.parse("tel:" + perfil.telefone)
                                            : android.net.Uri.parse(
                                                    "mailto:"
                                                            + (perfil.email == null
                                                                    ? ""
                                                                    : perfil.email));
                            try {
                                startActivity(
                                        new Intent(
                                                perfil.telefone != null
                                                                && !perfil.telefone.isBlank()
                                                        ? Intent.ACTION_DIAL
                                                        : Intent.ACTION_SENDTO,
                                                uri));
                            } catch (android.content.ActivityNotFoundException e) {
                                toast("Nenhum aplicativo de contato disponível.");
                            }
                        });
        renderMateriais();

        carregarEstrelas();
        carregarComentarios();
        mostrarAba();
    }

    private void mostrarAba() {
        findViewById(R.id.abaSobre).setVisibility(aba == 0 ? View.VISIBLE : View.GONE);
        findViewById(R.id.abaMateriais).setVisibility(aba == 1 ? View.VISIBLE : View.GONE);
        findViewById(R.id.abaComentarios).setVisibility(aba == 2 ? View.VISIBLE : View.GONE);
    }

    private void carregarEstrelas() {
        EmpresaData.api()
                .distribuicaoEstrelas(cooperativaId)
                .enqueue(
                        new Callback<EmpresaResponses.DistribuicaoEstrelas>() {
                            @Override
                            public void onResponse(
                                    Call<EmpresaResponses.DistribuicaoEstrelas> c,
                                    Response<EmpresaResponses.DistribuicaoEstrelas> r) {
                                if (!r.isSuccessful() || r.body() == null || !vivo()) return;
                                EmpresaResponses.DistribuicaoEstrelas d = r.body();
                                long total =
                                        d.estrelas1
                                                + d.estrelas2
                                                + d.estrelas3
                                                + d.estrelas4
                                                + d.estrelas5;
                                ((TextView) findViewById(R.id.txtQtdAvalPP))
                                        .setText(
                                                total
                                                        + (total == 1
                                                                ? " avaliação"
                                                                : " avaliações"));
                                LinearLayout raiz = findViewById(R.id.barrasEstrelas);
                                raiz.removeAllViews();
                                long[] contagens = {
                                    d.estrelas5, d.estrelas4, d.estrelas3, d.estrelas2, d.estrelas1
                                };
                                for (int i = 0; i < 5; i++) {
                                    View v =
                                            LayoutInflater.from(EmpresaPerfilPublicoActivity.this)
                                                    .inflate(
                                                            R.layout.item_empresa_barra_estrela,
                                                            raiz,
                                                            false);
                                    ((TextView) v.findViewById(R.id.txtNumEstrela))
                                            .setText(String.valueOf(5 - i));
                                    int pct =
                                            total == 0
                                                    ? 0
                                                    : (int)
                                                            Math.round(
                                                                    contagens[i] * 100.0 / total);
                                    ((ProgressBar) v.findViewById(R.id.barraEstrela))
                                            .setProgress(pct);
                                    raiz.addView(v);
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<EmpresaResponses.DistribuicaoEstrelas> c, Throwable t) {}
                        });
    }

    private String pedidoAvaliacao;

    private void carregarComentarios() {
        EmpresaData.api()
                .avaliacoesPorAvaliado(cooperativaId)
                .enqueue(
                        new Callback<List<EmpresaResponses.Avaliacao>>() {
                            @Override
                            public void onResponse(
                                    Call<List<EmpresaResponses.Avaliacao>> c,
                                    Response<List<EmpresaResponses.Avaliacao>> r) {
                                if (!r.isSuccessful() || r.body() == null || !vivo()) return;
                                LinearLayout raiz = findViewById(R.id.listaComentariosPP);
                                raiz.removeAllViews();
                                findViewById(R.id.txtSemComentariosPP)
                                        .setVisibility(
                                                r.body().isEmpty() ? View.VISIBLE : View.GONE);
                                for (EmpresaResponses.Avaliacao av : r.body()) {
                                    View v =
                                            LayoutInflater.from(EmpresaPerfilPublicoActivity.this)
                                                    .inflate(
                                                            R.layout.item_empresa_avaliacao,
                                                            raiz,
                                                            false);
                                    ((TextView) v.findViewById(R.id.txtAvatarAval))
                                            .setText(GestorUi.iniciais(av.avaliadorNome));
                                    ((TextView) v.findViewById(R.id.txtNomeAval))
                                            .setText(
                                                    av.avaliadorNome == null
                                                            ? "Empresa"
                                                            : av.avaliadorNome);
                                    ((TextView) v.findViewById(R.id.txtNotaAval))
                                            .setText("★ " + av.nota);
                                    ((TextView) v.findViewById(R.id.txtComentarioAval))
                                            .setText(av.comentario);
                                    ((TextView) v.findViewById(R.id.txtDataAval))
                                            .setText(GestorUi.data(av.dataAvaliacao));
                                    raiz.addView(v);
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<List<EmpresaResponses.Avaliacao>> c, Throwable t) {
                                if (vivo()) toast("Não foi possível carregar os comentários.");
                            }
                        });
    }

    private void abrirComentar() {
        com.example.renovai.EmpresaPedidos.listar(
                new com.example.renovai.GestorData.Ouvinte<
                        List<com.example.renovai.EmpresaPedidos.Vista>>() {
                    @Override
                    public void aoReceber(
                            List<com.example.renovai.EmpresaPedidos.Vista> lista, boolean doCache) {
                        if (!vivo()) return;
                        pedidoAvaliacao = null;
                        for (com.example.renovai.EmpresaPedidos.Vista v : lista)
                            if (cooperativaId.equals(v.cooperativaId)
                                    && com.example.renovai.EmpresaStatus.aprovado(v.estado)) {
                                pedidoAvaliacao = v.pedidoId;
                                break;
                            }
                        if (pedidoAvaliacao == null) {
                            toast(
                                    "Avalie após um pedido aceito ou concluído com esta"
                                            + " cooperativa.");
                            return;
                        }
                        android.widget.RatingBar rating =
                                new android.widget.RatingBar(EmpresaPerfilPublicoActivity.this);
                        rating.setNumStars(5);
                        rating.setStepSize(1f);
                        rating.setRating(5f);
                        android.widget.EditText texto =
                                new android.widget.EditText(EmpresaPerfilPublicoActivity.this);
                        texto.setHint("Escreva um comentário (opcional)");
                        LinearLayout box = new LinearLayout(EmpresaPerfilPublicoActivity.this);
                        box.setOrientation(LinearLayout.VERTICAL);
                        int m = GestorUi.dp(EmpresaPerfilPublicoActivity.this, 20);
                        box.setPadding(m, GestorUi.dp(EmpresaPerfilPublicoActivity.this, 8), m, 0);
                        box.addView(rating);
                        box.addView(
                                texto,
                                new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT));
                        new MaterialAlertDialogBuilder(
                                        EmpresaPerfilPublicoActivity.this)
                                .setTitle("Avaliar cooperativa")
                                .setView(box)
                                .setPositiveButton(
                                        "Enviar",
                                        (d, w) ->
                                                enviarAvaliacao(
                                                        null,
                                                        Math.round(rating.getRating()),
                                                        texto.getText().toString().trim()))
                                .setNegativeButton("Cancelar", null)
                                .show();
                    }

                    @Override
                    public void aoErro(String m) {
                        toast(m);
                    }
                });
    }

    private void enviarAvaliacao(String meuId, int nota, String comentario) {
        if (comentario.length() > 4000) {
            toast("O comentário deve ter até 4000 caracteres.");
            return;
        }
        EmpresaData.api()
                .criarAvaliacao(
                        new EmpresaRequests.Avaliacao(
                                null,
                                cooperativaId,
                                pedidoAvaliacao,
                                Math.max(1, nota),
                                comentario.isEmpty() ? null : comentario))
                .enqueue(
                        new Callback<EmpresaResponses.Avaliacao>() {
                            @Override
                            public void onResponse(
                                    Call<EmpresaResponses.Avaliacao> c,
                                    Response<EmpresaResponses.Avaliacao> r) {
                                if (!vivo()) return;
                                if (r.isSuccessful()) {
                                    toast("Avaliação enviada.");
                                    carregarEstrelas();
                                    carregarComentarios();
                                } else toast(EmpresaData.erro(r));
                            }

                            @Override
                            public void onFailure(Call<EmpresaResponses.Avaliacao> c, Throwable t) {
                                toast("Sem conexão com o servidor.");
                            }
                        });
    }

    /**
     * Aba Materiais: cada material em estoque com a foto e as especificações ao lado do nome
     * (categoria, preço sugerido por kg, se está disponível e quando o estoque foi atualizado).
     * O estoque vem no perfil público; os dados do material (GET /empresas-conta/materiais) e a
     * categoria-pai (GET /categorias-material) chegam depois e completam cada linha.
     */
    private void renderMateriais() {
        LinearLayout mats = findViewById(R.id.listaMateriaisPP);
        mats.removeAllViews();
        List<GestorResponses.Estoque> disp = perfil.materiaisDisponiveis;
        findViewById(R.id.txtSemMateriaisPP)
                .setVisibility(disp == null || disp.isEmpty() ? View.VISIBLE : View.GONE);
        if (disp == null || disp.isEmpty()) return;
        java.util.Map<String, View> linhas = new java.util.HashMap<>();
        for (GestorResponses.Estoque e : disp) {
            View v = LayoutInflater.from(this).inflate(R.layout.item_empresa_estoque_linha, mats, false);
            ((TextView) v.findViewById(R.id.txtEstoqueLinhaNome))
                    .setText(e.materialCategoria == null ? "Material" : e.materialCategoria);
            ((TextView) v.findViewById(R.id.txtEstoqueLinhaKg)).setText(GestorUi.kg(e.quantidadeKg));
            especificacoes(v, e, null, null);
            if (e.materialId != null) linhas.put(e.materialId, v);
            mats.addView(v);
        }
        EmpresaData.api()
                .materiaisDisponiveis()
                .enqueue(
                        new Callback<List<com.example.renovai.dto.response.MaterialResponse>>() {
                            @Override
                            public void onResponse(
                                    Call<List<com.example.renovai.dto.response.MaterialResponse>> c,
                                    Response<List<com.example.renovai.dto.response.MaterialResponse>> r) {
                                if (!vivo() || !r.isSuccessful() || r.body() == null) return;
                                java.util.Map<String, com.example.renovai.dto.response.MaterialResponse> porId =
                                        new java.util.HashMap<>();
                                for (com.example.renovai.dto.response.MaterialResponse m : r.body())
                                    if (m.getMaterialId() != null) porId.put(m.getMaterialId(), m);
                                EmpresaData.categorias(
                                        new com.example.renovai.GestorData.Ouvinte<
                                                List<com.example.renovai.dto.response.CategoriaMaterialResponse>>() {
                                            @Override
                                            public void aoReceber(
                                                    List<com.example.renovai.dto.response.CategoriaMaterialResponse> cats,
                                                    boolean cache) {
                                                if (vivo()) completar(disp, linhas, porId, cats);
                                            }

                                            @Override
                                            public void aoErro(String m) {
                                                if (vivo()) completar(disp, linhas, porId, null);
                                            }
                                        });
                            }

                            @Override
                            public void onFailure(
                                    Call<List<com.example.renovai.dto.response.MaterialResponse>> c,
                                    Throwable t) {}
                        });
    }

    private void completar(
            List<GestorResponses.Estoque> disp,
            java.util.Map<String, View> linhas,
            java.util.Map<String, com.example.renovai.dto.response.MaterialResponse> porId,
            List<com.example.renovai.dto.response.CategoriaMaterialResponse> categorias) {
        for (GestorResponses.Estoque e : disp) {
            View v = e.materialId == null ? null : linhas.get(e.materialId);
            if (v == null) continue;
            com.example.renovai.dto.response.MaterialResponse m = porId.get(e.materialId);
            String pai = null;
            if (m != null && categorias != null)
                for (com.example.renovai.dto.response.CategoriaMaterialResponse cat : categorias)
                    if (m.getCategoriaId() != null && m.getCategoriaId().equalsIgnoreCase(cat.getCategoriaId()))
                        pai = cat.getCategoriaPaiNome();
            especificacoes(v, e, m, pai);
        }
    }

    /** Linha de especificações ao lado do nome; sem dado nenhum, a linha some. */
    private static void especificacoes(
            View v,
            GestorResponses.Estoque e,
            com.example.renovai.dto.response.MaterialResponse m,
            String categoriaPai) {
        List<String> partes = new java.util.ArrayList<>();
        if (categoriaPai != null && !categoriaPai.isBlank()) partes.add(categoriaPai);
        if (m != null && m.getPrecoSugerido() != null)
            partes.add(GestorUi.dinheiro(m.getPrecoSugerido().doubleValue()) + "/kg");
        if (m != null && m.getEstaDisponivel() != null)
            partes.add(Boolean.TRUE.equals(m.getEstaDisponivel()) ? "Disponível" : "Indisponível");
        if (e.dataAtualizacao != null) partes.add("Atualizado em " + GestorUi.data(e.dataAtualizacao));
        TextView detalhes = v.findViewById(R.id.txtEstoqueLinhaDetalhes);
        detalhes.setText(android.text.TextUtils.join(" · ", partes));
        detalhes.setVisibility(partes.isEmpty() ? View.GONE : View.VISIBLE);
        EmpresaCooperativaCard.foto(v.findViewById(R.id.imgEstoqueLinha), m == null ? null : m.getImagemUrl());
    }
}
