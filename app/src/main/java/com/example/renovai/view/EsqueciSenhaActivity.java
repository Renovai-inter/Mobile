package com.example.renovai.view;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
 * Tela 1.7 — Esqueci minha senha. 1º fator: o e-mail cadastrado. A API envia para ele um código
 * de 6 dígitos (2º fator), que é digitado na tela seguinte (RecuperarSenhaActivity).
 */
public class EsqueciSenhaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        getWindow().setStatusBarColor(Color.parseColor("#519059"));
        setContentView(R.layout.activity_esqueci_senha);

        findViewById(R.id.btnVoltar).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        findViewById(R.id.btnEnviar).setOnClickListener(v -> enviar());
    }

    private void enviar() {
        String email = ((EditText) findViewById(R.id.edtEmail)).getText().toString().trim();
        if (email.isEmpty()) { Toast.makeText(this, "Informe seu e-mail.", Toast.LENGTH_LONG).show(); return; }

        View prog = findViewById(R.id.progressoEsqueci);
        View btn = findViewById(R.id.btnEnviar);
        prog.setVisibility(View.VISIBLE);
        btn.setEnabled(false);

        AuthApiService api = ApiClient.createService(AuthApiService.class);
        api.esqueciSenha(new EsqueciSenhaRequest(email)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> c, Response<Void> r) {
                prog.setVisibility(View.GONE);
                btn.setEnabled(true);
                if (r.isSuccessful()) {
                    // A API responde igual mesmo se o e-mail não tiver conta (não revela quem está
                    // cadastrado), por isso a mensagem é condicional.
                    Toast.makeText(EsqueciSenhaActivity.this, "Se o e-mail estiver cadastrado, enviamos um código de 6 dígitos. Confira também a caixa de spam.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(EsqueciSenhaActivity.this, RecuperarSenhaActivity.class);
                    intent.putExtra(RecuperarSenhaActivity.EXTRA_EMAIL, email);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(EsqueciSenhaActivity.this, EmpresaData.erro(r), Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<Void> c, Throwable t) {
                prog.setVisibility(View.GONE);
                btn.setEnabled(true);
                Toast.makeText(EsqueciSenhaActivity.this, "Sem conexão com o servidor.", Toast.LENGTH_LONG).show();
            }
        });
    }
}
