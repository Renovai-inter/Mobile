package com.gabriel.renovai.view;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.gabriel.renovai.R;

public class CadastroEmpresaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightStatusBars(false);

        getWindow().setStatusBarColor(Color.parseColor("#882B4E"));

        setContentView(R.layout.activity_cadastro_empresa);

        //Implementando os elementos
        ImageView btnVoltar = findViewById(R.id.btnVoltar);


        //Ir para Escolhe activity
        btnVoltar.setOnClickListener(v -> {
            Intent intent = new Intent(this, EscolheActivity.class);
            startActivity(intent);
        });
    }
}