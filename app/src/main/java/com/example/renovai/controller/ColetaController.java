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
     * POST /coletas (tela 2.2).
     *
     * <p>Só envia os campos que a API realmente aceita (ColetaRequest). "Materiais coletados",
     * "necessita triagem?" e "data prevista" NÃO são enviados — o backend não tem onde guardar isso
     * hoje (ver IMPLEMENTACAO.md).
     */
    public void criar(
            String cooperadoId,
            BigDecimal quantidadeKg,
            String tipoColeta,
            String rotaId,
            String imagemUrl,
            CriarCallback callback) {
        if (cooperadoId == null || cooperadoId.trim().isEmpty()) {
            callback.onErro("Não foi possível identificar o cooperado logado.");
            return;
        }
        if (quantidadeKg == null || quantidadeKg.compareTo(new BigDecimal("0.001")) < 0) {
            callback.onErro("Informe o peso da coleta.");
            return;
        }
        boolean externa =
                "EXTERNA".equalsIgnoreCase(tipoColeta) || "EXTERNO".equalsIgnoreCase(tipoColeta);
        if (externa && (rotaId == null || rotaId.trim().isEmpty())) {
            callback.onErro("Selecione a rota de origem para coletas externas.");
            return;
        }

        ColetaRequest request =
                montarRequest(cooperadoId, quantidadeKg, tipoColeta, rotaId, imagemUrl);

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
                                    callback.onErro(
                                            "Erro ao registrar coleta (código "
                                                    + response.code()
                                                    + ").");
                                }
                            }

                            @Override
                            public void onFailure(Call<ColetaResponse> call, Throwable t) {
                                callback.onErro("Não foi possível conectar ao servidor.");
                            }
                        });
    }
}
