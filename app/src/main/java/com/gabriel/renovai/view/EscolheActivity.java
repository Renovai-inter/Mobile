package com.gabriel.renovai.view;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.gabriel.renovai.R;

public class EscolheActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightStatusBars(false);

        getWindow().setStatusBarColor(Color.parseColor("#882B4E"));

        setContentView(R.layout.activity_escolhe);

        // Implementando os elementos
        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        View empresaButton = findViewById(R.id.empresaButton);
        View cooperativaButton = findViewById(R.id.cooperativaButton);

        // Ir para o cadastro de empresa
        empresaButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, CadastroEmpresaActivity.class);
            startActivity(intent);
        });

        // Voltar para tela de login
        btnVoltar.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        });
    }
}