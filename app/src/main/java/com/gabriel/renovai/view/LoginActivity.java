package com.gabriel.renovai.view;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.gabriel.renovai.R;

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


        // Ir para a tela de EscolhaCadastro
        txtCadastre.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, EscolheActivity.class);
            startActivity(intent);
        });


    }
}