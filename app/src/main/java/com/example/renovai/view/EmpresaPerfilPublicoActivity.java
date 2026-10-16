package com.example.renovai.view;

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
        findViewById(R.id.imgFavPP).setOnClickListener(v -> alternarFavorito());
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
                    if (vivo()) atualizarEstrela();
                });
    }

    private void atualizarEstrela() {
        findViewById(R.id.imgFavPP).setAlpha(favoritada ? 1f : 0.4f);
    }

    private void alternarFavorito() {
        String empresaId = EmpresaData.empresaId();
        if (empresaId == null) return;
        if (favoritada) {
            EmpresaData.api()
                    .desfavoritar(cooperativaId)
                    .enqueue(
                            new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> c, Response<Void> r) {
                                    if (!vivo()) return;
                                    if (!r.isSuccessful()) {
                                        toast(EmpresaData.erro(r));
                                        return;
                                    }
                                    EmpresaData.invalidar("empresa:favoritos", "empresa:dashboard");
                                    favoritada = false;
                                    atualizarEstrela();
                                }

                                @Override
                                public void onFailure(Call<Void> c, Throwable t) {}
                            });
        } else {
            EmpresaData.api()
                    .favoritar(new EmpresaRequests.Favorito(cooperativaId))
                    .enqueue(
                            new Callback<EmpresaResponses.Favorito>() {
                                @Override
                                public void onResponse(
                                        Call<EmpresaResponses.Favorito> c,
                                        Response<EmpresaResponses.Favorito> r) {
                                    if (!vivo()) return;
                                    if (!r.isSuccessful()) {
                                        toast(EmpresaData.erro(r));
                                        return;
                                    }
                                    EmpresaData.invalidar("empresa:favoritos", "empresa:dashboard");
                                    favoritada = true;
                                    atualizarEstrela();
                                }

                                @Override
                                public void onFailure(
                                        Call<EmpresaResponses.Favorito> c, Throwable t) {}
                            });
        }
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
        LinearLayout mats = findViewById(R.id.listaMateriaisPP);
        mats.removeAllViews();
        List<GestorResponses.Estoque> disp = perfil.materiaisDisponiveis;
        findViewById(R.id.txtSemMateriaisPP)
                .setVisibility(disp == null || disp.isEmpty() ? View.VISIBLE : View.GONE);
        if (disp != null)
            for (GestorResponses.Estoque e : disp) {
                View v =
                        LayoutInflater.from(this)
                                .inflate(R.layout.item_empresa_estoque_linha, mats, false);
                ((TextView) v.findViewById(R.id.txtEstoqueLinhaNome))
                        .setText(e.materialCategoria == null ? "Material" : e.materialCategoria);
                ((TextView) v.findViewById(R.id.txtEstoqueLinhaKg))
                        .setText(GestorUi.kg(e.quantidadeKg));
                mats.addView(v);
            }

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
                        new androidx.appcompat.app.AlertDialog.Builder(
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
}
