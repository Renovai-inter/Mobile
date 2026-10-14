package com.example.renovai;

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

    /** Login com Google: a API confere o ID token e devolve o mesmo JWT do login por senha. */
    @POST("auth/google")
    Call<LoginResponse> loginGoogle(@Body GoogleLoginRequest request);

    @POST("auth/cadastro")
    Call<CadastroEmpresaResponse> cadastroEmpresa(@Body CadastroEmpresaRequest request);

    @POST("auth/esqueci-senha")
    Call<Void> esqueciSenha(@Body EsqueciSenhaRequest request);

    /** Confere o código de 6 dígitos enviado por e-mail, sem gastá-lo. */
    @POST("auth/verificar-codigo")
    Call<Void> verificarCodigo(@Body VerificarCodigoRequest request);

    @POST("auth/redefinir-senha")
    Call<Void> redefinirSenha(@Body RedefinirSenhaRequest request);
}
