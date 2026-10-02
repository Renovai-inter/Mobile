package com.example.renovai.view;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

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

        EdgeToEdge.enable(this);

        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightStatusBars(false);

        getWindow().setStatusBarColor(Color.parseColor("#882B4E"));

        setContentView(R.layout.activity_login);


        // Instanciando os elementos
        TextView txtCadastre = findViewById(R.id.txtCadastre);
        TextView txtEsqueceuSenha = findViewById(R.id.txtEsqueceuSenha);
        Button btnEntrar = findViewById(R.id.btnEntrar);
        TextView edtEmail = findViewById(R.id.edtEmail);
        TextView edtSenha = findViewById(R.id.edtSenha);

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
        AuthController authController = new AuthController();

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

            authController.login(email, senha, new AuthController.LoginCallback() {
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
                        btnEntrar.setEnabled(true);
                        btnEntrar.setText(textoOriginalBtnEntrar);
                        Toast.makeText(LoginActivity.this, mensagem, Toast.LENGTH_LONG).show();
                    });
                }
            });
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
            if (r.contains("GESTOR")) {
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
