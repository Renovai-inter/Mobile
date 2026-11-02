package com.example.renovai.view;

import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.renovai.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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

        empresaButton.setOnClickListener(v -> abrirCadastro(CadastroActivity.TIPO_EMPRESA));
        cooperativaButton.setOnClickListener(v -> orientarCooperativa(this));

        // Voltar para a tela anterior (o login continua aberto embaixo)
        btnVoltar.setOnClickListener(v -> finish());
    }

    static void orientarCooperativa(AppCompatActivity activity) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.cooperativa_cadastro_titulo)
                .setMessage(R.string.cooperativa_cadastro_mensagem)
                .setPositiveButton(R.string.cooperativa_abrir_site, (dialog, which) -> {
                    try {
                        activity.startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse(activity.getString(R.string.cooperativa_cadastro_url))));
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(activity, R.string.cooperativa_sem_navegador,
                                Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cooperativa_ir_login, (dialog, which) -> {
                    Intent login = new Intent(activity, LoginActivity.class);
                    login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    activity.startActivity(login);
                    activity.finish();
                })
                .show();
    }

    private void abrirCadastro(String tipo) {
        Intent intent = new Intent(this, CadastroActivity.class);
        intent.putExtra(CadastroActivity.EXTRA_TIPO, tipo);
        startActivity(intent);
    }
}
