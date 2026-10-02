package com.example.renovai.view;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.renovai.GestorBottomNav;
import com.example.renovai.GestorCache;
import com.example.renovai.GestorData;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.controller.TriagemController;
import com.example.renovai.dto.request.GestorRequests;
import com.example.renovai.dto.request.TriagemRequest;
import com.example.renovai.dto.response.ColetaResponse;
import com.example.renovai.dto.response.GestorResponses;
import com.example.renovai.dto.response.GestorResponses.FuncionarioDetalhe;
import com.example.renovai.dto.response.MaterialResponse;
import com.example.renovai.dto.response.TriagemResponse;

import okhttp3.ResponseBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tela 4.8.2 — Nova triagem: escolhe a coleta aguardando triagem e os funcionários participantes.
 *
 * <p>Como a API não tem um endpoint único de "iniciar triagem", o app monta o fluxo com os
 * endpoints existentes: 1) GET /status → id do status TRIAGEM/PENDENTE 2) GET
 * /materiais/disponiveis → materiais da cooperativa (uma linha de triagem por material) 3) POST
 * /equipes → equipe da triagem (gestor = quem está logado) 4) POST /equipes-cooperados → um por
 * funcionário escolhido 5) POST /triagens → uma linha por material, com peso marcador de 0,001 kg
 * (mínimo aceito pela API) O cooperado completa os pesos depois na tela 2.3 (PUT /triagens/{id} e
 * PATCH /triagens/{id}/concluir).
 */
public class GestorNovaTriagemActivity extends GestorBaseActivity {

    private List<ColetaResponse> coletas = new ArrayList<>();
    private List<TriagemResponse> triagens = new ArrayList<>();
    private List<FuncionarioDetalhe> funcs = new ArrayList<>();
    private String coletaSel;
    private final Set<String> funcSel = new LinkedHashSet<>();
    private boolean funcsIniciados = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_gestor_nova_triagem, GestorBottomNav.Aba.NENHUMA)) return;
        titulo("Nova Triagem");
        findViewById(R.id.btnIniciarTriagem).setOnClickListener(v -> iniciar());
        GestorData.coletas(
                false,
                (l, c) -> {
                    coletas = l;
                    if (vivo()) renderColetas();
                });
        GestorData.triagens(
                false,
                (l, c) -> {
                    triagens = l;
                    if (vivo()) renderColetas();
                });
        GestorData.funcionarios(
                false,
                (l, c) -> {
                    funcs = l;
                    if (vivo()) renderFuncs();
                });
    }

    private List<ColetaResponse> disponiveis() {
        Set<String> comTriagem = new HashSet<>();
        for (TriagemResponse t : triagens)
            if (t.getColetaId() != null) comTriagem.add(t.getColetaId().toLowerCase());
        List<ColetaResponse> l = new ArrayList<>();
        for (ColetaResponse c : coletas)
            if (c.getColetaId() != null && !comTriagem.contains(c.getColetaId().toLowerCase()))
                l.add(c);
        l.sort(
                (a, x) ->
                        String.valueOf(x.getDataColeta())
                                .compareTo(String.valueOf(a.getDataColeta())));
        return l;
    }

    private void renderColetas() {
        List<ColetaResponse> disp = disponiveis();
        boolean existe = false;
        for (ColetaResponse c : disp)
            if (c.getColetaId().equalsIgnoreCase(coletaSel)) existe = true;
        if (!existe) coletaSel = disp.isEmpty() ? null : disp.get(0).getColetaId();

        LinearLayout sel = findViewById(R.id.coletaSelecionada),
                outras = findViewById(R.id.listaOutrasColetas);
        sel.removeAllViews();
        outras.removeAllViews();
        for (ColetaResponse c : disp) {
            boolean ehSel = c.getColetaId().equalsIgnoreCase(coletaSel);
            View v =
                    LayoutInflater.from(this)
                            .inflate(R.layout.item_gestor_coleta_sel, ehSel ? sel : outras, false);
            ((TextView) v.findViewById(R.id.txtColetaSelNome))
                    .setText("Coleta " + GestorUi.idCurto(c.getColetaId()));
            ((TextView) v.findViewById(R.id.txtColetaSelSub))
                    .setText(
                            (c.getCooperadoNome() == null ? "—" : c.getCooperadoNome())
                                    + " – "
                                    + GestorUi.kg(
                                            c.getQuantidadeKg() == null
                                                    ? 0d
                                                    : c.getQuantidadeKg().doubleValue()));
            ((CheckBox) v.findViewById(R.id.chkColeta)).setChecked(ehSel);
            TextView badge = v.findViewById(R.id.txtColetaSelBadge);
            badge.setText(ehSel ? "Aguardando" : "Disponível");
            if (ehSel) {
                GestorUi.badge(badge, GestorUi.EM_NEGOCIACAO);
                badge.setText("Aguardando");
                badge.setCompoundDrawables(null, null, null, null);
            }
            GestorUi.borda(
                    v.findViewById(R.id.cardColetaSel),
                    ehSel ? Color.parseColor("#519059") : Color.parseColor("#DDDDDD"));
            ((CheckBox) v.findViewById(R.id.chkColeta)).setClickable(false);
            v.setOnClickListener(
                    x -> {
                        if (emAndamento) return;
                        coletaSel = c.getColetaId();
                        renderColetas();
                    });
            (ehSel ? sel : outras).addView(v);
        }
        findViewById(R.id.txtSemColetas).setVisibility(disp.isEmpty() ? View.VISIBLE : View.GONE);
        findViewById(R.id.txtOutrasColetas)
                .setVisibility(disp.size() > 1 ? View.VISIBLE : View.GONE);
        resumo();
    }

    private void renderFuncs() {
        LinearLayout raiz = findViewById(R.id.listaFuncionarios);
        raiz.removeAllViews();
        if (!funcsIniciados && !funcs.isEmpty()) {
            funcsIniciados = true;
            for (FuncionarioDetalhe f : funcs)
                if (!f.inativo() && !Boolean.TRUE.equals(f.pendente)) funcSel.add(f.funcionarioId);
        }
        for (FuncionarioDetalhe f : funcs) {
            if (Boolean.TRUE.equals(f.pendente)) continue;
            boolean inativo = f.inativo();
            boolean marcado = funcSel.contains(f.funcionarioId);
            View v = LayoutInflater.from(this).inflate(R.layout.item_gestor_func_sel, raiz, false);
            ((CheckBox) v.findViewById(R.id.chkFunc)).setChecked(marcado);
            ((TextView) v.findViewById(R.id.txtFuncSelAvatar)).setText(GestorUi.iniciais(f.nome));
            ((TextView) v.findViewById(R.id.txtFuncSelNome)).setText(f.nome == null ? "—" : f.nome);
            ((TextView) v.findViewById(R.id.txtFuncSelCargo))
                    .setText(f.cargo == null ? "Função" : f.cargo);
            TextView badge = v.findViewById(R.id.txtFuncSelBadge);
            badge.setText(inativo ? "Inativo" : "Ativo");
            GestorUi.badge(badge, inativo ? GestorUi.EM_NEGOCIACAO : GestorUi.CONCLUIDO);
            badge.setCompoundDrawables(null, null, null, null);
            GestorUi.borda(
                    v.findViewById(R.id.cardFuncSel),
                    marcado ? Color.parseColor("#519059") : Color.parseColor("#DDDDDD"));
            v.setAlpha(inativo ? 0.6f : 1f);
            v.setOnClickListener(
                    x -> {
                        if (emAndamento) return;
                        if (inativo) {
                            toast(f.nome + " está inativo e não pode participar da triagem.");
                            return;
                        }
                        if (!funcSel.remove(f.funcionarioId)) funcSel.add(f.funcionarioId);
                        renderFuncs();
                    });
            raiz.addView(v);
            ((CheckBox) v.findViewById(R.id.chkFunc)).setClickable(false);
        }
        resumo();
    }

    private void resumo() {
        ((TextView) findViewById(R.id.txtResFunc)).setText(String.valueOf(funcSel.size()));
        String nome = "—", peso = "0 kg";
        for (ColetaResponse c : coletas)
            if (c.getColetaId() != null && c.getColetaId().equalsIgnoreCase(coletaSel)) {
                nome = GestorUi.idCurto(c.getColetaId());
                peso =
                        GestorUi.kg(
                                c.getQuantidadeKg() == null
                                        ? 0d
                                        : c.getQuantidadeKg().doubleValue());
            }
        ((TextView) findViewById(R.id.txtResColeta)).setText(nome);
        ((TextView) findViewById(R.id.txtResPeso)).setText(peso);
    }

    private boolean emAndamento = false;

    private void iniciar() {
        if (emAndamento) return;
        if (coletaSel == null) {
            toast("Selecione uma coleta para triar.");
            return;
        }
        if (funcSel.isEmpty()) {
            toast("Selecione ao menos um funcionário.");
            return;
        }
        emAndamento = true;
        mostrarProgresso(true);
        final boolean[] seguiu = {false};
        GestorData.status(
                new GestorData.Ouvinte<List<GestorResponses.Status>>() {
                    @Override
                    public void aoReceber(List<GestorResponses.Status> l, boolean doCache) {
                        if (seguiu[0] || (doCache && l.isEmpty())) return;
                        seguiu[0] = true;
                        String statusId = statusInicial(l);
                        if (statusId == null) {
                            falhar("Não há status PENDENTE cadastrado para TRIAGEM no banco.");
                            return;
                        }
                        buscarMateriais(statusId);
                    }

                    @Override
                    public void aoErro(String m) {
                        if (!seguiu[0]) {
                            seguiu[0] = true;
                            falhar(m);
                        }
                    }
                });
    }

    private void mostrarProgresso(boolean on) {
        findViewById(R.id.progresso).setVisibility(on ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnIniciarTriagem).setEnabled(!on);
    }

    private void falhar(String msg) {
        emAndamento = false;
        if (vivo()) {
            mostrarProgresso(false);
            toast(msg);
        }
    }

    /**
     * Linha de triagem recém-criada = status TRIAGEM/PENDENTE (é o que faz ela aparecer para o
     * cooperado em /triagens/abertas).
     */
    private String statusInicial(List<GestorResponses.Status> l) {
        String pendente = null, andamento = null;
        for (GestorResponses.Status s : l) {
            if (s.referencia == null
                    || !s.referencia.equalsIgnoreCase("TRIAGEM")
                    || s.statusAtual == null) continue;
            String a = s.statusAtual.toUpperCase();
            if (a.equals("PENDENTE")) pendente = s.statusId;
            else if ((a.equals("EM_ANDAMENTO") || a.equals("EM ANDAMENTO")) && andamento == null)
                andamento = s.statusId;
        }
        return pendente != null ? pendente : andamento;
    }

    private void buscarMateriais(String statusId) {
        GestorData.api()
                .materiaisDisponiveis()
                .enqueue(
                        new Callback<List<MaterialResponse>>() {
                            @Override
                            public void onResponse(
                                    Call<List<MaterialResponse>> c,
                                    Response<List<MaterialResponse>> r) {
                                if (!r.isSuccessful() || r.body() == null) {
                                    falhar(erro(r, "Não foi possível carregar os materiais"));
                                    return;
                                }
                                List<String> ids = new ArrayList<>();
                                for (MaterialResponse m : r.body())
                                    if (m.getMaterialId() != null
                                            && m.getCooperativaId() != null
                                            && m.getCooperativaId().equalsIgnoreCase(coopId()))
                                        ids.add(m.getMaterialId());
                                if (ids.isEmpty()) // cooperativa sem materiais próprios: usa os
                                    // materiais gerais (sem cooperativa)
                                    for (MaterialResponse m : r.body())
                                        if (m.getMaterialId() != null
                                                && m.getCooperativaId() == null)
                                            ids.add(m.getMaterialId());
                                if (ids.isEmpty()) {
                                    falhar(
                                            "Nenhum material disponível cadastrado para criar as"
                                                + " linhas da triagem.");
                                    return;
                                }
                                criarEquipe(statusId, ids);
                            }

                            @Override
                            public void onFailure(Call<List<MaterialResponse>> c, Throwable t) {
                                falhar("Sem conexão com o servidor.");
                            }
                        });
    }

    private void criarEquipe(String statusId, List<String> materiaisIds) {
        String nome =
                "Triagem "
                        + GestorUi.idCurto(coletaSel)
                        + " - "
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
        GestorData.api()
                .criarEquipe(new GestorRequests.Equipe(funcId(), nome, true))
                .enqueue(
                        new Callback<GestorResponses.Equipe>() {
                            @Override
                            public void onResponse(
                                    Call<GestorResponses.Equipe> c,
                                    Response<GestorResponses.Equipe> r) {
                                if (!r.isSuccessful()
                                        || r.body() == null
                                        || r.body().equipeId == null) {
                                    falhar(erro(r, "Não foi possível criar a equipe da triagem"));
                                    return;
                                }
                                adicionarMembros(r.body(), statusId, materiaisIds);
                            }

                            @Override
                            public void onFailure(Call<GestorResponses.Equipe> c, Throwable t) {
                                falhar("Sem conexão com o servidor.");
                            }
                        });
    }

    private void adicionarMembros(
            GestorResponses.Equipe equipe, String statusId, List<String> materiaisIds) {
        final List<String> membros = new ArrayList<>(funcSel);
        final AtomicInteger pend = new AtomicInteger(membros.size()), falhas = new AtomicInteger(0);
        for (String cooperadoId : membros) {
            GestorData.api()
                    .adicionarMembroEquipe(
                            new GestorRequests.EquipeCooperado(equipe.equipeId, cooperadoId))
                    .enqueue(
                            new Callback<ResponseBody>() {
                                @Override
                                public void onResponse(
                                        Call<ResponseBody> c, Response<ResponseBody> r) {
                                    if (!r.isSuccessful()) falhas.incrementAndGet();
                                    fim();
                                }

                                @Override
                                public void onFailure(Call<ResponseBody> c, Throwable t) {
                                    falhas.incrementAndGet();
                                    fim();
                                }

                                private void fim() {
                                    if (pend.decrementAndGet() > 0) return;
                                    if (falhas.get() > 0) {
                                        desativarEquipe(equipe);
                                        falhar(
                                                falhas.get()
                                                        + " de "
                                                        + membros.size()
                                                        + " funcionários não puderam ser vinculados"
                                                        + " à equipe. Tente novamente.");
                                    } else {
                                        criarLinhas(equipe, statusId, materiaisIds);
                                    }
                                }
                            });
        }
    }

    private void criarLinhas(
            GestorResponses.Equipe equipe, String statusId, List<String> materiaisIds) {
        String imagem = null;
        for (ColetaResponse c : coletas)
            if (c.getColetaId() != null && c.getColetaId().equalsIgnoreCase(coletaSel))
                imagem = c.getImagemUrl();
        final AtomicInteger pend = new AtomicInteger(materiaisIds.size()),
                falhas = new AtomicInteger(0);
        final int total = materiaisIds.size();
        for (String materialId : materiaisIds) {
            TriagemRequest body =
                    new TriagemRequest(
                            equipe.equipeId,
                            coletaSel,
                            materialId,
                            statusId,
                            TriagemController.PESO_INICIAL,
                            TriagemController.PESO_INICIAL,
                            imagem);
            GestorData.api()
                    .criarTriagem(body)
                    .enqueue(
                            new Callback<TriagemResponse>() {
                                @Override
                                public void onResponse(
                                        Call<TriagemResponse> c, Response<TriagemResponse> r) {
                                    if (!r.isSuccessful()) falhas.incrementAndGet();
                                    fim();
                                }

                                @Override
                                public void onFailure(Call<TriagemResponse> c, Throwable t) {
                                    falhas.incrementAndGet();
                                    fim();
                                }

                                private void fim() {
                                    if (pend.decrementAndGet() > 0) return;
                                    GestorData.invalidar(GestorCache.TRIAGENS, GestorCache.ESTOQUE);
                                    emAndamento = false;
                                    if (!vivo()) return;
                                    mostrarProgresso(false);
                                    int ok = total - falhas.get();
                                    if (ok == 0) {
                                        desativarEquipe(equipe);
                                        toast(
                                                "Não foi possível criar as linhas da triagem. Tente"
                                                    + " novamente.");
                                        return;
                                    }
                                    toast(
                                            falhas.get() == 0
                                                    ? "Triagem iniciada com " + ok + " materiais."
                                                    : "Triagem iniciada, mas só "
                                                            + ok
                                                            + " de "
                                                            + total
                                                            + " materiais foram criados.");
                                    finish();
                                }
                            });
        }
    }

    /**
     * Equipe criada mas sem uso (falha no meio do caminho): só marca como inativa, sem apagar nada.
     */
    private void desativarEquipe(GestorResponses.Equipe equipe) {
        GestorData.api()
                .atualizarEquipe(
                        equipe.equipeId, new GestorRequests.Equipe(funcId(), equipe.nome, false))
                .enqueue(
                        new Callback<GestorResponses.Equipe>() {
                            @Override
                            public void onResponse(
                                    Call<GestorResponses.Equipe> c,
                                    Response<GestorResponses.Equipe> r) {}

                            @Override
                            public void onFailure(Call<GestorResponses.Equipe> c, Throwable t) {}
                        });
    }

    private static String erro(Response<?> r, String padrao) {
        try {
            if (r.errorBody() != null) {
                com.google.gson.JsonObject o =
                        com.google.gson.JsonParser.parseString(r.errorBody().string())
                                .getAsJsonObject();
                if (o.has("mensagem")) return o.get("mensagem").getAsString();
            }
        } catch (Exception ignored) {
        }
        return padrao + " (erro " + r.code() + ").";
    }
}
