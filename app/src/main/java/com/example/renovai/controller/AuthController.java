package com.example.renovai.controller;

import com.example.renovai.ApiClient;
import com.example.renovai.AuthApiService;
import com.example.renovai.SessionManager;
import com.example.renovai.dto.request.LoginRequest;
import com.example.renovai.dto.response.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Controller (camada C do MVC) responsável pela autenticação do usuário.
 * Isola a View (LoginActivity) de qualquer detalhe de rede/Retrofit: a
 * Activity só chama login(...) e reage ao resultado via LoginCallback.
 *
 * Espelha o fluxo de POST /auth/login do backend (AuthApiService).
 */
public class AuthController {

    /** Contrato que a View implementa para reagir ao resultado do login. */
    public interface LoginCallback {
        void onSuccess(LoginResponse usuario);
        void onErro(String mensagem);
    }

    private final AuthApiService authApiService;

    public AuthController() {
        this.authApiService = ApiClient.createService(AuthApiService.class);
    }

    /**
     * Executa o login de forma assíncrona. Em caso de sucesso, já salva a
     * sessão (token/email/role) via SessionManager antes de notificar a
     * View — assim, na volta do callback, todas as próximas chamadas à API
     * já saem autenticadas automaticamente (ver AuthInterceptor).
     */
    public void login(String email, String senha, LoginCallback callback) {
        if (email == null || email.trim().isEmpty() || senha == null || senha.trim().isEmpty()) {
            callback.onErro("Preencha email e senha.");
            return;
        }

        LoginRequest request = new LoginRequest(email.trim(), senha);

        authApiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse body = response.body();
                    SessionManager.salvarSessao(body.getToken(), body.getEmail(), body.getRole());
                    callback.onSuccess(body);
                } else if (response.code() == 401) {
                    callback.onErro("Email ou senha incorretos.");
                } else {
                    callback.onErro("Erro ao entrar (código " + response.code() + "). Tente novamente.");
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                callback.onErro("Não foi possível conectar ao servidor. Verifique sua internet.");
            }
        });
    }
}
