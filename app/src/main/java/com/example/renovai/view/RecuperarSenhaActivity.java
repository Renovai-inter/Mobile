package com.example.renovai.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.renovai.ApiClient;
import com.example.renovai.AuthApiService;
import com.example.renovai.EmpresaData;
import com.example.renovai.R;
import com.example.renovai.dto.request.RedefinirSenhaRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tela 1.7.2 — Redefinir Senha (Nova Senha): recebe o e-mail e o código digitados em 1.7/1.7.1 e
 * define a nova senha. Se a API recusar o código, volta para a 1.7.1 com {@link
 * #RESULT_CODIGO_INVALIDO}.
 */
public class RecuperarSenhaActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "email";
    public static final String EXTRA_CODIGO = "codigo";
    /** Mensagem da API devolvida junto com {@link #RESULT_CODIGO_INVALIDO}. */
    public static final String EXTRA_ERRO = "erro";
    public static final int RESULT_CODIGO_INVALIDO = RESULT_FIRST_USER;

    private String email;
    private String codigo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightStatusBars(false);


        setContentView(R.layout.activity_recuperar_senha);

        email = getIntent().getStringExtra(EXTRA_EMAIL);
        codigo = getIntent().getStringExtra(EXTRA_CODIGO);
        if (email == null || codigo == null) {
            // Só se chega aqui pela tela do código (1.7.1)
            startActivity(new Intent(this, EsqueciSenhaActivity.class));
            finish();
            return;
        }

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        findViewById(R.id.btnAlterar).setOnClickListener(v -> alterar());
    }

    private void alterar() {
        String senha = ((EditText) findViewById(R.id.edtNovaSenha)).getText().toString();
        String confirmar = ((EditText) findViewById(R.id.edtConfirmarSenha)).getText().toString();

        if (senha.length() < 6 || senha.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            Toast.makeText(this, "A senha deve ter ao menos 6 caracteres e até 72 bytes.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!senha.equals(confirmar)) { Toast.makeText(this, "As senhas não coincidem.", Toast.LENGTH_LONG).show(); return; }

        View prog = findViewById(R.id.progressoAlterar);
        View btn = findViewById(R.id.btnAlterar);
        prog.setVisibility(View.VISIBLE);
        btn.setEnabled(false);

        AuthApiService api = ApiClient.createService(AuthApiService.class);
        api.redefinirSenha(new RedefinirSenhaRequest(email, codigo, senha)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> c, Response<Void> r) {
                prog.setVisibility(View.GONE);
                btn.setEnabled(true);
                if (r.isSuccessful()) {
                    Toast.makeText(RecuperarSenhaActivity.this, "Senha alterada. Entre com a nova senha.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(RecuperarSenhaActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else if (r.code() == 400 || r.code() == 422) {
                    setResult(RESULT_CODIGO_INVALIDO, new Intent().putExtra(EXTRA_ERRO, EmpresaData.erro(r)));
                    finish();
                } else {
                    Toast.makeText(RecuperarSenhaActivity.this, "Não foi possível alterar a senha (erro " + r.code() + ").", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> c, Throwable t) {
                prog.setVisibility(View.GONE);
                btn.setEnabled(true);
                Toast.makeText(RecuperarSenhaActivity.this, "Sem conexão com o servidor.", Toast.LENGTH_LONG).show();
            }
        });
    }
}
