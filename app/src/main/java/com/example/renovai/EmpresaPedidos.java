package com.example.renovai;

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

/** Consulta apenas os pedidos e negociações da conta autenticada. */
public final class EmpresaPedidos {
    public static class Vista {
        public String pedidoId, negociacaoId, cooperativaId, cooperativaNome, data, materiais = "";
        public double valor, peso;
        public int estado;
    }

    private EmpresaPedidos() {}

    public static String rotulo(int estado) {
        return EmpresaStatus.rotulo(estado);
    }

    public static GestorResponses.Negociacao negociacaoDoPedido(
            List<GestorResponses.Negociacao> lista, String pedidoId, String cooperativaId) {
        GestorResponses.Negociacao escolhida = null;
        for (GestorResponses.Negociacao n : lista) {
            if (!pedidoId.equalsIgnoreCase(n.pedidoId)
                    || (cooperativaId != null && !cooperativaId.equalsIgnoreCase(n.cooperativaId)))
                continue;
            if (escolhida == null
                    || (n.dataInicio == null ? "" : n.dataInicio)
                                    .compareTo(
                                            escolhida.dataInicio == null
                                                    ? ""
                                                    : escolhida.dataInicio)
                            > 0) escolhida = n;
        }
        return escolhida;
    }

    public static void listar(GestorData.Ouvinte<List<Vista>> o) {
        EmpresaData.pedidos(
                true,
                new GestorData.Ouvinte<List<PedidoResponse>>() {
                    @Override
                    public void aoReceber(List<PedidoResponse> pedidos, boolean cache) {
                        if (cache) return;
                        if (pedidos.isEmpty()) {
                            o.aoReceber(new ArrayList<>(), false);
                            return;
                        }
                        final String token = SessionManager.getToken();
                        List<Vista> lista = new ArrayList<>();
                        List<GestorResponses.Negociacao> negociacoes = new ArrayList<>();
                        List<String> vinculos = new ArrayList<>();
                        AtomicInteger pendentes = new AtomicInteger(pedidos.size() * 2 + 1);
                        final boolean[] incompleto = {false};
                        Runnable fim =
                                () -> {
                                    if (pendentes.decrementAndGet() != 0
                                            || !java.util.Objects.equals(
                                                    token, SessionManager.getToken())) return;
                                    for (int i = 0; i < lista.size(); i++) {
                                        Vista v = lista.get(i);
                                        GestorResponses.Negociacao n =
                                                negociacaoDoPedido(
                                                        negociacoes, v.pedidoId, v.cooperativaId);
                                        if (n != null) {
                                            v.negociacaoId = n.negociacaoId;
                                            if (v.cooperativaId == null) {
                                                v.cooperativaId = n.cooperativaId;
                                                v.cooperativaNome = n.cooperativaNome;
                                            }
                                            if (n.valorTotal != null) v.valor = n.valorTotal;
                                            if (n.itens != null && !n.itens.isEmpty()) {
                                                v.peso = 0;
                                                v.materiais = "";
                                                for (GestorResponses.NegItem it : n.itens) {
                                                    v.peso +=
                                                            it.quantidadeKg == null
                                                                    ? 0
                                                                    : it.quantidadeKg;
                                                    if (!v.materiais.isEmpty()) v.materiais += ", ";
                                                    v.materiais +=
                                                            it.materialCategoria == null
                                                                    ? "Material"
                                                                    : it.materialCategoria;
                                                }
                                            }
                                        }
                                        v.estado =
                                                EmpresaStatus.estado(
                                                        n,
                                                        vinculos.get(i),
                                                        pedidos.get(i).getDataConclusao());
                                    }
                                    o.aoReceber(lista, false);
                                    if (incompleto[0])
                                        o.aoErro(
                                                "Alguns detalhes dos pedidos não puderam ser"
                                                        + " atualizados. Tente novamente.");
                                };
                        EmpresaData.api()
                                .negociacoesPorEmpresa()
                                .enqueue(
                                        new Callback<List<GestorResponses.Negociacao>>() {
                                            @Override
                                            public void onResponse(
                                                    Call<List<GestorResponses.Negociacao>> c,
                                                    Response<List<GestorResponses.Negociacao>> r) {
                                                if (r.isSuccessful() && r.body() != null)
                                                    negociacoes.addAll(r.body());
                                                else incompleto[0] = true;
                                                fim.run();
                                            }

                                            @Override
                                            public void onFailure(
                                                    Call<List<GestorResponses.Negociacao>> c,
                                                    Throwable t) {
                                                incompleto[0] = true;
                                                fim.run();
                                            }
                                        });
                        for (int i = 0; i < pedidos.size(); i++) {
                            PedidoResponse p = pedidos.get(i);
                            final int indice = i;
                            Vista v = new Vista();
                            v.pedidoId = p.getPedidoId();
                            v.data = p.getDataPedido();
                            v.valor = p.getValorTotal() == null ? 0 : p.getValorTotal();
                            lista.add(v);
                            vinculos.add(p.getStatusAtual());
                            EmpresaData.api()
                                    .cooperativasPedido(v.pedidoId)
                                    .enqueue(
                                            new Callback<List<PedidoCooperativaResponse>>() {
                                                @Override
                                                public void onResponse(
                                                        Call<List<PedidoCooperativaResponse>> c,
                                                        Response<List<PedidoCooperativaResponse>>
                                                                r) {
                                                    if (r.isSuccessful()
                                                            && r.body() != null
                                                            && !r.body().isEmpty()) {
                                                        PedidoCooperativaResponse coop =
                                                                r.body().get(0);
                                                        v.cooperativaId = coop.getCooperativaId();
                                                        v.cooperativaNome =
                                                                coop.getCooperativaNome();
                                                        vinculos.set(indice, coop.getStatusAtual());
                                                    } else incompleto[0] = true;
                                                    fim.run();
                                                }

                                                @Override
                                                public void onFailure(
                                                        Call<List<PedidoCooperativaResponse>> c,
                                                        Throwable t) {
                                                    incompleto[0] = true;
                                                    fim.run();
                                                }
                                            });
                            EmpresaData.api()
                                    .itensPedido(v.pedidoId)
                                    .enqueue(
                                            new Callback<List<ItemResponse>>() {
                                                @Override
                                                public void onResponse(
                                                        Call<List<ItemResponse>> c,
                                                        Response<List<ItemResponse>> r) {
                                                    if (r.isSuccessful() && r.body() != null)
                                                        for (ItemResponse it : r.body()) {
                                                            v.peso +=
                                                                    it.getQuantidadeKg() == null
                                                                            ? 0
                                                                            : it.getQuantidadeKg();
                                                            if (!v.materiais.isEmpty())
                                                                v.materiais += ", ";
                                                            v.materiais +=
                                                                    it.getMaterialCategoria()
                                                                                    == null
                                                                            ? "Material"
                                                                            : it
                                                                                    .getMaterialCategoria();
                                                        }
                                                    else incompleto[0] = true;
                                                    fim.run();
                                                }

                                                @Override
                                                public void onFailure(
                                                        Call<List<ItemResponse>> c, Throwable t) {
                                                    incompleto[0] = true;
                                                    fim.run();
                                                }
                                            });
                        }
                    }

                    @Override
                    public void aoErro(String m) {
                        o.aoErro(m);
                    }
                });
    }

    public static void detalhe(String id, GestorData.Ouvinte<Vista> o) {
        listar(
                new GestorData.Ouvinte<List<Vista>>() {
                    @Override
                    public void aoReceber(List<Vista> lista, boolean cache) {
                        for (Vista v : lista)
                            if (id.equalsIgnoreCase(v.pedidoId)) {
                                o.aoReceber(v, cache);
                                return;
                            }
                        o.aoErro("Pedido não encontrado.");
                    }

                    @Override
                    public void aoErro(String m) {
                        o.aoErro(m);
                    }
                });
    }
}
