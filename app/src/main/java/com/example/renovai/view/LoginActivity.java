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

public class LoginActivity extends AppCompatActivity {

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

        // Ir para a tela de recuperar senha
        txtEsqueceuSenha.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RecuperarSenhaActivity.class);
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

            authController.login(email, senha, new AuthController.LoginCallback() {
                @Override
                public void onSuccess(LoginResponse usuario) {
                    runOnUiThread(() -> {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    });
                }

                @Override
                public void onErro(String mensagem) {
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, mensagem, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });


    }
}