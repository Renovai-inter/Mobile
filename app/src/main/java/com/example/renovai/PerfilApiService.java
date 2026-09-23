package com.example.renovai;

import com.example.renovai.dto.response.PerfilResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

/** Só consome GET /perfis, que já existe na API — nada foi criado no backend. */
public interface PerfilApiService {

    @GET("perfis")
    Call<List<PerfilResponse>> listar();
}
