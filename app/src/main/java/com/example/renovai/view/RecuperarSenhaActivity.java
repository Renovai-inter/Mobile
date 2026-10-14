package com.example.renovai.view;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.renovai.ApiClient;
import com.example.renovai.AuthApiService;
import com.example.renovai.EmpresaData;
import com.example.renovai.R;
import com.example.renovai.dto.request.EsqueciSenhaRequest;
import com.example.renovai.dto.request.RedefinirSenhaRequest;
import com.example.renovai.dto.request.VerificarCodigoRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tela 1.7.1 — Redefinir senha (etapas 2 e 3 de 3 da verificação em dois fatores):
 *
 * <ol>
 *   <li>a pessoa digita o código de 6 dígitos recebido por e-mail (2º fator) e a API confere
 *       (POST /auth/verificar-codigo);
 *   <li>só então aparecem os campos da nova senha (POST /auth/redefinir-senha).
 * </ol>
 *
 * O código vale 15 minutos e é invalidado após 5 tentativas erradas (regra da API).
 */
public class RecuperarSenhaActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "email";
    private static final long ESPERA_REENVIO_MS = 60_000L;

    private String email;
    private boolean codigoConfirmado;
    private CountDownTimer timerReenvio;

    private EditText edtCodigo;
    private View grupoNovaSenha;
    private TextView txtBtnAlterar;
    private TextView txtReenviar;
    private View progresso;
    private View btnAlterar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightStatusBars(false);

        getWindow().setStatusBarColor(Color.parseColor("#882B4E"));

        setContentView(R.layout.activity_recuperar_senha);

        email = getIntent().getStringExtra(EXTRA_EMAIL);
        if (email == null || email.trim().isEmpty()) {
            // Sem e-mail não há como conferir o código: volta para a etapa do e-mail.
            startActivity(new Intent(this, EsqueciSenhaActivity.class));
            finish();
            return;
        }

        edtCodigo = findViewById(R.id.edtToken);
        grupoNovaSenha = findViewById(R.id.grupoNovaSenha);
        txtBtnAlterar = findViewById(R.id.txtBtnAlterar);
        txtReenviar = findViewById(R.id.txtReenviar);
        progresso = findViewById(R.id.progressoAlterar);
        btnAlterar = findViewById(R.id.btnAlterar);

        EtapasRedefinicao.marcar(this, EtapasRedefinicao.CODIGO);
        ((TextView) findViewById(R.id.txtInfoCodigo))
                .setText("Digite o código de 6 dígitos que enviamos para " + email
                        + ". Ele chega de renovai.suporte@gmail.com e vale por 15 minutos.");

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        btnAlterar.setOnClickListener(v -> {
            if (codigoConfirmado) alterar(); else verificarCodigo();
        });
        txtReenviar.setOnClickListener(v -> reenviar());
        iniciarEsperaReenvio();
    }

    @Override
    protected void onDestroy() {
        if (timerReenvio != null) timerReenvio.cancel();
        super.onDestroy();
    }

    private AuthApiService api() {
        return ApiClient.createService(AuthApiService.class);
    }

    private String codigo() {
        return edtCodigo.getText().toString().trim();
    }

    private void carregando(boolean sim) {
        progresso.setVisibility(sim ? View.VISIBLE : View.GONE);
        btnAlterar.setEnabled(!sim);
    }

    // ── etapa 1: conferir o código ──

    private void verificarCodigo() {
        String codigo = codigo();
        if (!codigo.matches("\\d{6}")) {
            Toast.makeText(this, "Digite os 6 números do código.", Toast.LENGTH_LONG).show();
            return;
        }
        carregando(true);
        api().verificarCodigo(new VerificarCodigoRequest(email, codigo)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> c, Response<Void> r) {
                carregando(false);
                if (r.isSuccessful()) {
                    codigoConfirmado = true;
                    edtCodigo.setEnabled(false);
                    txtReenviar.setVisibility(View.GONE);
                    grupoNovaSenha.setVisibility(View.VISIBLE);
                    EtapasRedefinicao.marcar(RecuperarSenhaActivity.this, EtapasRedefinicao.NOVA_SENHA);
                    ((TextView) findViewById(R.id.txtInfoCodigo))
                            .setText("Código confirmado. Agora crie sua nova senha (mínimo 6 caracteres).");
                    txtBtnAlterar.setText("Alterar senha");
                    findViewById(R.id.edtNovaSenha).requestFocus();
                } else {
                    Toast.makeText(RecuperarSenhaActivity.this, EmpresaData.erro(r), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> c, Throwable t) {
                carregando(false);
                Toast.makeText(RecuperarSenhaActivity.this, "Sem conexão com o servidor.", Toast.LENGTH_LONG).show();
            }
        });
    }

    // ── etapa 2: nova senha ──

    private void alterar() {
        String senha = ((EditText) findViewById(R.id.edtNovaSenha)).getText().toString();
        String confirmar = ((EditText) findViewById(R.id.edtConfirmarSenha)).getText().toString();

        if (senha.length() < 6) { Toast.makeText(this, "A senha deve ter no mínimo 6 caracteres.", Toast.LENGTH_LONG).show(); return; }
        if (!senha.equals(confirmar)) { Toast.makeText(this, "As senhas não coincidem.", Toast.LENGTH_LONG).show(); return; }

        carregando(true);
        api().redefinirSenha(new RedefinirSenhaRequest(email, codigo(), senha)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> c, Response<Void> r) {
                carregando(false);
                if (r.isSuccessful()) {
                    Toast.makeText(RecuperarSenhaActivity.this, "Senha alterada. Entre com a nova senha.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(RecuperarSenhaActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(RecuperarSenhaActivity.this, EmpresaData.erro(r), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> c, Throwable t) {
                carregando(false);
                Toast.makeText(RecuperarSenhaActivity.this, "Sem conexão com o servidor.", Toast.LENGTH_LONG).show();
            }
        });
    }

    // ── reenviar código (no máximo 1 por minuto) ──

    private void reenviar() {
        txtReenviar.setEnabled(false);
        api().esqueciSenha(new EsqueciSenhaRequest(email)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> c, Response<Void> r) {
                if (r.isSuccessful()) {
                    edtCodigo.setText("");
                    Toast.makeText(RecuperarSenhaActivity.this, "Enviamos um novo código. O anterior deixou de valer.", Toast.LENGTH_LONG).show();
                    iniciarEsperaReenvio();
                } else {
                    txtReenviar.setEnabled(true);
                    Toast.makeText(RecuperarSenhaActivity.this, EmpresaData.erro(r), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> c, Throwable t) {
                txtReenviar.setEnabled(true);
                Toast.makeText(RecuperarSenhaActivity.this, "Sem conexão com o servidor.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void iniciarEsperaReenvio() {
        if (timerReenvio != null) timerReenvio.cancel();
        txtReenviar.setEnabled(false);
        timerReenvio = new CountDownTimer(ESPERA_REENVIO_MS, 1000) {
            @Override
            public void onTick(long restante) {
                txtReenviar.setText("Reenviar código em " + (restante / 1000 + 1) + "s");
            }

            @Override
            public void onFinish() {
                txtReenviar.setText("Reenviar código");
                txtReenviar.setEnabled(true);
            }
        }.start();
    }
}
