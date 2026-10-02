package com.example.renovai;

import com.example.renovai.dto.request.GestorRequests;
import com.example.renovai.dto.response.GestorResponses.Item;
import com.example.renovai.dto.response.GestorResponses.Negociacao;
import com.example.renovai.dto.response.GestorResponses.Pedido;
import com.example.renovai.dto.response.GestorResponses.PedidoCoop;
import com.example.renovai.dto.response.GestorResponses.Status;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Regras de pedidos do Gestor: lista de pendentes, histórico (negociações), aceitar e recusar. */
public final class GestorPedidos {

    /** Visão única de um pedido para as telas (pendente, em negociação, concluído ou recusado). */
    public static class Vista {
        public String pedidoCoopId,
                pedidoId,
                empresaId,
                empresaNome,
                data,
                negociacaoId,
                statusCoop;
        public double peso, valor;
        public int estado = GestorUi.OUTRO;
        public boolean pendente, aceito;
        public List<Item> itens = new ArrayList<>();
    }

    public interface Retorno {
        void ok();

        void erro(String mensagem);
    }

    private GestorPedidos() {}

    public static boolean ehPendente(String status) {
        return !GestorUi.tem(
                status, "ACEIT", "RECUS", "CONCLU", "NEGOCIA", "CANCEL", "FECHAD", "FINALIZ");
    }

    /** Detalhes (pedido + itens) com cache por pedido. */
    public static void detalhe(String pedidoId, GestorData.Ouvinte<Vista> o) {
        final Pedido cachePed = GestorCache.obter("pedido:" + pedidoId);
        final List<Item> cacheIt = GestorCache.obter("itens:" + pedidoId);
        if (cachePed != null && cacheIt != null) {
            o.aoReceber(montar(cachePed, cacheIt), true);
            return;
        }
        final Pedido[] ped = {null};
        final List<Item>[] its = new List[] {null};
        final AtomicInteger pend = new AtomicInteger(2);
        final Runnable fim =
                () -> {
                    if (pend.decrementAndGet() > 0) return;
                    if (ped[0] == null) {
                        o.aoErro("Não foi possível carregar o pedido.");
                        return;
                    }
                    if (its[0] == null) its[0] = new ArrayList<>();
                    GestorCache.guardar("pedido:" + pedidoId, ped[0]);
                    GestorCache.guardar("itens:" + pedidoId, its[0]);
                    o.aoReceber(montar(ped[0], its[0]), false);
                };
        GestorData.api()
                .pedido(pedidoId)
                .enqueue(
                        new Callback<Pedido>() {
                            @Override
                            public void onResponse(Call<Pedido> c, Response<Pedido> r) {
                                if (r.isSuccessful()) ped[0] = r.body();
                                fim.run();
                            }

                            @Override
                            public void onFailure(Call<Pedido> c, Throwable t) {
                                fim.run();
                            }
                        });
        GestorData.api()
                .itensPedido(pedidoId)
                .enqueue(
                        new Callback<List<Item>>() {
                            @Override
                            public void onResponse(Call<List<Item>> c, Response<List<Item>> r) {
                                if (r.isSuccessful()) its[0] = r.body();
                                fim.run();
                            }

                            @Override
                            public void onFailure(Call<List<Item>> c, Throwable t) {
                                fim.run();
                            }
                        });
    }

    private static Vista montar(Pedido p, List<Item> itens) {
        Vista v = new Vista();
        v.pedidoId = p.pedidoId;
        v.empresaId = p.empresaId;
        v.empresaNome = p.empresaNome;
        v.data = p.dataPedido;
        v.itens = itens;
        for (Item i : itens) {
            v.peso += i.quantidadeKg == null ? 0 : i.quantidadeKg;
            v.valor +=
                    (i.quantidadeKg == null ? 0 : i.quantidadeKg)
                            * (i.precoUnitario == null ? 0 : i.precoUnitario);
        }
        if (v.valor == 0 && p.valorTotal != null) v.valor = p.valorTotal;
        return v;
    }

    /** Pedidos aguardando resposta da cooperativa (Recusar / Aceitar). */
    public static void pendentes(GestorData.Ouvinte<List<Vista>> o) {
        GestorData.pedidosCoop(
                false,
                new GestorData.Ouvinte<List<PedidoCoop>>() {
                    @Override
                    public void aoReceber(List<PedidoCoop> lista, boolean doCache) {
                        final List<PedidoCoop> pend = new ArrayList<>();
                        for (PedidoCoop pc : lista) if (ehPendente(pc.statusAtual)) pend.add(pc);
                        if (pend.isEmpty()) {
                            o.aoReceber(new ArrayList<>(), doCache);
                            return;
                        }
                        final List<Vista> saida = new ArrayList<>();
                        final AtomicInteger falta = new AtomicInteger(pend.size());
                        for (PedidoCoop pc : pend) {
                            detalhe(
                                    pc.pedidoId,
                                    new GestorData.Ouvinte<Vista>() {
                                        @Override
                                        public void aoReceber(Vista v, boolean c) {
                                            v.pedidoCoopId = pc.pedidoCooperativaId;
                                            v.statusCoop = pc.statusAtual;
                                            v.pendente = true;
                                            synchronized (saida) {
                                                saida.add(v);
                                            }
                                            if (falta.decrementAndGet() == 0)
                                                o.aoReceber(ordenar(saida), doCache);
                                        }

                                        @Override
                                        public void aoErro(String m) {
                                            if (falta.decrementAndGet() == 0)
                                                o.aoReceber(ordenar(saida), doCache);
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

    private static List<Vista> ordenar(List<Vista> l) {
        List<Vista> r = new ArrayList<>(l);
        r.sort((a, b) -> String.valueOf(b.data).compareTo(String.valueOf(a.data)));
        return r;
    }

    /** Histórico: negociações da cooperativa + pedidos recusados antes de virar negociação. */
    public static void historico(GestorData.Ouvinte<List<Vista>> o) {
        GestorData.negociacoes(
                false,
                new GestorData.Ouvinte<List<Negociacao>>() {
                    @Override
                    public void aoReceber(List<Negociacao> negs, boolean doCache) {
                        final List<Vista> saida = new ArrayList<>();
                        final Set<String> comNeg = new HashSet<>();
                        for (Negociacao n : negs) {
                            comNeg.add(n.pedidoId);
                            Vista v = new Vista();
                            v.negociacaoId = n.negociacaoId;
                            v.pedidoId = n.pedidoId;
                            v.empresaId = n.empresaId;
                            v.empresaNome = n.empresaNome;
                            v.data = n.dataInicio;
                            v.peso = GestorUi.pesoNegociacao(n);
                            v.valor = n.valorTotal == null ? 0 : n.valorTotal;
                            v.estado = GestorUi.estadoNeg(n);
                            v.aceito = EmpresaStatus.estado(n, null, null) == EmpresaStatus.ACEITO;
                            if (v.estado == GestorUi.OUTRO) v.estado = GestorUi.EM_NEGOCIACAO;
                            saida.add(v);
                        }
                        GestorData.pedidosCoop(
                                false,
                                new GestorData.Ouvinte<List<PedidoCoop>>() {
                                    @Override
                                    public void aoReceber(List<PedidoCoop> pcs, boolean c2) {
                                        final List<PedidoCoop> rec = new ArrayList<>();
                                        for (PedidoCoop pc : pcs)
                                            if (!comNeg.contains(pc.pedidoId)
                                                    && GestorUi.tem(
                                                            pc.statusAtual, "RECUS", "CANCEL"))
                                                rec.add(pc);
                                        if (rec.isEmpty()) {
                                            o.aoReceber(ordenar(saida), doCache && c2);
                                            return;
                                        }
                                        final List<Vista> extra = new ArrayList<>();
                                        final AtomicInteger falta = new AtomicInteger(rec.size());
                                        for (PedidoCoop pc : rec) {
                                            detalhe(
                                                    pc.pedidoId,
                                                    new GestorData.Ouvinte<Vista>() {
                                                        @Override
                                                        public void aoReceber(Vista v, boolean c3) {
                                                            v.pedidoCoopId = pc.pedidoCooperativaId;
                                                            v.estado = GestorUi.RECUSADO;
                                                            synchronized (extra) {
                                                                extra.add(v);
                                                            }
                                                            fim();
                                                        }

                                                        @Override
                                                        public void aoErro(String m) {
                                                            fim();
                                                        }

                                                        private void fim() {
                                                            if (falta.decrementAndGet() == 0) {
                                                                List<Vista> todos =
                                                                        new ArrayList<>(saida);
                                                                todos.addAll(extra);
                                                                o.aoReceber(
                                                                        ordenar(todos),
                                                                        doCache && c2);
                                                            }
                                                        }
                                                    });
                                        }
                                    }

                                    @Override
                                    public void aoErro(String m) {
                                        o.aoReceber(ordenar(saida), doCache);
                                    }
                                });
                    }

                    @Override
                    public void aoErro(String m) {
                        o.aoErro(m);
                    }
                });
    }

    private static Status achar(List<Status> todos, String[] refs, String[] status) {
        if (todos == null) return null;
        for (Status s : todos) {
            boolean refOk = refs == null || refs.length == 0 || GestorUi.tem(s.referencia, refs);
            if (refOk && GestorUi.tem(s.statusAtual, status)) return s;
        }
        return null;
    }

    /** Inicia a negociação; o aceite do acordo permanece com a Empresa. */
    public static void aceitar(Vista v, Retorno r) {
        GestorData.status(
                new GestorData.Ouvinte<List<Status>>() {
                    private boolean tratado;

                    @Override
                    public void aoReceber(List<Status> todos, boolean cache) {
                        if (tratado || (cache && !GestorCache.valido(GestorCache.STATUS))) return;
                        tratado = true;
                        garantirNegociacao(v, todos, r);
                    }

                    @Override
                    public void aoErro(String m) {
                        if (!tratado) {
                            tratado = true;
                            r.erro(m);
                        }
                    }
                });
    }

    private static void garantirNegociacao(Vista v, List<Status> todos, Retorno r) {
        GestorData.api()
                .negociacoesDoPedido(v.pedidoId)
                .enqueue(
                        new Callback<List<Negociacao>>() {
                            @Override
                            public void onResponse(
                                    Call<List<Negociacao>> c, Response<List<Negociacao>> resp) {
                                if (!resp.isSuccessful() || resp.body() == null) {
                                    r.erro("Não foi possível consultar as negociações do pedido.");
                                    return;
                                }
                                if (resp.isSuccessful() && resp.body() != null) {
                                    for (Negociacao n : resp.body()) {
                                        if (GestorData.coop() != null
                                                && GestorData.coop()
                                                        .equalsIgnoreCase(n.cooperativaId)) {
                                            terminar(r);
                                            return;
                                        }
                                    }
                                }
                                Status em =
                                        achar(
                                                todos,
                                                new String[] {"NEGOCIACAO"},
                                                new String[] {
                                                    "EM_NEGOCIACAO",
                                                    "EM NEGOCIA",
                                                    "EM_ANDAMENTO",
                                                    "EM ANDAMENTO"
                                                });
                                if (em == null) {
                                    r.erro(
                                            "Não foi encontrado um status de negociação em"
                                                    + " andamento.");
                                    return;
                                }
                                GestorData.api()
                                        .abrirNegociacao(
                                                new GestorRequests.Negociacao(
                                                        v.pedidoId,
                                                        GestorData.coop(),
                                                        v.empresaId,
                                                        em.statusId,
                                                        v.valor))
                                        .enqueue(
                                                new Callback<Negociacao>() {
                                                    @Override
                                                    public void onResponse(
                                                            Call<Negociacao> c2,
                                                            Response<Negociacao> r2) {
                                                        if (!r2.isSuccessful()
                                                                || r2.body() == null) {
                                                            r.erro(
                                                                    "Não foi possível abrir a"
                                                                            + " negociação (erro "
                                                                            + r2.code()
                                                                            + ").");
                                                            return;
                                                        }
                                                        terminar(r);
                                                    }

                                                    @Override
                                                    public void onFailure(
                                                            Call<Negociacao> c2, Throwable t) {
                                                        r.erro("Sem conexão com o servidor.");
                                                    }
                                                });
                            }

                            @Override
                            public void onFailure(Call<List<Negociacao>> c, Throwable t) {
                                r.erro("Sem conexão com o servidor.");
                            }
                        });
    }

    private static void terminar(Retorno r) {
        GestorData.invalidar(GestorCache.NEGOCIACOES, GestorCache.PEDIDOS_COOP);
        r.ok();
    }

    public static void recusar(Vista v, Retorno r) {
        GestorData.status(
                new GestorData.Ouvinte<List<Status>>() {
                    private boolean tratado;
                    @Override
                    public void aoReceber(List<Status> todos, boolean doCache) {
                        if (tratado || (doCache && !GestorCache.valido(GestorCache.STATUS))) return;
                        tratado = true;
                        Status rec = achar(todos, new String[] {"PEDIDO"}, new String[] {"RECUS"});
                        if (rec == null) {
                            r.erro(
                                    "O status \"Recusado\" não foi encontrado na tabela de status"
                                            + " da API.");
                            return;
                        }
                        GestorData.api()
                                .statusPedidoCoop(v.pedidoCoopId, rec.statusId)
                                .enqueue(
                                        new Callback<PedidoCoop>() {
                                            @Override
                                            public void onResponse(
                                                    Call<PedidoCoop> c, Response<PedidoCoop> resp) {
                                                if (resp.isSuccessful()) terminar(r);
                                                else
                                                    r.erro(
                                                            "Não foi possível recusar o pedido"
                                                                    + " (erro "
                                                                    + resp.code()
                                                                    + ").");
                                            }

                                            @Override
                                            public void onFailure(Call<PedidoCoop> c, Throwable t) {
                                                r.erro("Sem conexão com o servidor.");
                                            }
                                        });
                    }

                    @Override
                    public void aoErro(String m) {
                        r.erro(m);
                    }
                });
    }

    /** Marca como concluído registrando o valor final (PATCH /negociacoes/{id}/concluir). */
    public static void concluir(String negociacaoId, double valorFinal, Retorno r) {
        GestorData.api()
                .fechar(negociacaoId, new GestorRequests.Fechar(valorFinal, null))
                .enqueue(
                        new Callback<Negociacao>() {
                            @Override
                            public void onResponse(Call<Negociacao> c, Response<Negociacao> resp) {
                                if (resp.isSuccessful()) terminar(r);
                                else r.erro(EmpresaData.erro(resp));
                            }

                            @Override
                            public void onFailure(Call<Negociacao> c, Throwable t) {
                                r.erro("Sem conexão com o servidor.");
                            }
                        });
    }
}
