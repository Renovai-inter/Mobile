package com.example.renovai;

import com.example.renovai.dto.response.MaterialResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/** Espelha MaterialController do backend (rota base: /materiais). */
public interface MaterialApiService {

    @GET("materiais")
    Call<List<MaterialResponse>> listar();

    @GET("materiais/disponiveis")
    Call<List<MaterialResponse>> listarDisponiveis();

    @GET("materiais/por-categoria")
    Call<List<MaterialResponse>> buscarPorCategoria(@Query("categoriaId") String categoriaId);
}
