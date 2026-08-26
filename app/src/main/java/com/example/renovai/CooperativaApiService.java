package com.example.renovai;

import com.example.renovai.dto.request.CooperativaRequest;
import com.example.renovai.dto.response.CooperativaResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Espelha CooperativaController do backend (rota base: /cooperativas). */
public interface CooperativaApiService {

    @GET("cooperativas")
    Call<List<CooperativaResponse>> listar();

    @GET("cooperativas/{id}")
    Call<CooperativaResponse> buscarPorId(@Path("id") String id);

    @GET("cooperativas/buscar")
    Call<List<CooperativaResponse>> buscarComFiltros(
            @Query("categoriaId") String categoriaId,
            @Query("cidade") String cidade,
            @Query("quantidadeMin") String quantidadeMin
    );

    @POST("cooperativas")
    Call<CooperativaResponse> criar(@Body CooperativaRequest request);

    @PUT("cooperativas/{id}")
    Call<CooperativaResponse> atualizar(@Path("id") String id, @Body CooperativaRequest request);

    @DELETE("cooperativas/{id}")
    Call<Void> deletar(@Path("id") String id);
}
