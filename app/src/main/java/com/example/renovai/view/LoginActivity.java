package com.example.renovai.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.renovai.MainActivity;
import com.example.renovai.controller.AuthController;

import com.example.renovai.R;
import com.example.renovai.dto.response.LoginResponse;

import android.os.Handler;
import android.os.Looper;

import com.example.renovai.CooperadoPreload;
import com.example.renovai.CooperadoSession;
import com.example.renovai.GestorCache;
import com.example.renovai.GestorData;

import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.example.renovai.BuildConfig;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private final AuthController authController = new AuthController();

    private void abrirTela(Class<?> destino) {
        Intent intent = new Intent(LoginActivity.this, destino);
        startActivity(intent);
        finish();
    }

    private void esperarCooperadoResolvido(LoginResponse usuario) {
        Handler handler = new Handler(Looper.getMainLooper());

        Runnable verificar = new Runnable() {
            private int tentativas = 0;

            @Override
            public void run() {
                if (CooperadoSession.temCooperadoResolvido()) {

                    Toast.makeText(
                            LoginActivity.this,
                            "Preparando seus dados...",
                            Toast.LENGTH_SHORT
                    ).show();

                    CooperadoPreload.carregar(() -> {
                        runOnUiThread(() -> abrirTela(CooperadoHomeActivity.class));
                    });

                    return;
                }

                tentativas++;

                if (tentativas >= 100) {
                    abrirTela(CooperadoHomeActivity.class);
                    return;
                }

                handler.postDelayed(this, 100);
            }
        };

        handler.post(verificar);
    }

    private void esperarMotoristaResolvido(LoginResponse usuario) {
        Handler handler = new Handler(Looper.getMainLooper());

        Runnable verificar = new Runnable() {
            private int tentativas = 0;

            @Override
            public void run() {
                if (CooperadoSession.temCooperadoResolvido()) {
                    runOnUiThread(() -> abrirTela(com.example.renovai.view.MotoristaHomeActivity.class));
                    return;
                }
                tentativas++;
                if (tentativas >= 100) {
                    abrirTela(com.example.renovai.view.MotoristaHomeActivity.class);
                    return;
                }
                handler.postDelayed(this, 100);
            }
        };
        handler.post(verificar);
    }

    private void prepararEntrada(LoginResponse usuario) {
        Class<?> destino = destinoParaRole(usuario.getRole());

        if (destino != CooperadoHomeActivity.class) {
            abrirTela(destino);
            return;
        }

        esperarCooperadoResolvido(usuario);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightStatusBars(false);


        setContentView(R.layout.activity_login);


        // Instanciando os elementos
        TextView txtCadastre = findViewById(R.id.txtCadastre);
        TextView txtEsqueceuSenha = findViewById(R.id.txtEsqueceuSenha);
        Button btnEntrar = findViewById(R.id.btnEntrar);
        TextView edtEmail = findViewById(R.id.edtEmail);
        TextView edtSenha = findViewById(R.id.edtSenha);
        com.example.renovai.SenhaVisivel.ativar(findViewById(R.id.edtSenha));

        // Ir para a tela de recuperar senha (1.7 — pede o e-mail antes)
        txtEsqueceuSenha.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, EsqueciSenhaActivity.class);
            startActivity(intent);
        });

        // Ir para a tela de EscolhaCadastro
        txtCadastre.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, EscolheActivity.class);
            startActivity(intent);
        });

        // Entrar com Google: abre a escolha de conta do Google e manda o ID token para a API
        MaterialButton btnGoogle = findViewById(R.id.btnGoogle);
        btnGoogle.setOnClickListener(v -> entrarComGoogle(btnGoogle));

        btnEntrar.setOnClickListener(v -> {
            String email = edtEmail.getText().toString();
            String senha = edtSenha.getText().toString();

            // Trava o botão durante a chamada — sem isso, como a API pode
            // demorar (cold start do Render), é fácil tocar duas vezes achando
            // que não aconteceu nada, e gerar duas requisições penduradas.
            CharSequence textoOriginalBtnEntrar = btnEntrar.getText();
            btnEntrar.setEnabled(false);
            btnEntrar.setText("Entrando...");
            Toast.makeText(LoginActivity.this,
                    "Conectando ao servidor — a primeira tentativa pode demorar um pouco.",
                    Toast.LENGTH_LONG).show();

            authController.login(email, senha, aoEntrar(() -> {
                btnEntrar.setEnabled(true);
                btnEntrar.setText(textoOriginalBtnEntrar);
            }));
        });


    }

    /**
     * Depois que a API aceitou o login (senha ou Google): abre a área certa para o perfil, com os
     * dados pré-carregados. {@code aoFalhar} devolve o botão ao normal quando dá erro.
     */
    private AuthController.LoginCallback aoEntrar(Runnable aoFalhar) {
        return new AuthController.LoginCallback() {
            @Override
            public void onSuccess(LoginResponse usuario) {
                runOnUiThread(() -> {

                    Class<?> destino = destinoParaRole(usuario.getRole());

                    // ÁREA GESTOR: limpa qualquer cache de outra conta e pré-carrega as
                    // listagens (mesma ideia do CooperadoPreload) antes de abrir a Home.
                    if (destino == GestorHomeActivity.class) {
                        GestorCache.limpar();
                        GestorData.precarregar(() -> runOnUiThread(() -> {
                            startActivity(new Intent(LoginActivity.this, GestorHomeActivity.class));
                            finish();
                        }));
                        return;
                    }

                    // ÁREA EMPRESA: mesma ideia — cache limpo e listagens (dashboard, pedidos,
                    // favoritos, meu perfil) pré-carregadas antes de abrir a Home.
                    if (destino == com.example.renovai.view.EmpresaHomeActivity.class) {
                        GestorCache.limpar();
                        com.example.renovai.EmpresaData.precarregar(() -> runOnUiThread(() -> {
                            startActivity(new Intent(LoginActivity.this, com.example.renovai.view.EmpresaHomeActivity.class));
                            finish();
                        }));
                        return;
                    }

                    // ÁREA MOTORISTA: reaproveita o CooperadoSession (role de Funcionario), então
                    // segue a mesma espera de "cooperado resolvido" antes de abrir a Home.
                    if (destino == com.example.renovai.view.MotoristaHomeActivity.class) {
                        esperarMotoristaResolvido(usuario);
                        return;
                    }

                    if (destino != CooperadoHomeActivity.class) {
                        Intent intent = new Intent(LoginActivity.this, destino);
                        startActivity(intent);
                        finish();
                        return;
                    }

                    CooperadoPreload.carregar(() -> {
                        runOnUiThread(() -> {
                            Intent intent = new Intent(
                                    LoginActivity.this,
                                    CooperadoHomeActivity.class
                            );

                            startActivity(intent);
                            finish();
                        });
                    });
                });
            }

            @Override
            public void onErro(String mensagem) {
                runOnUiThread(() -> {
                    aoFalhar.run();
                    Toast.makeText(LoginActivity.this, mensagem, Toast.LENGTH_LONG).show();
                });
            }
        };
    }

    /**
     * Login com Google pelo Credential Manager: o Google mostra as contas do celular e devolve um
     * ID token, que a API (POST /auth/google) confere. Precisa do "ID do cliente da Web" do
     * projeto Firebase em google.webClientId (local.properties).
     */
    private void entrarComGoogle(MaterialButton btn) {
        String clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID;
        if (clientId == null || clientId.isEmpty()) {
            Toast.makeText(this, "O login com Google ainda não foi configurado neste app.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        CharSequence textoOriginal = btn.getText();
        Runnable reativar = () -> {
            btn.setEnabled(true);
            btn.setText(textoOriginal);
        };
        btn.setEnabled(false);

        GetCredentialRequest pedido = new GetCredentialRequest.Builder()
                .addCredentialOption(new GetSignInWithGoogleOption.Builder(clientId).build())
                .build();
        CredentialManager.create(this).getCredentialAsync(
                this,
                pedido,
                null,
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse resposta) {
                        Credential credencial = resposta.getCredential();
                        String idToken = null;
                        if (credencial instanceof CustomCredential
                                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                        .equals(credencial.getType())) {
                            try {
                                idToken = GoogleIdTokenCredential.createFrom(credencial.getData())
                                        .getIdToken();
                            } catch (Exception e) {
                                Log.w(TAG, "Resposta do Google fora do formato esperado", e);
                            }
                        }
                        if (idToken == null) {
                            reativar.run();
                            Toast.makeText(LoginActivity.this,
                                    "O Google não devolveu o login. Tente de novo.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        btn.setText("Entrando...");
                        authController.loginGoogle(idToken, aoEntrar(reativar));
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        reativar.run();
                        if (e instanceof GetCredentialCancellationException) return; // fechou a janela
                        Log.w(TAG, "Login com Google falhou", e);
                        String mensagem = e instanceof NoCredentialException
                                ? "Nenhuma conta Google disponível neste celular. Adicione uma conta"
                                        + " nas configurações do Android e tente de novo."
                                : "Não foi possível abrir o login do Google. Tente de novo.";
                        Toast.makeText(LoginActivity.this, mensagem, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * O "role" de um login de Funcionario é o cargo cadastrado no banco (ver
     * AuthService.login() no backend, que usa funcionario.getCargo().getCargo()
     * como role) — por isso o casamento é por palavra-chave ("contém
     * COOPERADO"), não um valor fixo: os valores exatos de cargo cadastrados não
     * são conhecidos sem consultar o banco (ver IMPLEMENTACAO.md). Qualquer outro
     * role continua indo para MainActivity, exatamente como antes desta mudança.
     */
    private Class<?> destinoParaRole(String role) {
        // Gestor da cooperativa (cargo GESTOR_COOPERATIVA). "GESTOR_EMPRESA" é o login de Perfil
        // da empresa e continua indo para a área da Empresa (abaixo), não para o Gestor.
        if (role != null) {
            String r = role.toUpperCase(Locale.ROOT);
            if (r.contains("EMPRESA")) {
                return com.example.renovai.view.EmpresaHomeActivity.class;
            }
            // Admin da cooperativa (quem fez o cadastro da cooperativa) usa a mesma área do Gestor.
            if (r.contains("GESTOR") || r.equals("ADMIN_COOPERATIVA")) {
                return GestorHomeActivity.class;
            }
            if (r.contains("MOTORISTA")) {
                return com.example.renovai.view.MotoristaHomeActivity.class;
            }
        }
        if (role != null && role.toUpperCase(Locale.ROOT).contains("COOPERADO")) {
            return CooperadoHomeActivity.class;
        }
        return MainActivity.class;
    }
}
