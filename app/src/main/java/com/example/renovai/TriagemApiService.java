package com.example.renovai;

import com.example.renovai.dto.request.ConcluirTriagemRequest;
import com.example.renovai.dto.request.TriagemRequest;
import com.example.renovai.dto.response.TriagemResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * NÃO existia nenhuma interface para /triagens/** no projeto ainda — criada
 * seguindo exatamente com.renovai.api.controller.TriagemController (backend).
 * Cobre só o que as telas 2.1, 2.3 e 2.7 (Cooperado) usam.
 */
public interface TriagemApiService {

    @GET("triagens/abertas/por-cooperado/{cooperadoId}")
    Call<List<TriagemResponse>> listarAbertasPorCooperado(@Path("cooperadoId") String cooperadoId);

    @GET("triagens/por-cooperado/{cooperadoId}")
    Call<List<TriagemResponse>> listarPorCooperado(@Path("cooperadoId") String cooperadoId);

    @GET("triagens/por-coleta/{coletaId}")
    Call<List<TriagemResponse>> listarPorColeta(@Path("coletaId") String coletaId);

    @PUT("triagens/{id}")
    Call<TriagemResponse> atualizar(@Path("id") String id, @Body TriagemRequest request);

    @PATCH("triagens/{id}/concluir")
    Call<TriagemResponse> concluir(@Path("id") String id, @Body ConcluirTriagemRequest request);
}
