package com.example.renovai.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.renovai.R;

public class EscolheActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightStatusBars(false);


        setContentView(R.layout.activity_escolhe);

        // Implementando os elementos
        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        View empresaButton = findViewById(R.id.empresaButton);
        View cooperativaButton = findViewById(R.id.cooperativaButton);

        // Mesma tela de cadastro para os dois; o tipo escolhido vai no Intent
        empresaButton.setOnClickListener(v -> abrirCadastro(CadastroActivity.TIPO_EMPRESA));
        cooperativaButton.setOnClickListener(v -> abrirCadastro(CadastroActivity.TIPO_COOPERATIVA));

        // Voltar para a tela anterior (o login continua aberto embaixo)
        btnVoltar.setOnClickListener(v -> finish());
    }

    private void abrirCadastro(String tipo) {
        Intent intent = new Intent(this, CadastroActivity.class);
        intent.putExtra(CadastroActivity.EXTRA_TIPO, tipo);
        startActivity(intent);
    }
}