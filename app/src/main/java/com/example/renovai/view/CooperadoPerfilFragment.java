package com.example.renovai.view;

import android.view.View;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.renovai.CooperadoPreload;
import com.example.renovai.CooperadoUi;
import com.example.renovai.GestorUi;
import com.example.renovai.SessionManager;
import com.example.renovai.R;
import com.example.renovai.controller.PerfilCooperadoController;


/** Tela 2.4 — Perfil do Cooperado: nome, e-mail, cargo e cooperativa vinculada. */
public class CooperadoPerfilFragment extends TelaFragment {

    private TextView txtNomeCooperativaPerfil, txtIniciaisCooperativaPerfil;
    private TextView txtNomeCooperado, txtEmailCooperado, txtCargoCooperado;

    public CooperadoPerfilFragment() {
        super(R.layout.activity_perfil_cooperado);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        findViewById(R.id.btnVoltarPerfil).setOnClickListener(v -> voltar());

        txtNomeCooperativaPerfil = findViewById(R.id.txtNomeCooperativaPerfil);
        txtIniciaisCooperativaPerfil = findViewById(R.id.txtIniciaisCooperativaPerfil);
        txtNomeCooperado = findViewById(R.id.txtNomeCooperado);
        txtEmailCooperado = findViewById(R.id.txtEmailCooperado);
        txtCargoCooperado = findViewById(R.id.txtCargoCooperado);

        // Edição de nome exigiria reenviar CPF + senha atual no PUT /usuarios/{id}
        // (Requests.UsuarioRequest não permite atualização parcial) — não dá pra
        // fazer isso com segurança nesta tela sem pedir a senha de novo, então o
        // ícone só avisa em vez de simular uma atualização que a API não suporta.
        findViewById(R.id.btnEditarNome).setOnClickListener(v ->
                Toast.makeText(tela(),
                        "Edição de nome requer confirmar a senha atual — indisponível nesta tela.",
                        Toast.LENGTH_LONG).show());

        findViewById(R.id.btnSuporte).setOnClickListener(v -> abrirSuporte());
        findViewById(R.id.btnSair).setOnClickListener(v -> GestorUi.confirmar(
                tela(), "Sair da conta", "Deseja sair da conta?", "Sair", this::sair));


        carregarPerfil();
    }

    private void carregarPerfil() {
        new PerfilCooperadoController().carregarPerfil(new PerfilCooperadoController.Callback() {
            @Override
            public void onSuccess(PerfilCooperadoController.PerfilCooperado perfil) {
                txtNomeCooperativaPerfil.setText(
                        perfil.cooperativaNome != null ? perfil.cooperativaNome : "Cooperativa");
                txtIniciaisCooperativaPerfil.setText(CooperadoUi.gerarIniciais(perfil.cooperativaNome));
                txtNomeCooperado.setText(perfil.nome != null ? perfil.nome : "—");
                txtEmailCooperado.setText(perfil.email != null ? perfil.email : "—");
                txtCargoCooperado.setText(perfil.cargo != null ? perfil.cargo : "—");
            }

            @Override
            public void onErro(String mensagem) {
                Toast.makeText(tela(), mensagem, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void abrirSuporte() {
        SuporteChatActivity.abrir(requireContext());
    }

    /** Mesmo logout das outras áreas: descadastra o push, limpa sessão e cache e volta ao login. */
    private void sair() {
        SessionManager.logout();
        com.example.renovai.GestorCache.limpar();
        CooperadoPreload.limpar();
        Intent i = new Intent(tela(), LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }
}
