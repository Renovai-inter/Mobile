package com.example.renovai;

import com.example.renovai.dto.request.ColetaRequest;
import com.example.renovai.dto.response.ColetaResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Consome /coletas/** (com.renovai.api.controller.ColetaController).
 *
 * CORREÇÃO (área Cooperado): o método "listar" existente aqui usava
 * "coletas?cooperadoId=..." (query param), mas o endpoint real é
 * "coletas/por-cooperado/{cooperadoId}" (path param) — a versão antiga nunca
 * teria funcionado contra a API de verdade. Corrigido e estendido com o que as
 * telas de Cooperado (2.1, 2.2, 2.6) precisam.
 */
public interface ColetaApiService {

    @GET("coletas/por-cooperado/{cooperadoId}")
    Call<List<ColetaResponse>> listarPorCooperado(@Path("cooperadoId") String cooperadoId);

    @GET("coletas/{id}")
    Call<ColetaResponse> buscarPorId(@Path("id") String id);

    @POST("coletas")
    Call<ColetaResponse> criar(@Body ColetaRequest request);
}
