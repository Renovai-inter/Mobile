package com.example.renovai;

import com.example.renovai.dto.response.ColetaResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/** Espelha ColetaController do backend (rota base: /coletas). */
public interface ColetaApiService {

    @GET("coletas")
    Call<List<ColetaResponse>> listar(@Query("cooperadoId") String cooperadoId);
}
