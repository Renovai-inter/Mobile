package com.example.renovai.controller;

import com.example.renovai.ApiClient;
import com.example.renovai.TriagemApiService;
import com.example.renovai.dto.request.ConcluirTriagemRequest;
import com.example.renovai.dto.request.TriagemRequest;
import com.example.renovai.dto.response.TriagemResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller de Triagem para a área de Cooperado (telas 2.1, 2.3, 2.7). Endpoint real:
 * com.renovai.api.controller.TriagemController (backend).
 */
public class TriagemController {

    /**
     * Peso "marcador" das linhas criadas pelo gestor na tela 4.8.2. A API só aceita quantidadeKg >=
     * 0.001 em POST /triagens, então uma linha recém-criada (material ainda não separado) nasce com
     * 0,001 kg. Para o app isso significa "sem peso ainda".
     */
    public static final BigDecimal PESO_INICIAL = new BigDecimal("0.001");

    public static String validarPesos(BigDecimal peso, BigDecimal rejeito) {
        if (peso == null || peso.compareTo(PESO_INICIAL) < 0)
            return "Informe um peso de pelo menos 0,001 kg.";
        if (rejeito == null || rejeito.signum() < 0)
            return "Informe um peso de rejeito válido e não negativo.";
        if (rejeito.compareTo(peso) > 0) return "O rejeito não pode exceder o peso separado.";
        if (peso.stripTrailingZeros().scale() > 3 || rejeito.stripTrailingZeros().scale() > 3)
            return "Use até três casas decimais nos pesos.";
        return null;
    }

    public static boolean semPeso(BigDecimal kg) {
        return kg == null || kg.compareTo(PESO_INICIAL) <= 0;
    }

    /** Peso real (0 enquanto a linha só tem o peso marcador). */
    public static BigDecimal pesoReal(BigDecimal kg) {
        return semPeso(kg) ? BigDecimal.ZERO : kg;
    }

    public static BigDecimal pesoReal(TriagemResponse linha) {
        if (linha.getQuantidadeKg() == null) return BigDecimal.ZERO;
        if (com.example.renovai.GestorUi.tem(linha.getStatusAtual(), "CONCLU"))
            return linha.getQuantidadeKg();
        return pesoReal(linha.getQuantidadeKg());
    }

    public interface ListaCallback {
        void onSuccess(List<TriagemResponse> triagens);

        void onErro(String mensagem);
    }

    public interface SalvarCallback {
        void onSuccess(TriagemResponse triagem);

        void onErro(String mensagem);
    }

    private final TriagemApiService service;

    public TriagemController() {
        this.service = ApiClient.createService(TriagemApiService.class);
    }

    /** GET /triagens/abertas/por-cooperado/{cooperadoId} — telas 2.1 e 2.3. */
    public void listarAbertasPorCooperado(String cooperadoId, ListaCallback callback) {
        service.listarAbertasPorCooperado(cooperadoId)
                .enqueue(
                        new Callback<List<TriagemResponse>>() {
                            @Override
                            public void onResponse(
                                    Call<List<TriagemResponse>> call,
                                    Response<List<TriagemResponse>> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    callback.onSuccess(response.body());
                                } else {
                                    callback.onErro(
                                            "Erro "
                                                    + response.code()
                                                    + " ao carregar triagens em aberto.");
                                }
                            }

                            @Override
                            public void onFailure(Call<List<TriagemResponse>> call, Throwable t) {
                                callback.onErro("Não foi possível conectar ao servidor.");
                            }
                        });
    }

    /** GET /triagens/por-cooperado/{cooperadoId} — histórico completo (tela 2.7). */
    public void listarPorCooperado(String cooperadoId, ListaCallback callback) {
        service.listarPorCooperado(cooperadoId)
                .enqueue(
                        new Callback<List<TriagemResponse>>() {
                            @Override
                            public void onResponse(
                                    Call<List<TriagemResponse>> call,
                                    Response<List<TriagemResponse>> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    callback.onSuccess(response.body());
                                } else {
                                    callback.onErro(
                                            "Erro "
                                                    + response.code()
                                                    + " ao carregar histórico de triagens.");
                                }
                            }

                            @Override
                            public void onFailure(Call<List<TriagemResponse>> call, Throwable t) {
                                callback.onErro("Não foi possível conectar ao servidor.");
                            }
                        });
    }

    /** GET /triagens/por-coleta/{coletaId} — todos os materiais de uma coleta (tela 2.3). */
    public void listarPorColeta(String coletaId, ListaCallback callback) {
        service.listarPorColeta(coletaId)
                .enqueue(
                        new Callback<List<TriagemResponse>>() {
                            @Override
                            public void onResponse(
                                    Call<List<TriagemResponse>> call,
                                    Response<List<TriagemResponse>> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    callback.onSuccess(response.body());
                                } else {
                                    callback.onErro(
                                            "Erro "
                                                    + response.code()
                                                    + " ao carregar materiais da triagem.");
                                }
                            }

                            @Override
                            public void onFailure(Call<List<TriagemResponse>> call, Throwable t) {
                                callback.onErro("Não foi possível conectar ao servidor.");
                            }
                        });
    }

    /**
     * PUT /triagens/{id} — "salvar progresso" de UM material (tela 2.3, botão "Separar material").
     * equipeId/coletaId/materialId precisam ser reenviados porque o backend espera o TriagemRequest
     * completo no PUT (ver TriagemService.atualizar()); statusId vai null de propósito — ver
     * TriagemRequest.java.
     */
    public void salvarProgresso(
            String triagemId,
            String equipeId,
            String coletaId,
            String materialId,
            BigDecimal quantidadeKg,
            BigDecimal quantidadeRejeitoKg,
            String imagemUrl,
            SalvarCallback callback) {
        if (quantidadeKg == null || quantidadeKg.compareTo(PESO_INICIAL) < 0) {
            callback.onErro("Informe o peso separado.");
            return;
        }
        if (quantidadeRejeitoKg != null
                && (quantidadeRejeitoKg.signum() < 0
                        || quantidadeRejeitoKg.compareTo(quantidadeKg) > 0)) {
            callback.onErro("O peso rejeitado não pode ser maior que o peso separado.");
            return;
        }

        TriagemRequest request =
                new TriagemRequest(
                        equipeId,
                        coletaId,
                        materialId,
                        null,
                        quantidadeKg,
                        quantidadeRejeitoKg,
                        imagemUrl);

        service.atualizar(triagemId, request)
                .enqueue(
                        new Callback<TriagemResponse>() {
                            @Override
                            public void onResponse(
                                    Call<TriagemResponse> call,
                                    Response<TriagemResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    com.example.renovai.CooperadoPreload.invalidar();
                                    com.example.renovai.GestorData.invalidar(
                                            com.example.renovai.GestorCache.TRIAGENS,
                                            com.example.renovai.GestorCache.ESTOQUE);
                                    callback.onSuccess(response.body());
                                } else {
                                    callback.onErro(
                                            "Erro ao salvar progresso (código "
                                                    + response.code()
                                                    + ").");
                                }
                            }

                            @Override
                            public void onFailure(Call<TriagemResponse> call, Throwable t) {
                                callback.onErro("Não foi possível conectar ao servidor.");
                            }
                        });
    }

    /** PATCH /triagens/{id}/concluir — "marcar como concluída" (tela 2.3). */
    public void concluir(
            String triagemId,
            BigDecimal quantidadeFinalKg,
            String observacao,
            SalvarCallback callback) {
        if (quantidadeFinalKg == null || quantidadeFinalKg.compareTo(PESO_INICIAL) < 0) {
            callback.onErro("Informe um peso final de pelo menos 0,001 kg.");
            return;
        }
        ConcluirTriagemRequest request = new ConcluirTriagemRequest(quantidadeFinalKg, observacao);

        service.concluir(triagemId, request)
                .enqueue(
                        new Callback<TriagemResponse>() {
                            @Override
                            public void onResponse(
                                    Call<TriagemResponse> call,
                                    Response<TriagemResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    com.example.renovai.CooperadoPreload.invalidar();
                                    com.example.renovai.GestorData.invalidar(
                                            com.example.renovai.GestorCache.TRIAGENS,
                                            com.example.renovai.GestorCache.ESTOQUE);
                                    callback.onSuccess(response.body());
                                } else {
                                    callback.onErro(
                                            "Erro ao concluir triagem (código "
                                                    + response.code()
                                                    + ").");
                                }
                            }

                            @Override
                            public void onFailure(Call<TriagemResponse> call, Throwable t) {
                                callback.onErro("Não foi possível conectar ao servidor.");
                            }
                        });
    }

    /**
     * Agrupa uma lista "achatada" de TriagemResponse (uma linha por material) em um Grupo por
     * coleta (coletaId) — é o que telas 2.1 e 2.7 mostram como "um cartão de triagem". Critérios
     * (heurística documentada em IMPLEMENTACAO.md, já que a API não expõe um "status do grupo"): -
     * CONCLUIDA: todos os materiais da coleta têm statusAtual contendo "CONCLU". - PENDENTE: nenhum
     * material tem quantidadeKg > 0 ainda. - EM_ANDAMENTO: qualquer outra combinação. Progresso =
     * kg concluído / kg total (todas as linhas), em percentual.
     */
    public static List<com.example.renovai.adapter.TriagemListAdapter.Grupo> agruparPorColeta(
            List<TriagemResponse> triagens) {

        java.util.LinkedHashMap<String, List<TriagemResponse>> porColeta =
                new java.util.LinkedHashMap<>();
        for (TriagemResponse t : triagens) {
            porColeta.computeIfAbsent(t.getColetaId(), k -> new java.util.ArrayList<>()).add(t);
        }

        List<com.example.renovai.adapter.TriagemListAdapter.Grupo> resultado =
                new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, List<TriagemResponse>> entrada : porColeta.entrySet()) {
            List<TriagemResponse> materiais = entrada.getValue();

            BigDecimal kgTotal = BigDecimal.ZERO;
            BigDecimal kgConcluido = BigDecimal.ZERO;
            boolean algumProgresso = false;
            boolean todosConcluidos = true;
            String dataMaisRecente = null;
            String triagemIdReferencia = materiais.get(0).getTriagemId();

            for (TriagemResponse m : materiais) {
                BigDecimal kg = pesoReal(m);
                kgTotal = kgTotal.add(kg);

                boolean concluido =
                        m.getStatusAtual() != null
                                && m.getStatusAtual()
                                        .toUpperCase(java.util.Locale.ROOT)
                                        .contains("CONCLU");
                if (concluido) {
                    kgConcluido = kgConcluido.add(kg);
                } else {
                    todosConcluidos = false;
                }
                if (kg.compareTo(BigDecimal.ZERO) > 0) algumProgresso = true;

                if (m.getDataTriagem() != null
                        && (dataMaisRecente == null
                                || m.getDataTriagem().compareTo(dataMaisRecente) > 0)) {
                    dataMaisRecente = m.getDataTriagem();
                }
            }

            com.example.renovai.adapter.TriagemListAdapter.StatusGrupo status;
            if (todosConcluidos) {
                status = com.example.renovai.adapter.TriagemListAdapter.StatusGrupo.CONCLUIDA;
            } else if (algumProgresso) {
                status = com.example.renovai.adapter.TriagemListAdapter.StatusGrupo.EM_ANDAMENTO;
            } else {
                status = com.example.renovai.adapter.TriagemListAdapter.StatusGrupo.PENDENTE;
            }

            int progresso =
                    kgTotal.compareTo(BigDecimal.ZERO) > 0
                            ? kgConcluido
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(kgTotal, 0, java.math.RoundingMode.HALF_UP)
                                    .intValue()
                            : 0;

            resultado.add(
                    new com.example.renovai.adapter.TriagemListAdapter.Grupo(
                            entrada.getKey(),
                            triagemIdReferencia,
                            status,
                            status
                                            == com.example.renovai.adapter.TriagemListAdapter
                                                    .StatusGrupo.CONCLUIDA
                                    ? kgTotal
                                    : kgConcluido,
                            progresso,
                            dataMaisRecente));
        }
        return resultado;
    }
}
