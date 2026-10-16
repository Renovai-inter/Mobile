package com.example.renovai.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.renovai.EmpresaBottomNav;
import com.example.renovai.EmpresaData;
import com.example.renovai.EmpresaPedidos;
import com.example.renovai.EmpresaStatus;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.dto.request.GestorRequests;
import com.example.renovai.dto.response.EmpresaResponses;
import com.example.renovai.dto.response.GestorResponses;
import com.example.renovai.dto.response.ItemResponse;
import com.example.renovai.dto.response.PedidoCooperativaResponse;
import com.example.renovai.dto.response.PedidoResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** A Empresa aceita a contraproposta; a conclusão permanece sob responsabilidade do gestor. */
public class EmpresaPedidoDetalheActivity extends EmpresaBaseActivity {
    public static final String EXTRA_PEDIDO_ID = "pedidoId", EXTRA_NEG_ID = "negociacaoId";
    private String pedidoId, cooperativaId, cooperativaNome, statusVinculo;
    private boolean alterando, carregando, negociacaoAtualizada;
    private int geracao;
    private PedidoResponse pedido;
    private GestorResponses.Negociacao neg;
    private List<ItemResponse> itens = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_empresa_pedido_detalhe, EmpresaBottomNav.Aba.NENHUMA))
            return;
        titulo("Detalhes do pedido");
        pedidoId = getIntent().getStringExtra(EXTRA_PEDIDO_ID);
        if (pedidoId == null) {
            toast("Pedido não informado.");
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (vivo() && pedidoId != null && !alterando) carregar();
    }

    private void carregar() {
        int versao = ++geracao;
        carregando = true;
        negociacaoAtualizada = false;
        findViewById(R.id.progressoE).setVisibility(View.VISIBLE);
        render();
        AtomicInteger pendentes = new AtomicInteger(4);
        Runnable fim =
                () -> {
                    if (pendentes.decrementAndGet() == 0 && vivo() && versao == geracao) {
                        carregando = false;
                        findViewById(R.id.progressoE).setVisibility(View.GONE);
                        render();
                    }
                };
        EmpresaData.api()
                .pedido(pedidoId)
                .enqueue(
                        new Callback<PedidoResponse>() {
                            @Override
                            public void onResponse(
                                    Call<PedidoResponse> c, Response<PedidoResponse> r) {
                                if (!vivo() || versao != geracao) return;
                                if (r.isSuccessful() && r.body() != null) pedido = r.body();
                                else toast(EmpresaData.erro(r));
                                fim.run();
                            }

                            @Override
                            public void onFailure(Call<PedidoResponse> c, Throwable t) {
                                if (vivo() && versao == geracao) {
                                    toast("Não foi possível carregar o pedido.");
                                    fim.run();
                                }
                            }
                        });
        EmpresaData.api()
                .itensPedido(pedidoId)
                .enqueue(
                        new Callback<List<ItemResponse>>() {
                            @Override
                            public void onResponse(
                                    Call<List<ItemResponse>> c, Response<List<ItemResponse>> r) {
                                if (!vivo() || versao != geracao) return;
                                if (r.isSuccessful() && r.body() != null) itens = r.body();
                                else toast(EmpresaData.erro(r));
                                fim.run();
                            }

                            @Override
                            public void onFailure(Call<List<ItemResponse>> c, Throwable t) {
                                if (vivo() && versao == geracao) {
                                    toast("Não foi possível carregar os materiais.");
                                    fim.run();
                                }
                            }
                        });
        EmpresaData.api()
                .cooperativasPedido(pedidoId)
                .enqueue(
                        new Callback<List<PedidoCooperativaResponse>>() {
                            @Override
                            public void onResponse(
                                    Call<List<PedidoCooperativaResponse>> c,
                                    Response<List<PedidoCooperativaResponse>> r) {
                                if (!vivo() || versao != geracao) return;
                                if (r.isSuccessful() && r.body() != null && !r.body().isEmpty()) {
                                    PedidoCooperativaResponse coop = r.body().get(0);
                                    cooperativaId = coop.getCooperativaId();
                                    cooperativaNome = coop.getCooperativaNome();
                                    statusVinculo = coop.getStatusAtual();
                                    contato(versao);
                                } else toast(EmpresaData.erro(r));
                                fim.run();
                            }

                            @Override
                            public void onFailure(
                                    Call<List<PedidoCooperativaResponse>> c, Throwable t) {
                                if (vivo() && versao == geracao) {
                                    toast("Não foi possível carregar a cooperativa.");
                                    fim.run();
                                }
                            }
                        });
        EmpresaData.api()
                .negociacoesPorEmpresa()
                .enqueue(
                        new Callback<List<GestorResponses.Negociacao>>() {
                            @Override
                            public void onResponse(
                                    Call<List<GestorResponses.Negociacao>> c,
                                    Response<List<GestorResponses.Negociacao>> r) {
                                if (!vivo() || versao != geracao) return;
                                if (r.isSuccessful() && r.body() != null) {
                                    neg =
                                            EmpresaPedidos.negociacaoDoPedido(
                                                    r.body(), pedidoId, null);
                                    negociacaoAtualizada = true;
                                } else toast(EmpresaData.erro(r));
                                fim.run();
                            }

                            @Override
                            public void onFailure(
                                    Call<List<GestorResponses.Negociacao>> c, Throwable t) {
                                if (vivo() && versao == geracao) {
                                    toast("Não foi possível atualizar a negociação.");
                                    fim.run();
                                }
                            }
                        });
    }

    private void contato(int versao) {
        EmpresaData.api()
                .perfilPublico(cooperativaId)
                .enqueue(
                        new Callback<EmpresaResponses.CooperativaPerfilPublico>() {
                            @Override
                            public void onResponse(
                                    Call<EmpresaResponses.CooperativaPerfilPublico> c,
                                    Response<EmpresaResponses.CooperativaPerfilPublico> r) {
                                if (!vivo()
                                        || versao != geracao
                                        || !r.isSuccessful()
                                        || r.body() == null) return;
                                EmpresaResponses.CooperativaPerfilPublico p = r.body();
                                ((TextView) findViewById(R.id.txtPedidoEnderecoE))
                                        .setText(p.endereco == null ? "Não informado" : p.endereco);
                                ((TextView) findViewById(R.id.txtPedidoComplementoE))
                                        .setText(
                                                p.contato() == null
                                                        ? "Não informado"
                                                        : p.contato());
                                findViewById(R.id.txtPedidoComplementoE)
                                        .setOnClickListener(
                                                v -> {
                                                    if (p.contato() == null
                                                            || p.contato().isBlank()) return;
                                                    boolean telefone =
                                                            p.telefone != null
                                                                    && !p.telefone.isBlank();
                                                    try {
                                                        startActivity(
                                                                new Intent(
                                                                        telefone
                                                                                ? Intent.ACTION_DIAL
                                                                                : Intent
                                                                                        .ACTION_SENDTO,
                                                                        Uri.parse(
                                                                                (telefone
                                                                                                ? "tel:"
                                                                                                : "mailto:")
                                                                                        + p
                                                                                                .contato())));
                                                    } catch (
                                                            android.content
                                                                            .ActivityNotFoundException
                                                                    e) {
                                                        toast(
                                                                "Nenhum aplicativo de contato"
                                                                    + " disponível.");
                                                    }
                                                });
                            }

                            @Override
                            public void onFailure(
                                    Call<EmpresaResponses.CooperativaPerfilPublico> c,
                                    Throwable t) {
                                if (vivo() && versao == geracao)
                                    toast("Não foi possível carregar o contato.");
                            }
                        });
    }

    private int estado() {
        return EmpresaStatus.estado(
                neg,
                statusVinculo == null ? pedido.getStatusAtual() : statusVinculo,
                pedido.getDataConclusao());
    }

    private void render() {
        findViewById(R.id.boxAcoesPedidoE).setVisibility(View.GONE);
        findViewById(R.id.btnConversarE).setVisibility(View.GONE);
        if (pedido == null) return;
        int estado = estado();
        ((TextView) findViewById(R.id.txtPedidoTituloE))
                .setText("Pedido " + GestorUi.idCurto(pedidoId));
        ((TextView) findViewById(R.id.txtPedidoIniciadoE))
                .setText("Iniciado em " + GestorUi.data(pedido.getDataPedido()));
        TextView badge = findViewById(R.id.txtPedidoStatusE);
        badge.setText(EmpresaStatus.rotulo(estado));
        GestorUi.badge(badge, EmpresaStatus.cor(estado));
        badge.setCompoundDrawables(null, null, null, null);
        ((TextView) findViewById(R.id.txtPedidoCoopE))
                .setText(neg != null ? neg.cooperativaNome : cooperativaNome);
        LinearLayout lista = findViewById(R.id.listaItensPedidoE);
        lista.removeAllViews();
        if (neg != null && neg.itens != null && !neg.itens.isEmpty()) {
            for (GestorResponses.NegItem it : neg.itens)
                material(lista, it.materialCategoria, it.quantidadeKg);
        } else
            for (ItemResponse it : itens)
                material(lista, it.getMaterialCategoria(), it.getQuantidadeKg());
        double valor =
                neg != null && neg.valorTotal != null
                        ? neg.valorTotal
                        : pedido.getValorTotal() == null ? 0 : pedido.getValorTotal();
        ((TextView) findViewById(R.id.txtPedidoTotalE)).setText(GestorUi.dinheiro(valor));
        View proposta = findViewById(R.id.boxContrapropostaE);
        proposta.setVisibility(neg == null ? View.GONE : View.VISIBLE);
        ((TextView) findViewById(R.id.txtValorContrapropostaE))
                .setText(
                        (EmpresaStatus.aprovado(estado)
                                        ? "Valor aceito: "
                                        : "Valor da negociação: ")
                                + GestorUi.dinheiro(valor));
        TextView observacao = findViewById(R.id.txtObservacaoContrapropostaE);
        observacao.setText(neg == null ? "" : neg.observacao);
        observacao.setVisibility(
                neg != null && neg.observacao != null && !neg.observacao.isBlank()
                        ? View.VISIBLE
                        : View.GONE);
        TextView confirmacao = findViewById(R.id.txtConfirmacaoAceiteE);
        confirmacao.setVisibility(EmpresaStatus.aprovado(estado) ? View.VISIBLE : View.GONE);
        confirmacao.setText(
                estado == EmpresaStatus.FINALIZADO
                        ? "Pedido finalizado pela cooperativa."
                        : "Valor aceito. Aguarde a cooperativa concluir o pedido. Combine entrega e"
                              + " pagamento pelo contato abaixo.");
        if (neg != null) {
            findViewById(R.id.btnConversarE).setVisibility(View.VISIBLE);
            findViewById(R.id.btnConversarE)
                    .setOnClickListener(
                            v -> {
                                Intent i = new Intent(this, EmpresaChatActivity.class);
                                i.putExtra(EmpresaChatActivity.EXTRA_NEG_ID, neg.negociacaoId);
                                i.putExtra(
                                        EmpresaChatActivity.EXTRA_COOPERATIVA_ID,
                                        neg.cooperativaId);
                                startActivity(i);
                            });
        }
        boolean podeResponder =
                neg != null
                        && negociacaoAtualizada
                        && !carregando
                        && !alterando
                        && (estado == EmpresaStatus.EM_NEGOCIACAO
                                || estado == EmpresaStatus.ABERTO);
        findViewById(R.id.boxAcoesPedidoE).setVisibility(podeResponder ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnAceitarValorE)
                .setOnClickListener(
                        v ->
                                GestorUi.confirmar(
                                        this,
                                        "Aceitar valor",
                                        "Aceitar "
                                                + GestorUi.dinheiro(valor)
                                                + "? A cooperativa concluirá o pedido depois.",
                                        "Aceitar",
                                        () -> responder(true)));
        findViewById(R.id.btnRecusarE)
                .setOnClickListener(
                        v ->
                                GestorUi.confirmar(
                                        this,
                                        "Recusar negociação",
                                        "Recusar esta negociação?",
                                        "Recusar",
                                        () -> responder(false)));
    }

    private void material(LinearLayout lista, String nome, Double peso) {
        View v =
                LayoutInflater.from(this)
                        .inflate(R.layout.item_gestor_material_linha, lista, false);
        ((TextView) v.findViewById(R.id.txtMaterialNome)).setText(nome == null ? "Material" : nome);
        ((TextView) v.findViewById(R.id.txtMaterialKg)).setText(GestorUi.kg(peso));
        lista.addView(v);
    }

    private void responder(boolean aceitar) {
        if (neg == null
                || alterando
                || carregando
                || !negociacaoAtualizada
                || EmpresaStatus.aprovado(estado())
                || estado() == EmpresaStatus.RECUSADO) return;
        alterando = true;
        render();
        findViewById(R.id.progressoE).setVisibility(View.VISIBLE);
        Call<GestorResponses.Negociacao> chamada =
                aceitar
                        ? EmpresaData.api().aceitarNegociacao(neg.negociacaoId)
                        : EmpresaData.api()
                                .recusarNegociacao(
                                        neg.negociacaoId,
                                        new GestorRequests.Recusar("Recusado pela empresa"));
        chamada.enqueue(
                new Callback<GestorResponses.Negociacao>() {
                    @Override
                    public void onResponse(
                            Call<GestorResponses.Negociacao> c,
                            Response<GestorResponses.Negociacao> r) {
                        alterando = false;
                        EmpresaData.invalidar(
                                "empresa:negociacoes", "empresa:pedidos", "empresa:dashboard");
                        if (!vivo()) return;
                        if (r.isSuccessful() && r.body() != null) {
                            neg = r.body();
                            toast(
                                    aceitar
                                            ? "Valor aceito. A conclusão será feita pela"
                                                  + " cooperativa."
                                            : "Negociação recusada.");
                        } else toast(EmpresaData.erro(r));
                        carregar();
                    }

                    @Override
                    public void onFailure(Call<GestorResponses.Negociacao> c, Throwable t) {
                        alterando = false;
                        EmpresaData.invalidar(
                                "empresa:negociacoes", "empresa:pedidos", "empresa:dashboard");
                        if (!vivo()) return;
                        toast(
                                "Não foi possível confirmar a operação. Atualizando o estado da"
                                    + " negociação.");
                        carregar();
                    }
                });
    }
}
