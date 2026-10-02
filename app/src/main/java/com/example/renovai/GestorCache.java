package com.example.renovai;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache em memória das listagens do Gestor (mesmo espírito do CooperadoPreload).
 *
 * <p>Diferença importante: os dados NÃO são apagados quando "vencem" — só ficam marcados como
 * velhos. Assim a tela sempre consegue mostrar na hora o que já foi baixado antes e, em paralelo,
 * GestorData busca a versão nova ("mostrar do cache e atualizar depois").
 */
public final class GestorCache {

    public static final String COLETAS = "coletas";
    public static final String TRIAGENS = "triagens";
    public static final String ESTOQUE = "estoque";
    public static final String NEGOCIACOES = "negociacoes";
    public static final String PEDIDOS_COOP = "pedidosCoop";
    public static final String RATEIOS = "rateios";
    public static final String FUNCIONARIOS = "funcionarios";
    public static final String ROTAS = "rotas";
    public static final String STATUS = "status";
    public static final String PERFIL_COOP = "perfilCoop";
    public static final String CARGOS = "cargos";

    private static final long VALIDADE_MS = 2 * 60 * 1000L;

    private static final Map<String, Object> dados = new HashMap<>();
    private static final Map<String, Long> instantes = new HashMap<>();
    private static long sessao;

    public static synchronized long sessao() {
        return sessao;
    }

    private GestorCache() {}

    public static synchronized void guardar(String chave, Object valor) {
        dados.put(chave, valor);
        instantes.put(chave, System.currentTimeMillis());
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T> T obter(String chave) {
        return (T) dados.get(chave);
    }

    public static synchronized boolean valido(String chave) {
        Long t = instantes.get(chave);
        return t != null && t > 0 && System.currentTimeMillis() - t < VALIDADE_MS;
    }

    /**
     * Marca como velho (a próxima leitura rebusca), mas mantém o conteúdo para exibição imediata.
     */
    public static synchronized void invalidar(String... chaves) {
        for (String c : chaves) {
            if (instantes.containsKey(c)) instantes.put(c, 0L);
        }
    }

    public static synchronized void invalidarTudo() {
        for (String c : new java.util.ArrayList<>(instantes.keySet())) instantes.put(c, 0L);
    }

    public static synchronized void limpar() {
        sessao++;
        dados.clear();
        instantes.clear();
    }
}
