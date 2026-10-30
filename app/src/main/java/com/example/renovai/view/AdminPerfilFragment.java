package com.example.renovai.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.renovai.CooperadoPreload;
import com.example.renovai.CooperadoSession;
import com.example.renovai.GestorCache;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.SessionManager;

/** Perfil do Admin do site: dados da conta (da sessão do login), Suporte e sair. */
public class AdminPerfilFragment extends AdminAbaFragment {

    public AdminPerfilFragment() {
        super(R.layout.activity_gestor_perfil);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle b) {
        super.onViewCreated(view, b);
        titulo("Perfil");
        String nome = CooperadoSession.getUsuarioNome();
        ((TextView) findViewById(R.id.txtIniciaisPerfil))
                .setText(GestorUi.iniciais(nome != null ? nome : SessionManager.getEmail()));
        ((TextView) findViewById(R.id.txtPerfilCooperativa)).setText(nome != null ? nome : "Administrador");
        ((TextView) findViewById(R.id.txtPerfilTipo)).setText("Perfil: Administrador do site");

        // os 4 campos do layout do Gestor são reaproveitados (sem lápis: a API não edita esta conta)
        campo(R.id.campoNome, "Nome", nome, null);
        campo(R.id.campoEmail, "E-mail", SessionManager.getEmail(), null);
        campo(R.id.campoCpf, "Cargo", CooperadoSession.getCargo() != null ? CooperadoSession.getCargo() : "Admin do site", null);
        campo(R.id.campoTelefone, "Acesso", "Todas as cooperativas, empresas e contas", null);

        findViewById(R.id.btnSuporte).setOnClickListener(v -> SuporteChatActivity.abrir(requireContext()));
        findViewById(R.id.btnSair).setOnClickListener(v ->
                GestorUi.confirmar(tela(), "Sair da conta", "Deseja sair da conta?", "Sair", this::sair));
    }

    private void sair() {
        SessionManager.logout();
        CooperadoSession.limpar();
        GestorCache.limpar();
        CooperadoPreload.limpar();
        Intent i = new Intent(requireContext(), LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }
}
