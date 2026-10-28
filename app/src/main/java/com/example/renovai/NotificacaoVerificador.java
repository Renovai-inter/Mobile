package com.example.renovai;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.renovai.Notificacoes.Aviso;
import com.example.renovai.Notificacoes.Perfil;
import com.example.renovai.NotificacaoRegras.Mudanca;
import com.example.renovai.dto.response.ColetaResponse;
import com.example.renovai.dto.response.EstoqueResponse;
import com.example.renovai.dto.response.GestorResponses;
import com.example.renovai.dto.response.RateioFuncionarioResponse;
import com.example.renovai.dto.response.RateioListaResponse;
import com.example.renovai.dto.response.TriagemResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Uma rodada de verificação (roda no NotificacaoWorker, fora da thread principal): busca na API
 * os dados do perfil logado, compara com o retrato salvo da rodada anterior e mostra os avisos.
 * Chama a API direto (execute) em vez do GestorData/EmpresaData para não mexer no cache das telas.
 */
final class NotificacaoVerificador {

    private static final String PREFS = "renovai_notificacoes";
    private static final Type TIPO_RETRATO = new TypeToken<Map<String, String>>() {}.getType();
    private static final Gson GSON = new Gson();

    private final Context c;
    private final SharedPreferences prefs;
    private final String conta;
    private final Perfil perfil;
    private final List<Aviso> avisos = new ArrayList<>();

    private NotificacaoVerificador(Context c, Perfil perfil) {
        this.c = c;
        this.perfil = perfil;
        this.conta = perfil + ":" + SessionManager.getEmail();
        this.prefs = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Uma rodada por vez: a periódica e a "verificar agora" não podem duplicar avisos. */
    static synchronized void verificar(Context c) {
        Perfil perfil = Notificacoes.perfil();
        if (perfil == null) return; // deslogado ou perfil sem notificações
        NotificacaoVerificador v = new NotificacaoVerificador(c, perfil);
        switch (perfil) {
            case GESTOR:
                v.gestor();
                break;
            case COOPERADO:
                v.cooperado();
                break;
            case EMPRESA:
                v.empresa();
                break;
        }
        // conta trocou durante a rodada (logout/login): descarta os avisos da conta anterior
        if (perfil != Notificacoes.perfil()) return;
        Notificacoes.criarCanais(c);
        for (Aviso a : v.avisos) Notificacoes.mostrar(c, a);
    }

    // ───────────────────────────── Gestor ─────────────────────────────

    private void gestor() {
        GestorApiService api = ApiClient.createService(GestorApiService.class);
        String coop = CooperadoSession.getCooperativaId();

        // Propostas recebidas: pedido novo enviado para a cooperativa, ainda sem resposta
        List<GestorResponses.PedidoCoop> pedidos = buscar(api.pedidosCoop(coop));
        if (pedidos != null) {
            Map<String, String> agora = new HashMap<>();
            Map<String, GestorResponses.PedidoCoop> porId = new HashMap<>();
            for (GestorResponses.PedidoCoop p : pedidos) {
                agora.put(p.pedidoCooperativaId, String.valueOf(p.statusAtual));
                porId.put(p.pedidoCooperativaId, p);
            }
            for (Mudanca m : NotificacaoRegras.comparar(ler("g_pedidos"), agora)) {
                GestorResponses.PedidoCoop p = porId.get(m.id);
                if (!m.ehNovo() || !GestorPedidos.ehPendente(p.statusAtual)) continue;
                GestorResponses.Pedido ped = buscar(api.pedido(p.pedidoId));
                String empresa = ped != null && ped.empresaNome != null ? ped.empresaNome : "Uma empresa";
                avisar(Notificacoes.CANAL_PEDIDOS, "g_proposta:" + m.id, "Nova proposta recebida",
                        empresa + " enviou um pedido para a cooperativa.", R.id.nav_g_home);
            }
            gravar("g_pedidos", agora);
        }

        // Propostas aceitas pela empresa e finalizadas
        List<GestorResponses.Negociacao> negs = buscar(api.negociacoes(coop));
        if (negs != null) {
            Map<String, GestorResponses.Negociacao> porId = porNegociacao(negs);
            Map<String, String> agora = retratoNegociacoes(negs);
            for (Mudanca m : NotificacaoRegras.comparar(ler("g_negociacoes"), agora)) {
                GestorResponses.Negociacao n = porId.get(m.id);
                String empresa = n.empresaNome != null ? n.empresaNome : "a empresa";
                if (NotificacaoRegras.chegouAoEstado(m, EmpresaStatus.FINALIZADO)) {
                    avisar(Notificacoes.CANAL_PEDIDOS, "g_neg:" + m.id, "Proposta finalizada",
                            "O pedido de " + empresa + " foi finalizado ("
                                    + GestorUi.dinheiro(n.valorTotal) + ").", R.id.nav_g_home);
                } else if (NotificacaoRegras.chegouAoEstado(m, EmpresaStatus.ACEITO)) {
                    avisar(Notificacoes.CANAL_PEDIDOS, "g_neg:" + m.id, "Proposta aceita",
                            capitalizar(empresa) + " aceitou a proposta de "
                                    + GestorUi.dinheiro(n.valorTotal) + ".", R.id.nav_g_home);
                }
            }
            gravar("g_negociacoes", agora);
        }

        // Coletas: registradas e mudanças de estado
        List<ColetaResponse> coletas = buscar(api.coletas(coop));
        if (coletas != null) {
            Map<String, String> agora = new HashMap<>();
            Map<String, ColetaResponse> porId = new HashMap<>();
            for (ColetaResponse col : coletas) {
                agora.put(col.getColetaId(), String.valueOf(col.getStatusAtual()));
                porId.put(col.getColetaId(), col);
            }
            for (Mudanca m : NotificacaoRegras.comparar(ler("g_coletas"), agora)) {
                ColetaResponse col = porId.get(m.id);
                String quem = col.getCooperadoNome() != null ? col.getCooperadoNome() : "Um cooperado";
                if (m.ehNovo()) {
                    avisar(Notificacoes.CANAL_OPERACAO, "g_coleta:" + m.id, "Nova coleta registrada",
                            quem + " registrou uma coleta de " + GestorUi.kg(kg(col.getQuantidadeKg()))
                                    + " (" + rotulo(m.novo) + ").", R.id.nav_g_coletas);
                } else {
                    avisar(Notificacoes.CANAL_OPERACAO, "g_coleta:" + m.id, "Coleta mudou de estado",
                            "Coleta de " + quem + ": " + rotulo(m.antigo) + " → " + rotulo(m.novo)
                                    + ".", R.id.nav_g_coletas);
                }
            }
            gravar("g_coletas", agora);
        }

        // Triagens: coleta entrou em triagem e mudanças de estado (pendente → em andamento → concluída)
        List<TriagemResponse> triagens = buscar(api.triagens(coop));
        if (triagens != null) {
            Map<String, String> agora = new HashMap<>();
            Map<String, TriagemResponse> porId = new HashMap<>();
            for (TriagemResponse t : triagens) {
                agora.put(t.getTriagemId(), String.valueOf(t.getStatusAtual()));
                porId.put(t.getTriagemId(), t);
            }
            for (Mudanca m : NotificacaoRegras.comparar(ler("g_triagens"), agora)) {
                TriagemResponse t = porId.get(m.id);
                String material = t.getMaterialCategoria() != null ? t.getMaterialCategoria() : "material";
                if (m.ehNovo()) {
                    avisar(Notificacoes.CANAL_OPERACAO, "g_triagem:" + m.id, "Coleta enviada para triagem",
                            capitalizar(material) + " (" + GestorUi.kg(kg(t.getQuantidadeKg()))
                                    + ") entrou em triagem: " + rotulo(m.novo) + ".", R.id.nav_g_coletas);
                } else {
                    avisar(Notificacoes.CANAL_OPERACAO, "g_triagem:" + m.id, "Triagem mudou de estado",
                            "Triagem de " + material + ": " + rotulo(m.antigo) + " → " + rotulo(m.novo)
                                    + ".", R.id.nav_g_coletas);
                }
            }
            gravar("g_triagens", agora);
        }
    }

    // ───────────────────────────── Cooperado ─────────────────────────────

    private void cooperado() {
        String coop = CooperadoSession.getCooperativaId();

        // Estoque: material que caiu abaixo de 1.000 kg
        Map<String, String> estoque = new HashMap<>();
        Map<String, String> nomes = new HashMap<>();
        List<GestorResponses.Estoque> porCoop =
                buscar(ApiClient.createService(GestorApiService.class).estoque(coop));
        if (porCoop != null) {
            for (GestorResponses.Estoque e : porCoop) {
                estoque.put(e.estoqueId, String.valueOf(e.quantidadeKg == null ? 0 : e.quantidadeKg));
                nomes.put(e.estoqueId, e.materialCategoria);
            }
        } else {
            // o cooperado pode não ter acesso ao endpoint do gestor: usa a listagem geral
            List<EstoqueResponse> geral =
                    buscar(ApiClient.createService(EstoqueApiService.class).listar(coop, null));
            if (geral == null) estoque = null;
            else
                for (EstoqueResponse e : geral) {
                    estoque.put(e.getEstoqueId(), String.valueOf(kg(e.getQuantidadeKg())));
                    nomes.put(e.getEstoqueId(), e.getMaterialCategoria());
                }
        }
        if (estoque != null) {
            Map<String, String> antes = ler("c_estoque");
            for (Mudanca m : NotificacaoRegras.comparar(antes, estoque)) {
                if (!NotificacaoRegras.caiuAbaixoDoLimite(m.antigo, m.novo)) continue;
                String material = nomes.get(m.id) != null ? capitalizar(nomes.get(m.id)) : "Um material";
                avisar(Notificacoes.CANAL_ESTOQUE, "c_estoque:" + m.id, "Estoque baixo: " + material,
                        material + " está com " + GestorUi.kg(Double.parseDouble(m.novo))
                                + ", abaixo de 1.000 kg.", R.id.nav_home_cooperado);
            }
            gravar("c_estoque", estoque);
        }

        // Rateios realizados em que o cooperado entrou
        RateioApiService rateioApi = ApiClient.createService(RateioApiService.class);
        List<RateioListaResponse> rateios = buscar(rateioApi.listarPorCooperativa(coop));
        if (rateios != null) {
            Map<String, String> antes = ler("c_rateios");
            Map<String, String> agora = new HashMap<>();
            String eu = CooperadoSession.getFuncionarioId();
            for (RateioListaResponse r : rateios) {
                if (antes == null || antes.containsKey(r.getRateioId())) {
                    agora.put(r.getRateioId(), "1");
                    continue;
                }
                // rateio novo: confere se o cooperado está na distribuição
                List<RateioFuncionarioResponse> dist =
                        buscar(rateioApi.listarDistribuicao(r.getRateioId()));
                if (dist == null) continue; // fica para a próxima rodada
                agora.put(r.getRateioId(), "1");
                for (RateioFuncionarioResponse f : dist) {
                    if (eu == null || !eu.equalsIgnoreCase(f.getFuncionarioId())) continue;
                    String tipo = r.getTipoRateio() != null
                            ? " " + r.getTipoRateio().toLowerCase(Locale.ROOT) : "";
                    avisar(Notificacoes.CANAL_RATEIOS, "c_rateio:" + r.getRateioId(), "Rateio realizado",
                            "Você recebeu " + GestorUi.dinheiro(kg(f.getValorRateio())) + " no rateio"
                                    + tipo + " de " + GestorUi.data(r.getDataRateio()) + ".",
                            R.id.nav_rateios);
                    break;
                }
            }
            gravar("c_rateios", agora);
        }
    }

    // ───────────────────────────── Empresa ─────────────────────────────

    private void empresa() {
        EmpresaApiService api = ApiClient.createService(EmpresaApiService.class);
        List<GestorResponses.Negociacao> negs = buscar(api.negociacoesPorEmpresa());
        if (negs == null) return;
        Map<String, GestorResponses.Negociacao> porId = porNegociacao(negs);
        Map<String, String> agora = retratoNegociacoes(negs);
        for (Mudanca m : NotificacaoRegras.comparar(ler("e_negociacoes"), agora)) {
            GestorResponses.Negociacao n = porId.get(m.id);
            String coop = n.cooperativaNome != null ? n.cooperativaNome : "A cooperativa";
            if (NotificacaoRegras.chegouAoEstado(m, EmpresaStatus.FINALIZADO)) {
                avisar(Notificacoes.CANAL_PEDIDOS, "e_neg:" + m.id, "Proposta finalizada",
                        coop + " finalizou o pedido (" + GestorUi.dinheiro(n.valorTotal) + ").",
                        R.id.nav_pedidos);
            } else if (NotificacaoRegras.ehContraproposta(m)) {
                avisar(Notificacoes.CANAL_PEDIDOS, "e_neg:" + m.id, "Nova contraproposta",
                        coop + " enviou uma contraproposta de " + GestorUi.dinheiro(n.valorTotal)
                                + ".", R.id.nav_pedidos);
            }
        }
        gravar("e_negociacoes", agora);
    }

    // ───────────────────────────── apoio ─────────────────────────────

    private static Map<String, String> retratoNegociacoes(List<GestorResponses.Negociacao> negs) {
        Map<String, String> r = new HashMap<>();
        for (GestorResponses.Negociacao n : negs)
            r.put(n.negociacaoId, NotificacaoRegras.retratoNegociacao(
                    EmpresaStatus.estado(n, null, null), n.valorTotal));
        return r;
    }

    private static Map<String, GestorResponses.Negociacao> porNegociacao(
            List<GestorResponses.Negociacao> negs) {
        Map<String, GestorResponses.Negociacao> r = new HashMap<>();
        for (GestorResponses.Negociacao n : negs) r.put(n.negociacaoId, n);
        return r;
    }

    private void avisar(String canal, String chave, String titulo, String texto, int aba) {
        avisos.add(new Aviso(canal, chave, titulo, texto, Notificacoes.casca(perfil), aba));
    }

    /** Retrato salvo da última rodada (null na primeira vez desta conta). */
    private Map<String, String> ler(String categoria) {
        String json = prefs.getString(conta + "|" + categoria, null);
        if (json == null) return null;
        try {
            return GSON.fromJson(json, TIPO_RETRATO);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void gravar(String categoria, Map<String, String> retrato) {
        prefs.edit().putString(conta + "|" + categoria, GSON.toJson(retrato)).apply();
    }

    /** Resposta da API, ou null se falhou (aí a categoria fica para a próxima rodada). */
    private static <T> T buscar(Call<T> call) {
        try {
            Response<T> r = call.execute();
            return r.isSuccessful() ? r.body() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Double kg(BigDecimal v) {
        return v == null ? 0d : v.doubleValue();
    }

    /** "EM_ANDAMENTO" → "Em andamento". */
    private static String rotulo(String status) {
        if (status == null || status.equals("null")) return "sem status";
        return capitalizar(status.replace('_', ' ').toLowerCase(Locale.ROOT));
    }

    private static String capitalizar(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
