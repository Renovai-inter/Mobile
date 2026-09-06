package com.example.renovai;

import com.example.renovai.dto.response.EstoqueResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface EstoqueApiService {

    @GET("estoques")
    Call<List<EstoqueResponse>> listar(
            @Query("cooperativaId") String cooperativaId,
            @Query("apenasDisponiveis") Boolean apenasDisponiveis
    );
}
