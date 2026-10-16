package com.example.renovai;

import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cache das listagens do Gestor e da Empresa (mesmo espírito do CooperadoPreload).
 *
 * <p>Diferença importante: os dados NÃO são apagados quando "vencem" — só ficam marcados como
 * velhos. Assim a tela sempre consegue mostrar na hora o que já foi baixado antes e, em paralelo,
 * GestorData/EmpresaData buscam a versão nova ("mostrar do cache e atualizar depois").
 *
 * <p>PERSISTÊNCIA (Firestore): toda listagem cujo tipo foi registrado com {@link #registrarTipo}
 * também é gravada no Firestore (ver {@link FirebaseCache}). Quando o app abre de novo — mesmo sem
 * internet — {@link #aposRestaurar} traz essas cópias de volta para a memória (marcadas como
 * velhas), e a tela mostra os dados antigos enquanto a API responde.
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
    private static final Gson GSON = new Gson();

    private static final Map<String, Object> dados = new HashMap<>();
    private static final Map<String, Long> instantes = new HashMap<>();
    private static long sessao;

    // ── persistência ──
    /** Tipo Java de cada chave persistida (necessário para o Gson reconstruir listas genéricas). */
    private static final Map<String, Type> tipos = new HashMap<>();
    /** JSON restaurado do Firestore e ainda não convertido (o tipo pode ser registrado depois). */
    private static final Map<String, String> brutos = new HashMap<>();
    /** Conta (e-mail) cujo cache já foi restaurado nesta sessão; null = ainda não restaurou. */
    private static String contaRestaurada;
    private static boolean restaurando;
    private static final List<Runnable> esperandoRestauracao = new ArrayList<>();

    public static synchronized long sessao() {
        return sessao;
    }

    private GestorCache() {}

    /** Registra o tipo de uma chave para que ela seja salva e restaurada do Firestore. */
    public static synchronized void registrarTipo(String chave, Type tipo) {
        tipos.put(chave, tipo);
    }

    public static synchronized void guardar(String chave, Object valor) {
        dados.put(chave, valor);
        instantes.put(chave, System.currentTimeMillis());
        brutos.remove(chave);
        Type tipo = tipos.get(chave);
        String conta = FirebaseCache.contaAtual();
        if (tipo != null && valor != null && conta != null) {
            try {
                FirebaseCache.salvar(conta, chave, GSON.toJson(valor, tipo));
            } catch (Exception ignored) {
                // cache é só uma cópia — nunca pode quebrar a tela
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T> T obter(String chave) {
        Object v = dados.get(chave);
        if (v == null) v = converterBruto(chave);
        return (T) v;
    }

    /** Converte (sob demanda) uma cópia restaurada do Firestore. Fica marcada como velha. */
    private static Object converterBruto(String chave) {
        String json = brutos.get(chave);
        Type tipo = tipos.get(chave);
        if (json == null || tipo == null) return null;
        try {
            Object v = GSON.fromJson(json, tipo);
            if (v != null) {
                dados.put(chave, v);
                instantes.put(chave, 0L); // velho: a tela mostra e já pede a versão nova
            }
            brutos.remove(chave);
            return v;
        } catch (Exception e) {
            brutos.remove(chave);
            return null;
        }
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
        for (String c : new ArrayList<>(instantes.keySet())) instantes.put(c, 0L);
    }

    /**
     * Limpa a MEMÓRIA (troca de conta / logout). As cópias no Firestore ficam guardadas por conta e
     * só voltam quando a mesma conta logar de novo.
     */
    public static synchronized void limpar() {
        sessao++;
        dados.clear();
        instantes.clear();
        brutos.clear();
        contaRestaurada = null;
    }

    /**
     * Garante que as cópias do Firestore da conta logada já foram trazidas para a memória e então
     * executa {@code acao}. Na primeira chamada lê o cache LOCAL do Firestore (rápido, funciona
     * offline); nas seguintes executa na hora.
     */
    public static void aposRestaurar(Runnable acao) {
        final String conta = FirebaseCache.contaAtual();
        synchronized (GestorCache.class) {
            if (conta == null || conta.equals(contaRestaurada)) {
                acao.run();
                return;
            }
            esperandoRestauracao.add(acao);
            if (restaurando) return;
            restaurando = true;
        }
        final long sessaoInicio = sessao();
        FirebaseCache.lerTudo(
                conta,
                mapa -> {
                    List<Runnable> executar;
                    synchronized (GestorCache.class) {
                        if (sessaoInicio == sessao) {
                            for (Map.Entry<String, String> e : mapa.entrySet()) {
                                // não sobrescreve o que já chegou da API nesse meio-tempo
                                if (!dados.containsKey(e.getKey()))
                                    brutos.put(e.getKey(), e.getValue());
                            }
                        }
                        contaRestaurada = conta;
                        restaurando = false;
                        executar = new ArrayList<>(esperandoRestauracao);
                        esperandoRestauracao.clear();
                    }
                    for (Runnable r : executar) r.run();
                });
    }
}
