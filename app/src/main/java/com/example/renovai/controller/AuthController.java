package com.example.renovai.controller;

import com.example.renovai.ApiClient;
import com.example.renovai.Atividade;
import com.example.renovai.AuthApiService;
import com.example.renovai.CooperadoSession;
import com.example.renovai.Documento;
import com.example.renovai.FirebaseCache;
import com.example.renovai.FuncionarioResolver;
import com.example.renovai.PerfilResolver;
import com.example.renovai.SessionManager;
import com.example.renovai.dto.request.CadastroCooperativaRequest;
import com.example.renovai.dto.request.CadastroEmpresaRequest;
import com.example.renovai.dto.request.GoogleLoginRequest;
import com.example.renovai.dto.request.LoginRequest;
import com.example.renovai.dto.response.CadastroEmpresaResponse;
import com.example.renovai.dto.response.FuncionarioResponse;
import com.example.renovai.dto.response.LoginResponse;

import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

public class AuthController {

    private static final String TAG = "AuthController";

    public interface LoginCallback {
        void onSuccess(LoginResponse usuario);

        void onErro(String mensagem);
    }

    public interface CadastroErroCallback {
        /** Erro de digitação ou parecido: a tela mostra um Toast. */
        void onErro(String mensagem);

        /** Erro que precisa de explicação (conta existente, servidor, conexão): pop-up. */
        default void onFalha(FalhaCadastro falha) {
            onErro(falha.mensagem);
        }
    }

    public interface CadastroEmpresaCallback extends CadastroErroCallback {
        void onSuccess(CadastroEmpresaResponse resposta);
    }

    public interface CadastroCooperativaCallback extends CadastroErroCallback {
        void onSuccess();
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

        // A API salva os e-mails em minúsculas e busca no login pelo texto exato
        LoginRequest request = new LoginRequest(email.trim().toLowerCase(java.util.Locale.ROOT), senha);

        authApiService.login(request).enqueue(aoResponderLogin(callback, false));
    }

    /**
     * Login com Google: o app já recebeu o ID token do Google (LoginActivity) e a API o confere no
     * Google. Só entra quem já tem conta no Renovaí com o mesmo e-mail — a API não cria conta.
     */
    public void loginGoogle(String idToken, LoginCallback callback) {
        if (idToken == null || idToken.isEmpty()) {
            callback.onErro("O Google não devolveu o login. Tente de novo.");
            return;
        }
        authApiService
                .loginGoogle(new GoogleLoginRequest(idToken))
                .enqueue(aoResponderLogin(callback, true));
    }

    /**
     * Mesmo tratamento para o login por senha e pelo Google: guarda a sessão, resolve a empresa
     * ou o cooperado e salva a cópia da conta no Firestore.
     */
    private retrofit2.Callback<LoginResponse> aoResponderLogin(
            LoginCallback callback, boolean google) {
        return new retrofit2.Callback<LoginResponse>() {
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

                                // Fora o login de Perfil da Empresa
                                // ("GESTOR_EMPRESA"), body.getUsuarioId() é um
                                // Usuario.usuarioId (cooperado, motorista, gestor
                                // ou admin de cooperativa — o cadastro de
                                // cooperativa cria o admin como Funcionario com
                                // cargo "Admin"), e dá pra resolver o Funcionario
                                // e cacheá-lo em CooperadoSession (a área do
                                // Gestor usa a cooperativa dele) — ver
                                // FuncionarioResolver. Contas antigas de admin que
                                // entram por Perfil devolvem o perfilId: aí o
                                // resolver só não acha nada.
                                boolean provavelmenteFuncionario =
                                        !"GESTOR_EMPRESA"
                                                .equalsIgnoreCase(body.getRole());

                                if (provavelmenteFuncionario
                                        && body.getUsuarioId() != null) {
                                    CooperadoSession.salvarUsuarioId(
                                            body.getUsuarioId());

                                    FuncionarioResolver.resolverPorUsuarioId(
                                            body.getUsuarioId(),
                                            funcionario -> {
                                                if (funcionario != null) {
                                                    salvarCooperadoSeEncontrado(
                                                            funcionario);
                                                    salvarCopiaDaConta(
                                                            body.getUsuarioId(),
                                                            funcionario);
                                                    entrou(callback, body);
                                                    return;
                                                }
                                                // A API não achou (ou falhou):
                                                // usa a cópia do último login
                                                // salva no Firestore, se houver.
                                                restaurarDaCopiaDaConta(
                                                        body.getEmail(),
                                                        body.getUsuarioId(),
                                                        () -> entrou(callback, body));
                                            });
                                } else {
                                    salvarCopiaDaConta(body.getUsuarioId(), null);
                                    entrou(callback, body);
                                }
                            });
                } else if (google) {
                    callback.onErro(erroGoogle(response));
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
        };
    }

    /** Login concluído (empresa ou funcionário já resolvidos): marca o dia no DAU e libera a tela. */
    private static void entrou(LoginCallback callback, LoginResponse body) {
        Atividade.registrarLogin();
        callback.onSuccess(body);
    }

    /** Mensagem do erro do login com Google (a API manda o motivo em "mensagem"). */
    private static String erroGoogle(Response<?> response) {
        if (response.code() == 404 || response.code() >= 500) {
            // 404/500: a versão da API no servidor ainda não tem a rota /auth/google
            return "O login com Google ainda não está disponível no servidor. Entre com e-mail e"
                    + " senha.";
        }
        try {
            if (response.errorBody() != null) {
                JsonObject json = JsonParser.parseString(response.errorBody().string()).getAsJsonObject();
                if (json.has("mensagem") && json.get("mensagem").isJsonPrimitive()) {
                    return json.get("mensagem").getAsString();
                }
            }
        } catch (Exception ignorado) {
            // corpo fora do formato da API: usa a mensagem genérica
        }
        return "Não foi possível entrar com o Google (código " + response.code() + ").";
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

    /**
     * Cópia da conta logada no Firestore (ver FirebaseCache.salvarConta). Sem token nem senha.
     * Usa só os dados deste login (funcionario pode ser null), nunca sobras de outra conta.
     */
    private static void salvarCopiaDaConta(String usuarioId, FuncionarioResponse funcionario) {
        Map<String, String> dados = new HashMap<>();
        dados.put("role", SessionManager.getRole());
        dados.put("usuarioId", usuarioId);
        dados.put("empresaId", SessionManager.getEmpresaId());
        if (funcionario != null) {
            dados.put("funcionarioId", funcionario.getFuncionarioId());
            dados.put("cooperativaId", funcionario.getCooperativaId());
            dados.put("cooperativaNome", funcionario.getCooperativaNome());
            dados.put("cargo", funcionario.getCargo());
            dados.put("nome", funcionario.getUsuarioNome());
        }
        FirebaseCache.salvarConta(SessionManager.getEmail(), dados);
    }

    /**
     * Preenche o CooperadoSession com a cópia do Firestore quando a API não devolveu o
     * Funcionario. Só vale se a cópia for do mesmo usuarioId (nunca mistura contas).
     */
    private static void restaurarDaCopiaDaConta(String email, String usuarioId, Runnable depois) {
        if (!FirebaseCache.ativo()) {
            depois.run();
            return;
        }
        FirebaseCache.lerConta(
                email,
                copia -> {
                    if (copia.get("funcionarioId") != null
                            && usuarioId.equals(copia.get("usuarioId"))) {
                        Log.i(TAG, "Funcionario restaurado da cópia da conta no Firestore");
                        CooperadoSession.salvar(
                                copia.get("funcionarioId"),
                                copia.get("cooperativaId"),
                                copia.get("cooperativaNome"),
                                copia.get("cargo"),
                                copia.get("nome"));
                    }
                    depois.run();
                });
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
        String erro =
                validarCadastro(
                        nome, cpf, email, telefone, nomeEmpresa, "empresa", cnpj, senha,
                        confirmacaoSenha);
        if (erro == null && (endereco == null || endereco.trim().isEmpty())) {
            erro = "Preencha o endereço da empresa.";
        }
        if (erro != null) {
            callback.onErro(erro);
            return;
        }
        CadastroEmpresaRequest request =
                new CadastroEmpresaRequest(
                        nome.trim(),
                        Documento.numeros(cpf),
                        email.trim(),
                        telefone.trim(),
                        senha,
                        nomeEmpresa.trim(),
                        Documento.numeros(cnpj),
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
                                    SessionManager.salvarEmpresaId(body.getEmpresaId());
                                    CooperadoSession.limpar();
                                    salvarCopiaDaConta(null, null);
                                    Atividade.registrarLogin();
                                    callback.onSuccess(body);
                                } else {
                                    tratarErroCadastro(response, "empresa", email.trim(), callback);
                                }
                            }

                            @Override
                            public void onFailure(Call<CadastroEmpresaResponse> call, Throwable t) {
                                Log.w(TAG, "Cadastro de empresa sem resposta", t);
                                callback.onFalha(FalhaCadastro.conexao());
                            }
                        });
    }

    /**
     * Cadastro de cooperativa (POST /auth/cadastro-cooperativa). Cria a cooperativa e o
     * administrador dela; a API não devolve token, então depois a pessoa entra pelo login.
     */
    public void cadastrarCooperativa(
            String nomeResponsavel,
            String cpf,
            String email,
            String contatoPreferencial, // WHATSAPP, TELEFONE ou EMAIL (CHECK do banco)
            String nomeCooperativa,
            String cnpj,
            String cep,
            String logradouro,
            String numero,
            String complemento,
            String bairro,
            String cidade,
            String senha,
            String confirmacaoSenha,
            CadastroCooperativaCallback callback) {
        String erro =
                validarCadastro(
                        nomeResponsavel, cpf, email, contatoPreferencial, nomeCooperativa, "cooperativa",
                        cnpj, senha, confirmacaoSenha);
        if (erro == null && Documento.numeros(cep).length() != 8) erro = "Informe o CEP com 8 números.";
        if (erro == null && vazio(logradouro)) erro = "Preencha a rua da cooperativa.";
        if (erro == null && vazio(numero)) erro = "Preencha o número do endereço.";
        if (erro == null && vazio(bairro)) erro = "Preencha o bairro.";
        if (erro == null && vazio(cidade)) erro = "Preencha a cidade.";
        if (erro != null) {
            callback.onErro(erro);
            return;
        }

        CadastroCooperativaRequest request =
                new CadastroCooperativaRequest(
                        nomeResponsavel.trim(),
                        Documento.formatarCpf(cpf),
                        nomeCooperativa.trim(),
                        contatoPreferencial,
                        email.trim(),
                        Documento.formatarCnpj(cnpj),
                        senha,
                        new CadastroCooperativaRequest.Endereco(
                                Documento.formatarCep(cep),
                                logradouro.trim(),
                                numero.trim(),
                                vazio(complemento) ? null : complemento.trim(),
                                bairro.trim(),
                                cidade.trim()));

        authApiService
                .cadastroCooperativa(request)
                .enqueue(
                        new retrofit2.Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    callback.onSuccess();
                                } else {
                                    tratarErroCadastro(response, "cooperativa", email.trim(), callback);
                                }
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Log.w(TAG, "Cadastro de cooperativa sem resposta", t);
                                callback.onFalha(FalhaCadastro.conexao());
                            }
                        });
    }

    /**
     * Traduz a resposta de erro da API. Campo mal preenchido (400, "Documento inválido.") vira
     * Toast; conta/dado já existente, 409, 5xx e o resto viram pop-up com explicação.
     */
    private static void tratarErroCadastro(
            Response<?> response, String tipo, String email, CadastroErroCallback callback) {
        String corpo = null;
        try {
            if (response.errorBody() != null) corpo = response.errorBody().string();
        } catch (java.io.IOException ignored) {
        }
        Log.w(TAG, "Cadastro de " + tipo + " recusado: HTTP " + response.code() + " " + corpo);

        String mensagem = null;
        JsonObject campos = null;
        try {
            JsonObject json = JsonParser.parseString(corpo).getAsJsonObject();
            if (json.has("mensagem") && json.get("mensagem").isJsonPrimitive()) {
                mensagem = json.get("mensagem").getAsString();
            }
            if (json.has("campos") && json.get("campos").isJsonObject()) {
                campos = json.getAsJsonObject("campos");
            }
        } catch (Exception ignored) {
            // corpo vazio ou que não é JSON (ex: página de erro do Render)
        }
        String m = mensagem == null ? "" : mensagem.toLowerCase(java.util.Locale.ROOT);
        int codigo = response.code();

        if (codigo == 400 && campos != null && campos.size() > 0) {
            StringBuilder nomes = new StringBuilder();
            for (String campo : campos.keySet()) {
                if (nomes.length() > 0) nomes.append(", ");
                nomes.append(nomeDoCampo(campo));
            }
            callback.onErro("Confira o preenchimento de: " + nomes + ".");
        } else if (codigo == 400 || m.contains("documento inválido") || m.contains("72 bytes")) {
            callback.onErro(mensagem != null ? mensagem : "Confira os dados preenchidos.");
        } else if (m.contains("e-mail já cadastrado")) {
            callback.onFalha(
                    new FalhaCadastro(
                            FalhaCadastro.Tipo.CONTA_EXISTENTE,
                            "Este e-mail já tem conta",
                            "Já existe uma conta no Renovai com o e-mail " + email + ".",
                            "Se a conta é sua, entre pelo login. Não lembra a senha? Use"
                                    + " \"Esqueceu a senha?\" na tela de login."));
        } else if (m.contains("cpf já cadastrado")) {
            callback.onFalha(
                    new FalhaCadastro(
                            FalhaCadastro.Tipo.DADOS_EM_USO,
                            "CPF já cadastrado",
                            "Este CPF já é responsável por outra conta no Renovai. Cada CPF só pode"
                                    + " estar ligado a uma conta.",
                            "Se a conta é sua, entre pelo login. Se não, confira se o CPF foi"
                                    + " digitado certo."));
        } else if (m.contains("cnpj já cadastrado")) {
            callback.onFalha(
                    new FalhaCadastro(
                            FalhaCadastro.Tipo.DADOS_EM_USO,
                            "Esta " + tipo + " já tem cadastro",
                            "O CNPJ informado já pertence a uma " + tipo + " cadastrada no Renovai.",
                            "Peça acesso a quem cuida da conta da " + tipo + ", ou confira se o CNPJ"
                                    + " foi digitado certo."));
        } else if (codigo == 409 || codigo >= 500) {
            // 409 = o banco recusou gravar (DataIntegrityViolationException na API). As checagens
            // de e-mail/CPF/CNPJ duplicado já passaram nesse ponto, então não é culpa dos dados.
            callback.onFalha(
                    new FalhaCadastro(
                            FalhaCadastro.Tipo.SERVIDOR,
                            "Problema no servidor",
                            "O servidor não conseguiu salvar o cadastro (erro " + codigo + "). Não"
                                    + " é um problema nos dados que você digitou, e nada foi salvo:"
                                    + " sua conta ainda não existe.",
                            "Tente de novo em alguns minutos. Se continuar, avise a equipe do"
                                    + " Renovai."));
        } else {
            callback.onFalha(
                    new FalhaCadastro(
                            FalhaCadastro.Tipo.OUTRO,
                            "Não foi possível cadastrar",
                            mensagem != null && !mensagem.isEmpty()
                                    ? mensagem
                                    : "O servidor recusou o cadastro (erro " + codigo + ").",
                            null));
        }
    }

    /** Nome legível dos campos que a API devolve em "campos" no erro 400. */
    private static String nomeDoCampo(String campo) {
        switch (campo) {
            case "nome": case "nomeResponsavel": return "nome";
            case "cpf": case "cpfResponsavel": return "CPF";
            case "email": return "e-mail";
            case "telefone": case "contatoPreferencial": return "telefone";
            case "nomeEmpresa": return "nome da empresa";
            case "cnpj": return "CNPJ";
            case "senha": return "senha";
            case "endereco": return "endereço";
            case "endereco.cep": return "CEP";
            case "endereco.logradouro": return "rua";
            case "endereco.numero": return "número";
            case "endereco.complemento": return "complemento";
            case "endereco.bairro": return "bairro";
            case "endereco.cidade": return "cidade";
            default: return campo;
        }
    }

    /** Campos comuns aos cadastros de empresa e cooperativa. Devolve a mensagem de erro ou null. */
    private static String validarCadastro(
            String nome,
            String cpf,
            String email,
            String telefone,
            String nomeOrganizacao,
            String tipo,
            String cnpj,
            String senha,
            String confirmacaoSenha) {
        if (vazio(nome)) return "Preencha o nome.";
        if (!Documento.cpfValido(cpf)) {
            return "CPF do responsável inválido: confira os 11 números (os dois últimos são dígitos"
                    + " verificadores).";
        }
        if (vazio(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return "Informe um e-mail válido.";
        }
        if (vazio(telefone)) return "Preencha o telefone.";
        if (vazio(nomeOrganizacao)) return "Preencha o nome da " + tipo + ".";
        if (!Documento.cnpjValido(cnpj)) {
            return "CNPJ da " + tipo + " inválido: confira os 14 números (os dois últimos são"
                    + " dígitos verificadores).";
        }
        if (senha == null || senha.length() < 6
                || senha.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            return "A senha deve ter ao menos 6 caracteres e até 72 bytes.";
        }
        if (!senha.equals(confirmacaoSenha)) {
            return "As senhas não coincidem, o campo confirmar senha deve ser igual ao de senha";
        }
        return null;
    }

    private static boolean vazio(String s) {
        return s == null || s.trim().isEmpty();
    }
}
