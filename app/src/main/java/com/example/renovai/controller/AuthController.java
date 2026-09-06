package com.example.renovai.controller;

import com.example.renovai.ApiClient;
import com.example.renovai.AuthApiService;
import com.example.renovai.SessionManager;
import com.example.renovai.dto.request.LoginRequest;
import com.example.renovai.dto.response.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class AuthController {

    public interface LoginCallback {
        void onSuccess(LoginResponse usuario);
        void onErro(String mensagem);
    }

    private final AuthApiService authApiService;

    public AuthController() {
        this.authApiService = ApiClient.createService(AuthApiService.class);
    }


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
                } else if (response.code() == 401 || response.code() == 422) {
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

    public void cadastrarEmpresa(String nome, String email, String telefone, String nomeEmpresa, String cnpj, String endereco, String senha, String confirmacaoSenha, LoginCallback callback) {
        if (nome == null || nome.trim().isEmpty()){
            callback.onErro("Preencha o nome.");
            return;
        }

        if (email == null || email.trim().isEmpty()){
            callback.onErro("Preencha o email.");
            return;
        }

        if (telefone == null || telefone.trim().isEmpty()){
            callback.onErro("Preencha o telefone.");
            return;
        }

        if(nomeEmpresa == null || nomeEmpresa.trim().isEmpty()){
            callback.onErro("Preencha o nome da empresa.");
            return;
        }

        if(cnpj == null || cnpj.trim().isEmpty()){
            callback.onErro("Preencha o cnpj.");
            return;
        }

        if(endereco == null || endereco.trim().isEmpty()){
            callback.onErro("Preencha o nome da empresa.");
            return;
        }

        if(senha == null || senha.trim().isEmpty()){
            callback.onErro("Preencha a senha");
            return;
        }

        if(confirmacaoSenha == null || confirmacaoSenha.trim().isEmpty()){
            callback.onErro("Preencha o campo confirmar senha'");
            return;
        }

        if (!confirmacaoSenha.equals(senha)){
            callback.onErro("As senhas não coincidem, o campo confirmar senha deve ser igual ao de senha");
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
