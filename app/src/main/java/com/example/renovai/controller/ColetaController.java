package com.example.renovai.controller;

import com.example.renovai.ApiClient;
import com.example.renovai.ColetaApiService;
import com.example.renovai.dto.request.ColetaRequest;
import com.example.renovai.dto.response.ColetaResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller de Coleta para a área de Cooperado (telas 2.1, 2.2, 2.6). Endpoint real:
 * com.renovai.api.controller.ColetaController (backend).
 */
public class ColetaController {
    public static ColetaRequest montarRequest(
            String cooperadoId, BigDecimal peso, String tipo, String rotaId, String imagemUrl) {
        boolean externa = "EXTERNA".equalsIgnoreCase(tipo) || "EXTERNO".equalsIgnoreCase(tipo);
        return new ColetaRequest(
                cooperadoId,
                null,
                peso,
                imagemUrl,
                externa ? "EXTERNA" : "ENTREGA",
                externa ? rotaId : null);
    }

    public interface ListaCallback {
        void onSuccess(List<ColetaResponse> coletas);

        void onErro(String mensagem);
    }

    public interface CriarCallback {
        void onSuccess(ColetaResponse coleta);

        void onErro(String mensagem);
    }

    private final ColetaApiService service;

    public ColetaController() {
        this.service = ApiClient.createService(ColetaApiService.class);
    }

    /** GET /coletas/por-cooperado/{cooperadoId} — histórico do cooperado (telas 2.1 e 2.6). */
    public void listarPorCooperado(String cooperadoId, ListaCallback callback) {
        service.listarPorCooperado(cooperadoId)
                .enqueue(
                        new Callback<List<ColetaResponse>>() {
                            @Override
                            public void onResponse(
                                    Call<List<ColetaResponse>> call,
                                    Response<List<ColetaResponse>> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    callback.onSuccess(response.body());
                                } else {
                                    callback.onErro(
                                            "Erro " + response.code() + " ao carregar coletas.");
                                }
                            }

                            @Override
                            public void onFailure(Call<List<ColetaResponse>> call, Throwable t) {
                                callback.onErro("Não foi possível conectar ao servidor.");
                            }
                        });
    }

    /**
     * POST /coletas (tela 2.2). O pedido já vem montado pela tela (montarRequest + materiais,
     * triagem e descrição); aqui ficam as conferências básicas e o tratamento da resposta.
     */
    public void criar(ColetaRequest request, CriarCallback callback) {
        if (request.getCooperadoId() == null || request.getCooperadoId().trim().isEmpty()) {
            callback.onErro("Não foi possível identificar o cooperado logado.");
            return;
        }
        if (request.getQuantidadeKg() == null
                || request.getQuantidadeKg().compareTo(new BigDecimal("0.001")) < 0) {
            callback.onErro("Informe o peso da coleta.");
            return;
        }
        if ("EXTERNA".equals(request.getTipoColeta())
                && (request.getRotaId() == null || request.getRotaId().trim().isEmpty())) {
            callback.onErro("Selecione a rota de origem para coletas externas.");
            return;
        }

        service.criar(request)
                .enqueue(
                        new Callback<ColetaResponse>() {
                            @Override
                            public void onResponse(
                                    Call<ColetaResponse> call, Response<ColetaResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    com.example.renovai.CooperadoPreload.invalidar();
                                    com.example.renovai.GestorData.invalidar(
                                            com.example.renovai.GestorCache.COLETAS);
                                    callback.onSuccess(response.body());
                                } else {
                                    // a API explica o motivo (ex.: soma dos pesos diferente do total)
                                    callback.onErro(
                                            com.example.renovai.EmpresaData.erro(response));
                                }
                            }

                            @Override
                            public void onFailure(Call<ColetaResponse> call, Throwable t) {
                                callback.onErro("Não foi possível conectar ao servidor.");
                            }
                        });
    }

    /** Equipe do cooperado (a primeira), ou null se ele não está em nenhuma. */
    public void equipeDoCooperado(String cooperadoId, java.util.function.Consumer<String> retorno) {
        service.equipesDoCooperado(cooperadoId)
                .enqueue(
                        new Callback<List<ColetaApiService.EquipeDoCooperado>>() {
                            @Override
                            public void onResponse(
                                    Call<List<ColetaApiService.EquipeDoCooperado>> call,
                                    Response<List<ColetaApiService.EquipeDoCooperado>> r) {
                                String id = null;
                                if (r.isSuccessful() && r.body() != null)
                                    for (ColetaApiService.EquipeDoCooperado e : r.body())
                                        if (e.equipeId != null) {
                                            id = e.equipeId;
                                            break;
                                        }
                                retorno.accept(id);
                            }

                            @Override
                            public void onFailure(
                                    Call<List<ColetaApiService.EquipeDoCooperado>> call, Throwable t) {
                                retorno.accept(null);
                            }
                        });
    }
}
