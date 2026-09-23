package com.example.renovai.view;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.renovai.R;
import com.example.renovai.controller.AuthController;
import com.example.renovai.dto.response.CadastroEmpresaResponse;
import com.google.android.material.card.MaterialCardView;

public class CadastroEmpresaActivity extends AppCompatActivity {

    private final AuthController authController = new AuthController();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightStatusBars(false);

        getWindow().setStatusBarColor(Color.parseColor("#882B4E"));

        setContentView(R.layout.activity_cadastro);

        //Implementando os elementos
        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        MaterialCardView btnCadastrar = findViewById(R.id.btnCadastrar);
        CheckBox checkTermos = findViewById(R.id.checkTermos);

        EditText edtNome = findViewById(R.id.edtNome);
        EditText edtEmail = findViewById(R.id.edtEmail);
        EditText edtTelefone = findViewById(R.id.edtTelefone);
        EditText edtEmpresa = findViewById(R.id.edtEmpresa);
        EditText edtCnpj = findViewById(R.id.edtCnpj);
        EditText edtEndereco = findViewById(R.id.edtEndereco);
        EditText edtSenha = findViewById(R.id.edtSenha);
        EditText edtConfirmarSenha = findViewById(R.id.edtConfirmarSenha);

        //Ir para Escolhe activity
        btnVoltar.setOnClickListener(v -> {
            Intent intent = new Intent(this, EscolheActivity.class);
            startActivity(intent);
        });

        //Enviar cadastro
        btnCadastrar.setOnClickListener(v -> {
            if (!checkTermos.isChecked()) {
                Toast.makeText(this, "Você precisa aceitar os Termos de Uso.", Toast.LENGTH_SHORT).show();
                return;
            }

            btnCadastrar.setEnabled(false);

            authController.cadastrarEmpresa(
                    edtNome.getText().toString(),
                    edtEmail.getText().toString(),
                    edtTelefone.getText().toString(),
                    edtEmpresa.getText().toString(),
                    edtCnpj.getText().toString(),
                    edtEndereco.getText().toString(),
                    edtSenha.getText().toString(),
                    edtConfirmarSenha.getText().toString(),
                    new AuthController.CadastroEmpresaCallback() {
                        @Override
                        public void onSuccess(CadastroEmpresaResponse resposta) {
                            Toast.makeText(CadastroEmpresaActivity.this,
                                    "Empresa cadastrada com sucesso!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(CadastroEmpresaActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }

                        @Override
                        public void onErro(String mensagem) {
                            btnCadastrar.setEnabled(true);
                            Toast.makeText(CadastroEmpresaActivity.this, mensagem, Toast.LENGTH_LONG).show();
                        }
                    }
            );
        });
    }
}