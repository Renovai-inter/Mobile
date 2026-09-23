package com.example.renovai;

import com.example.renovai.dto.response.EmpresaResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface EmpresaApiService {

    @GET("empresas/{id}")
    Call<EmpresaResponse> buscarPorId(@Path("id") String id);
}
