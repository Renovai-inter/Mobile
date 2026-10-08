package com.example.renovai;

import com.example.renovai.adapter.RateiListAdapter;
import com.example.renovai.controller.ColetaController;
import com.example.renovai.controller.RateiController;
import com.example.renovai.controller.TriagemController;
import com.example.renovai.dto.response.ColetaResponse;
import com.example.renovai.dto.response.TriagemResponse;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class CooperadoPreload {

    private static final long TEMPO_CACHE = 2 * 60 * 1000L;

    // ── cache offline (Firestore, ver FirebaseCache) ──
    private static final Gson GSON = new Gson();
    private static final String CHAVE_COLETAS = "cooperado:coletas";
    private static final String CHAVE_TRIAGENS = "cooperado:triagens";
    private static final String CHAVE_RATEIOS = "cooperado:rateios";
    private static final Type TIPO_COLETAS = new TypeToken<List<ColetaResponse>>() {}.getType();
    private static final Type TIPO_TRIAGENS = new TypeToken<List<TriagemResponse>>() {}.getType();
    private static final Type TIPO_RATEIOS =
            new TypeToken<List<RateiListAdapter.Item>>() {}.getType();
    /** Conta (e-mail) cujas cópias do Firestore já foram restauradas; null = ainda não. */
    private static String contaRestaurada;

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
        // Antes de ir à API, traz as cópias salvas no Firestore (funciona offline). Se a API
        // falhar (sem internet), as telas continuam mostrando esses dados.
        restaurarDoFirestore(CooperadoPreload::buscarDaRede);
    }

    private static void restaurarDoFirestore(Runnable depois) {
        final String conta = FirebaseCache.contaAtual();
        synchronized (CooperadoPreload.class) {
            if (conta == null || conta.equals(contaRestaurada)) {
                depois.run();
                return;
            }
        }
        final int g = geracao;
        FirebaseCache.lerTudo(
                conta,
                mapa -> {
                    synchronized (CooperadoPreload.class) {
                        if (g == geracao) {
                            try {
                                if (coletas.isEmpty() && mapa.containsKey(CHAVE_COLETAS))
                                    coletas = naoNula(GSON.fromJson(mapa.get(CHAVE_COLETAS), TIPO_COLETAS));
                                if (triagens.isEmpty() && mapa.containsKey(CHAVE_TRIAGENS))
                                    triagens = naoNula(GSON.fromJson(mapa.get(CHAVE_TRIAGENS), TIPO_TRIAGENS));
                                if (rateios.isEmpty() && mapa.containsKey(CHAVE_RATEIOS))
                                    rateios = naoNula(GSON.fromJson(mapa.get(CHAVE_RATEIOS), TIPO_RATEIOS));
                            } catch (Exception ignored) {
                                // cópia corrompida/antiga: ignora, a API repõe
                            }
                            contaRestaurada = conta;
                        }
                    }
                    depois.run();
                });
    }

    private static <T> List<T> naoNula(List<T> l) {
        return l != null ? new ArrayList<>(l) : new ArrayList<>();
    }

    private static void salvarNoFirestore(String chave, Object lista, Type tipo) {
        String conta = FirebaseCache.contaAtual();
        if (conta == null) return;
        try {
            FirebaseCache.salvar(conta, chave, GSON.toJson(lista, tipo));
        } catch (Exception ignored) {
            // o cache é só uma cópia — nunca pode quebrar a tela
        }
    }

    private static void buscarDaRede() {
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
                                salvarNoFirestore(CHAVE_COLETAS, coletas, TIPO_COLETAS);

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
                                salvarNoFirestore(CHAVE_TRIAGENS, triagens, TIPO_TRIAGENS);

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
                                    salvarNoFirestore(CHAVE_RATEIOS, rateios, TIPO_RATEIOS);
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
        contaRestaurada = null;
    }
}
