package com.example.renovai.controller;

import com.example.renovai.ApiClient;
import com.example.renovai.AuthApiService;
import com.example.renovai.CooperadoSession;
import com.example.renovai.FuncionarioResolver;
import com.example.renovai.PerfilResolver;
import com.example.renovai.SessionManager;
import com.example.renovai.dto.request.CadastroEmpresaRequest;
import com.example.renovai.dto.request.LoginRequest;
import com.example.renovai.dto.response.CadastroEmpresaResponse;
import com.example.renovai.dto.response.FuncionarioResponse;
import com.example.renovai.dto.response.LoginResponse;

import retrofit2.Call;
import retrofit2.Response;

public class AuthController {

    public interface LoginCallback {
        void onSuccess(LoginResponse usuario);

        void onErro(String mensagem);
    }

    public interface CadastroEmpresaCallback {
        void onSuccess(CadastroEmpresaResponse resposta);

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

        authApiService
                .login(request)
                .enqueue(
                        new retrofit2.Callback<LoginResponse>() {
                            @Override
                            public void onResponse(
                                    Call<LoginResponse> call, Response<LoginResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    LoginResponse body = response.body();
                                    SessionManager.salvarSessao(
                                            body.getToken(), body.getEmail(), body.getRole());

                                    // A API não devolve empresaId no /auth/login, então resolvemos
                                    // à
                                    // parte via GET /perfis (ver PerfilResolver). Não bloqueia o
                                    // sucesso do login: se não achar (ex: conta não é de empresa),
                                    // simplesmente fica null.
                                    PerfilResolver.resolverEmpresaId(
                                            body.getEmail(),
                                            empresaId -> {
                                                if ("GESTOR_EMPRESA"
                                                                .equalsIgnoreCase(body.getRole())
                                                        && empresaId == null) {
                                                    SessionManager.logout();
                                                    callback.onErro(
                                                            "Não foi possível carregar a conta da"
                                                                + " empresa. Tente entrar"
                                                                + " novamente.");
                                                    return;
                                                }
                                                SessionManager.salvarEmpresaId(empresaId);

                                                // ÁREA COOPERADO: se o login não for de Perfil
                                                // (Empresa ou
                                                // admin de Cooperativa — roles fixos
                                                // "GESTOR_EMPRESA" e
                                                // "ADMIN_COOPERATIVA", ver AuthService.login() no
                                                // backend),
                                                // então body.getUsuarioId() é de fato um
                                                // Usuario.usuarioId
                                                // (cooperado/motorista/gestor de cooperativa), e dá
                                                // pra
                                                // resolver o Funcionario correspondente e cacheá-lo
                                                // em
                                                // CooperadoSession — ver FuncionarioResolver.
                                                boolean provavelmenteFuncionario =
                                                        body.getRole() == null
                                                                || (!"GESTOR_EMPRESA"
                                                                                .equalsIgnoreCase(
                                                                                        body
                                                                                                .getRole())
                                                                        && !"ADMIN_COOPERATIVA"
                                                                                .equalsIgnoreCase(
                                                                                        body
                                                                                                .getRole()));

                                                if (provavelmenteFuncionario
                                                        && body.getUsuarioId() != null) {
                                                    CooperadoSession.salvarUsuarioId(
                                                            body.getUsuarioId());

                                                    FuncionarioResolver.resolverPorUsuarioId(
                                                            body.getUsuarioId(),
                                                            funcionario -> {
                                                                salvarCooperadoSeEncontrado(
                                                                        funcionario);
                                                                callback.onSuccess(body);
                                                            });
                                                } else {
                                                    callback.onSuccess(body);
                                                }
                                            });
                                } else if (response.code() == 401 || response.code() == 422) {
                                    callback.onErro("Email ou senha incorretos.");
                                } else {
                                    callback.onErro(
                                            "Erro ao entrar (código "
                                                    + response.code()
                                                    + "). Tente novamente.");
                                }
                            }

                            @Override
                            public void onFailure(Call<LoginResponse> call, Throwable t) {
                                callback.onErro(
                                        "Não foi possível conectar ao servidor. Verifique sua"
                                            + " internet.");
                            }
                        });
    }

    private void salvarCooperadoSeEncontrado(FuncionarioResponse funcionario) {
        if (funcionario == null) return; // conta não é de um Funcionario — não é erro
        CooperadoSession.salvar(
                funcionario.getFuncionarioId(),
                funcionario.getCooperativaId(),
                funcionario.getCooperativaNome(),
                funcionario.getCargo(),
                funcionario.getUsuarioNome());
    }

    public void cadastrarEmpresa(
            String nome,
            String cpf,
            String email,
            String telefone,
            String nomeEmpresa,
            String cnpj,
            String endereco,
            String senha,
            String confirmacaoSenha,
            CadastroEmpresaCallback callback) {
        if (nome == null || nome.trim().isEmpty()) {
            callback.onErro("Preencha o nome.");
            return;
        }

        if (email == null || email.trim().isEmpty()) {
            callback.onErro("Preencha o email.");
            return;
        }

        if (telefone == null || telefone.trim().isEmpty()) {
            callback.onErro("Preencha o telefone.");
            return;
        }

        if (nomeEmpresa == null || nomeEmpresa.trim().isEmpty()) {
            callback.onErro("Preencha o nome da empresa.");
            return;
        }

        if (cnpj == null || cnpj.trim().isEmpty()) {
            callback.onErro("Preencha o cnpj.");
            return;
        }

        if (endereco == null || endereco.trim().isEmpty()) {
            callback.onErro("Preencha o endereço da empresa.");
            return;
        }

        if (senha == null || senha.trim().isEmpty()) {
            callback.onErro("Preencha a senha");
            return;
        }

        if (confirmacaoSenha == null || confirmacaoSenha.trim().isEmpty()) {
            callback.onErro("Preencha o campo confirmar senha'");
            return;
        }

        if (!confirmacaoSenha.equals(senha)) {
            callback.onErro(
                    "As senhas não coincidem, o campo confirmar senha deve ser igual ao de senha");
            return;
        }

        if (cpf == null || !cpf.replaceAll("\\D", "").matches("[0-9]{11}")) {
            callback.onErro("Informe o CPF do responsável com 11 dígitos.");
            return;
        }
        if (senha.length() < 6
                || senha.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            callback.onErro("A senha deve ter ao menos 6 caracteres e até 72 bytes.");
            return;
        }
        CadastroEmpresaRequest request =
                new CadastroEmpresaRequest(
                        nome.trim(),
                        cpf.trim(),
                        email.trim(),
                        telefone.trim(),
                        senha,
                        nomeEmpresa.trim(),
                        cnpj.trim(),
                        endereco.trim());

        authApiService
                .cadastroEmpresa(request)
                .enqueue(
                        new retrofit2.Callback<CadastroEmpresaResponse>() {
                            @Override
                            public void onResponse(
                                    Call<CadastroEmpresaResponse> call,
                                    Response<CadastroEmpresaResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    CadastroEmpresaResponse body = response.body();
                                    SessionManager.salvarSessao(
                                            body.getToken(), body.getEmail(), body.getRole());
                                    callback.onSuccess(body);
                                } else if (response.code() == 422) {
                                    callback.onErro(com.example.renovai.EmpresaData.erro(response));
                                } else if (response.code() == 400) {
                                    callback.onErro(com.example.renovai.EmpresaData.erro(response));
                                } else {
                                    callback.onErro(
                                            "Erro ao cadastrar (código "
                                                    + response.code()
                                                    + "). Tente novamente.");
                                }
                            }

                            @Override
                            public void onFailure(Call<CadastroEmpresaResponse> call, Throwable t) {
                                callback.onErro(
                                        "Não foi possível conectar ao servidor. Verifique sua"
                                            + " internet.");
                            }
                        });
    }
}
