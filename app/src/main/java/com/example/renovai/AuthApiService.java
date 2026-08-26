package com.example.renovai;

import com.example.renovai.dto.request.LoginRequest;
import com.example.renovai.dto.response.LoginResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/** Espelha AuthController do backend (rota base: /auth). */
public interface AuthApiService {

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);
}
