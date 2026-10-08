package com.example.renovai.view;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.renovai.ApiClient;
import com.example.renovai.AuthApiService;
import com.example.renovai.R;
import com.example.renovai.dto.request.RedefinirSenhaRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Tela 1.7.1 — Redefinir Senha (Nova Senha): recebe o código gerado em 1.7 e define a nova senha. */
public class RecuperarSenhaActivity extends AppCompatActivity {

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

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnAlterar).setOnClickListener(v -> alterar());
    }

    private void alterar() {
        String token = ((EditText) findViewById(R.id.edtToken)).getText().toString().trim();
        String senha = ((EditText) findViewById(R.id.edtNovaSenha)).getText().toString();
        String confirmar = ((EditText) findViewById(R.id.edtConfirmarSenha)).getText().toString();

        if (token.isEmpty()) { Toast.makeText(this, "Cole o código de redefinição recebido.", Toast.LENGTH_LONG).show(); return; }
        if (senha.length() < 6) { Toast.makeText(this, "A senha deve ter no mínimo 6 caracteres.", Toast.LENGTH_LONG).show(); return; }
        if (!senha.equals(confirmar)) { Toast.makeText(this, "As senhas não coincidem.", Toast.LENGTH_LONG).show(); return; }

        View prog = findViewById(R.id.progressoAlterar);
        View btn = findViewById(R.id.btnAlterar);
        prog.setVisibility(View.VISIBLE);
        btn.setEnabled(false);

        AuthApiService api = ApiClient.createService(AuthApiService.class);
        api.redefinirSenha(new RedefinirSenhaRequest(token, senha)).enqueue(new Callback<Void>() {
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
                    Toast.makeText(RecuperarSenhaActivity.this, "Código inválido ou expirado. Solicite um novo.", Toast.LENGTH_LONG).show();
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
