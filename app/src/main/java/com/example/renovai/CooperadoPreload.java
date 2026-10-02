package com.example.renovai;

import com.example.renovai.adapter.RateiListAdapter;
import com.example.renovai.controller.ColetaController;
import com.example.renovai.controller.RateiController;
import com.example.renovai.controller.TriagemController;
import com.example.renovai.dto.response.ColetaResponse;
import com.example.renovai.dto.response.TriagemResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class CooperadoPreload {

    private static final long TEMPO_CACHE = 2 * 60 * 1000L;

    private static List<ColetaResponse> coletas = new ArrayList<>();
    private static List<TriagemResponse> triagens = new ArrayList<>();
    private static List<RateiListAdapter.Item> rateios = new ArrayList<>();

    private static long ultimaAtualizacao = 0;

    private static boolean carregando = false;
    private static final List<Callback> ouvintes = new ArrayList<>();
    private static int geracao, revisao;
    private static String ultimoErro;

    public static synchronized void invalidar() {
        ultimaAtualizacao = 0;
        revisao++;
    }

    public static synchronized String getUltimoErro() {
        return ultimoErro;
    }

    private CooperadoPreload() {}

    public interface Callback {
        void onComplete();
    }

    public static synchronized boolean estaValido() {
        return ultimaAtualizacao > 0
                && System.currentTimeMillis() - ultimaAtualizacao < TEMPO_CACHE;
    }

    public static synchronized boolean estaCarregando() {
        return carregando;
    }

    public static void carregar(Callback callback) {

        if (CooperadoSession.getFuncionarioId() == null) {
            callback.onComplete();
            return;
        }

        if (estaValido()) {
            callback.onComplete();
            return;
        }

        synchronized (CooperadoPreload.class) {
            ouvintes.add(callback);
            if (carregando) {
                return;
            }

            carregando = true;
            ultimoErro = null;
        }
        final int g = geracao, versao = revisao;
        AtomicInteger falhas = new AtomicInteger();

        String cooperadoId = CooperadoSession.getFuncionarioId();
        String cooperativaId = CooperadoSession.getCooperativaId();
        String funcionarioId = CooperadoSession.getFuncionarioId();

        AtomicInteger principaisPendentes = new AtomicInteger(cooperativaId != null ? 3 : 2);
        Callback fim =
                () -> {
                    List<Callback> avisar;
                    synchronized (CooperadoPreload.class) {
                        if (g != geracao) return;
                        if (falhas.get() > 0 || versao != revisao) ultimaAtualizacao = 0;
                        ultimoErro =
                                falhas.get() > 0
                                        ? "Não foi possível atualizar todos os dados. Tente"
                                              + " novamente."
                                        : null;
                        avisar = new ArrayList<>(ouvintes);
                        ouvintes.clear();
                    }
                    for (Callback o : avisar) o.onComplete();
                };

        new ColetaController()
                .listarPorCooperado(
                        cooperadoId,
                        new ColetaController.ListaCallback() {
                            @Override
                            public void onSuccess(List<ColetaResponse> resultado) {
                                if (g != geracao) return;
                                coletas =
                                        resultado != null
                                                ? new ArrayList<>(resultado)
                                                : new ArrayList<>();

                                finalizarPrincipal(principaisPendentes, fim);
                            }

                            @Override
                            public void onErro(String mensagem) {
                                if (g != geracao) return;
                                falhas.incrementAndGet();
                                finalizarPrincipal(principaisPendentes, fim);
                            }
                        });

        new TriagemController()
                .listarPorCooperado(
                        cooperadoId,
                        new TriagemController.ListaCallback() {
                            @Override
                            public void onSuccess(List<TriagemResponse> resultado) {
                                if (g != geracao) return;
                                triagens =
                                        resultado != null
                                                ? new ArrayList<>(resultado)
                                                : new ArrayList<>();

                                finalizarPrincipal(principaisPendentes, fim);
                            }

                            @Override
                            public void onErro(String mensagem) {
                                if (g != geracao) return;
                                falhas.incrementAndGet();
                                finalizarPrincipal(principaisPendentes, fim);
                            }
                        });

        if (cooperativaId != null && funcionarioId != null) {
            new RateiController()
                    .listarRecebidosPorCooperado(
                            cooperativaId,
                            funcionarioId,
                            new RateiController.ListaCallback() {
                                @Override
                                public void onSuccess(List<RateiListAdapter.Item> resultado) {
                                    if (g != geracao) return;
                                    rateios =
                                            resultado != null
                                                    ? new ArrayList<>(resultado)
                                                    : new ArrayList<>();
                                    finalizarPrincipal(principaisPendentes, fim);
                                }

                                @Override
                                public void onErro(String mensagem) {
                                    if (g != geracao) return;
                                    falhas.incrementAndGet();
                                    finalizarPrincipal(principaisPendentes, fim);
                                }
                            });
        }
    }

    private static void finalizarPrincipal(AtomicInteger pendentes, Callback callback) {
        if (pendentes.decrementAndGet() == 0) {

            synchronized (CooperadoPreload.class) {
                ultimaAtualizacao = System.currentTimeMillis();
                carregando = false;
            }

            callback.onComplete();
        }
    }

    public static synchronized List<ColetaResponse> getColetas() {
        return new ArrayList<>(coletas);
    }

    public static synchronized List<TriagemResponse> getTriagens() {
        return new ArrayList<>(triagens);
    }

    public static synchronized List<RateiListAdapter.Item> getRateios() {
        return new ArrayList<>(rateios);
    }

    public static synchronized long getUltimaAtualizacao() {
        return ultimaAtualizacao;
    }

    public static synchronized void limpar() {
        geracao++;
        revisao++;
        ouvintes.clear();
        ultimoErro = null;
        coletas.clear();
        triagens.clear();
        rateios.clear();
        ultimaAtualizacao = 0;
        carregando = false;
    }
}
