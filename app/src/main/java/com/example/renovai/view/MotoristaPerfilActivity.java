package com.example.renovai.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;

import com.example.renovai.CooperadoPreload;
import com.example.renovai.CooperadoSession;
import com.example.renovai.GestorCache;
import com.example.renovai.MotoristaBottomNav;
import com.example.renovai.MotoristaData;
import com.example.renovai.R;
import com.example.renovai.SessionManager;

/** Tela 3.3 — Perfil do motorista: nome, e-mail e cargo (a partir da sessão do login). */
public class MotoristaPerfilActivity extends MotoristaBaseActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_motorista_perfil, MotoristaBottomNav.Aba.PERFIL)) return;
        titulo("Perfil");
        topoPerfil("Perfil: " + (CooperadoSession.getCargo() != null ? CooperadoSession.getCargo() : "Motorista"));

        campo(R.id.campoNome, "Nome", CooperadoSession.getUsuarioNome(), null);
        campo(R.id.campoEmail, "E-mail", SessionManager.getEmail(), null);
        campo(R.id.campoCargo, "Cargo", CooperadoSession.getCargo(), null);

        findViewById(R.id.btnSuporte).setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Suporte")
                .setMessage("Precisa de ajuda? Envie um e-mail para suporte@renovai.com.")
                .setPositiveButton("Enviar e-mail", (d, w) -> {
                    Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:suporte@renovai.com"));
                    i.putExtra(Intent.EXTRA_SUBJECT, "Suporte — app Renovaí (Motorista)");
                    try { startActivity(i); } catch (Exception e) { toast("Nenhum app de e-mail encontrado."); }
                }).setNegativeButton("Fechar", null).show());
        findViewById(R.id.btnSair).setOnClickListener(v -> com.example.renovai.GestorUi.confirmar(this, "Sair da conta", "Deseja sair da conta?", "Sair", this::sair));
    }

    private void sair() {
        SessionManager.logout();
        CooperadoSession.limpar();
        GestorCache.limpar();
        CooperadoPreload.limpar();
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }
}
