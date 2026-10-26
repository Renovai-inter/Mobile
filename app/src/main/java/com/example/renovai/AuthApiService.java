package com.example.renovai;

import com.example.renovai.dto.request.CadastroCooperativaRequest;
import com.example.renovai.dto.request.CadastroEmpresaRequest;
import com.example.renovai.dto.request.LoginRequest;
import com.example.renovai.dto.request.EsqueciSenhaRequest;
import com.example.renovai.dto.request.GoogleLoginRequest;
import com.example.renovai.dto.request.RedefinirSenhaRequest;
import com.example.renovai.dto.request.VerificarCodigoRequest;
import com.example.renovai.dto.response.CadastroEmpresaResponse;
import com.example.renovai.dto.response.LoginResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApiService {

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    /** Login com Google: a API confere o ID token no Google e devolve o mesmo login. */
    @POST("auth/google")
    Call<LoginResponse> loginGoogle(@Body GoogleLoginRequest request);

    @POST("auth/cadastro")
    Call<CadastroEmpresaResponse> cadastroEmpresa(@Body CadastroEmpresaRequest request);

    /** Não devolve token: depois do cadastro a pessoa entra pelo login. */
    @POST("auth/cadastro-cooperativa")
    Call<Void> cadastroCooperativa(@Body CadastroCooperativaRequest request);

    @POST("auth/esqueci-senha")
    Call<Void> esqueciSenha(@Body EsqueciSenhaRequest request);

    /** Confere o código de 6 dígitos sem gastá-lo (204 = ok, 422 = errado/expirado). */
    @POST("auth/verificar-codigo")
    Call<Void> verificarCodigo(@Body VerificarCodigoRequest request);

    @POST("auth/redefinir-senha")
    Call<Void> redefinirSenha(@Body RedefinirSenhaRequest request);
}
