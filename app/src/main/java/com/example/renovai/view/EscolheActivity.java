package com.example.renovai.view;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.renovai.R;

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

        // Mesma tela de cadastro para os dois; o tipo escolhido vai no Intent
        empresaButton.setOnClickListener(v -> abrirCadastro(CadastroActivity.TIPO_EMPRESA));
        cooperativaButton.setOnClickListener(v -> abrirCadastro(CadastroActivity.TIPO_COOPERATIVA));

        // Voltar para tela de login
        btnVoltar.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        });
    }

    private void abrirCadastro(String tipo) {
        Intent intent = new Intent(this, CadastroActivity.class);
        intent.putExtra(CadastroActivity.EXTRA_TIPO, tipo);
        startActivity(intent);
    }
}