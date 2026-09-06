package com.example.renovai;

import com.example.renovai.dto.request.CadastroEmpresaRequest;
import com.example.renovai.dto.request.LoginRequest;
import com.example.renovai.dto.response.CadastroEmpresaResponse;
import com.example.renovai.dto.response.LoginResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApiService {

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("auth/cadastro")
    Call<CadastroEmpresaResponse> cadastroEmpresa(@Body CadastroEmpresaRequest request);
}
