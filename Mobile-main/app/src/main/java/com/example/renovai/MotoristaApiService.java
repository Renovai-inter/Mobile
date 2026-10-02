package com.example.renovai;

import com.example.renovai.dto.response.GestorResponses;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

import java.util.List;

/**
 * Endpoints usados pela área do Motorista (telas 3.1 a 3.3). Reaproveita os mesmos DTOs já escritos
 * para o Gestor (GestorResponses.Rota/RotaEnd — mesmíssimo shape de Responses.RotaResponse no
 * backend), já que rota é a mesma entidade nas duas áreas.
 */
public interface MotoristaApiService {

    @GET("rotas/por-cooperativa/{id}")
    Call<List<GestorResponses.Rota>> rotas(@Path("id") String cooperativaId);

    @GET("rotas/{id}")
    Call<GestorResponses.Rota> rota(@Path("id") String rotaId);
}
