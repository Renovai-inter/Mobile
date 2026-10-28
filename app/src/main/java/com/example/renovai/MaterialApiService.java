package com.example.renovai;

import com.example.renovai.dto.request.MaterialRequest;
import com.example.renovai.dto.response.CategoriaMaterialResponse;
import com.example.renovai.dto.response.MaterialResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface MaterialApiService {

    @GET("materiais")
    Call<List<MaterialResponse>> listar();

    @GET("materiais/disponiveis")
    Call<List<MaterialResponse>> listarDisponiveis();

    @GET("materiais/por-categoria")
    Call<List<MaterialResponse>> buscarPorCategoria(@Query("categoriaId") String categoriaId);

    /** Cadastro de material da cooperativa (tela Cadastrar material). */
    @POST("materiais")
    Call<MaterialResponse> criar(@Body MaterialRequest request);

    @GET("categorias-material")
    Call<List<CategoriaMaterialResponse>> categorias();
}
