package com.example.renovai.view;

import android.view.View;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;

import com.example.renovai.CooperadoPreload;
import com.example.renovai.CooperadoSession;
import com.example.renovai.GestorData;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.SessionManager;

/** Tela 4.6 — Perfil do gestor: nome, cargo, e-mail e cooperativa (somente leitura, vindos da sessão do login), Suporte e sair. */
public class GestorPerfilFragment extends GestorAbaFragment {

    public GestorPerfilFragment() {
        super(R.layout.activity_gestor_perfil);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle b) {
        super.onViewCreated(view, b);
        titulo("Perfil");
        topoPerfil("Perfil: Gestor");

        // os 4 campos do layout são reaproveitados: Nome, E-mail, Cargo e Cooperativa (sem lápis: a API não edita perfil do gestor)
        campo(R.id.campoNome, "Nome", CooperadoSession.getUsuarioNome(), null);
        campo(R.id.campoEmail, "E-mail", SessionManager.getEmail(), null);
        campo(R.id.campoCpf, "Cargo", CooperadoSession.getCargo(), null);
        campo(R.id.campoTelefone, "Cooperativa", CooperadoSession.getCooperativaNome(), null);

        findViewById(R.id.btnSuporte).setOnClickListener(v -> SuporteChatActivity.abrir(requireContext()));
        findViewById(R.id.btnSair).setOnClickListener(v -> GestorUi.confirmar(tela(), "Sair da conta", "Deseja sair da conta?", "Sair", this::sair));
    }

    private void sair() {
        SessionManager.logout();
        CooperadoSession.limpar();
        GestorData.limparTudo();
        CooperadoPreload.limpar();
        Intent i = new Intent(tela(), LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }
}