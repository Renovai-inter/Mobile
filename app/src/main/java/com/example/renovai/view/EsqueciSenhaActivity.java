package com.example.renovai.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.renovai.ApiClient;
import com.example.renovai.AuthApiService;
import com.example.renovai.EmpresaData;
import com.example.renovai.R;
import com.example.renovai.dto.request.EsqueciSenhaRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tela 1.7 — Esqueci minha senha: pede o e-mail; a API envia um código de 6 dígitos para ele e o
 * fluxo segue para a 1.7.1 (CodigoSenhaActivity) e depois a 1.7.2 (RecuperarSenhaActivity).
 */
public class EsqueciSenhaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        setContentView(R.layout.activity_esqueci_senha);

        // Voltar para a tela anterior (o login continua aberto embaixo)
        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        findViewById(R.id.btnEnviar).setOnClickListener(v -> enviar());
        com.example.renovai.TelaComTeclado.ajustar(findViewById(R.id.telaEsqueci));
        ApiClient.acordarServidor();
    }

    private void mostrarErroEmail(String mensagem) {
        TextView erro = findViewById(R.id.txtErroEmail);
        erro.setText(mensagem);
        erro.setVisibility(mensagem == null ? View.GONE : View.VISIBLE);
    }

    private void enviar() {
        // A API guarda os e-mails em minúsculas
        String email = ((EditText) findViewById(R.id.edtEmail)).getText().toString().trim().toLowerCase(Locale.ROOT);
        mostrarErroEmail(null);
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { Toast.makeText(this, "Informe um e-mail válido.", Toast.LENGTH_LONG).show(); return; }

        View prog = findViewById(R.id.progressoEsqueci);
        View btn = findViewById(R.id.btnEnviar);
        prog.setVisibility(View.VISIBLE);
        btn.setEnabled(false);
        // O servidor (Render gratuito) dorme quando fica parado e pode levar até 2 minutos para acordar.
        Toast.makeText(this, "Enviando o código... Na primeira tentativa o servidor pode levar até 2 minutos para responder.", Toast.LENGTH_LONG).show();

        AuthApiService api = ApiClient.createService(AuthApiService.class);
        api.esqueciSenha(new EsqueciSenhaRequest(email)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> c, Response<Void> r) {
                prog.setVisibility(View.GONE);
                btn.setEnabled(true);
                // Só segue para a tela do código quando a API confirma que o e-mail saiu (204).
                if (r.isSuccessful()) {
                    // Não finaliza: o "voltar" da tela do código retorna aqui para corrigir o e-mail
                    Intent intent = new Intent(EsqueciSenhaActivity.this, CodigoSenhaActivity.class);
                    intent.putExtra(CodigoSenhaActivity.EXTRA_EMAIL, email);
                    startActivity(intent);
                } else if (r.code() == 404) {
                    mostrarErroEmail("Não encontramos nenhuma conta com o e-mail " + email
                            + ". Confira se digitou certo ou faça seu cadastro.");
                } else {
                    // Ex.: 422 quando o e-mail não pôde ser enviado: o código nem fica valendo.
                    mostrarErroEmail(EmpresaData.erro(r));
                }
            }
            @Override
            public void onFailure(Call<Void> c, Throwable t) {
                prog.setVisibility(View.GONE);
                btn.setEnabled(true);
                mostrarErroEmail(t instanceof java.io.InterruptedIOException
                        ? "O servidor demorou demais para responder e o código não foi enviado. Tente de novo em instantes."
                        : "Sem conexão com o servidor. Verifique sua internet e tente de novo.");
            }
        });
    }
}
