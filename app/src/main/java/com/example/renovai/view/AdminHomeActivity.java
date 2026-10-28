package com.example.renovai.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.renovai.R;
import com.example.renovai.SessionManager;
import com.example.renovai.TelaInsets;

import java.util.Locale;

/**
 * Casca do Admin do site (role ADMIN_SITE): as abas do menu (Cooperativas · Empresas · Home ·
 * Contas · Perfil) são Fragments trocados aqui dentro, com histórico (ver MenuActivity).
 */
public class AdminHomeActivity extends MenuActivity {

    /** O login devolve ADMIN_SITE para o administrador do site (ver SecurityConfig da API). */
    public static boolean ehAdminDoSite(String role) {
        return role != null && role.toUpperCase(Locale.ROOT).contains("ADMIN_SITE");
    }

    @Override
    protected void onCreate(Bundle b) {
        boolean logado = SessionManager.estaLogado() && ehAdminDoSite(SessionManager.getRole());
        super.onCreate(logado ? b : null);
        if (!logado) {
            Toast.makeText(this, "Faça login novamente.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        montar(R.layout.activity_admin_menu, R.id.bottomNavigationAdmin, b);
        TelaInsets.conteudo(this, true);
        TelaInsets.navegacao(findViewById(R.id.bottomNavigationAdmin));
    }

    @Override
    protected int abaInicial() {
        return R.id.nav_a_home;
    }

    @Override
    protected Fragment criarAba(int aba) {
        if (aba == R.id.nav_a_cooperativas) return AdminListaFragment.nova(AdminListaFragment.COOPERATIVAS);
        if (aba == R.id.nav_a_empresas) return AdminListaFragment.nova(AdminListaFragment.EMPRESAS);
        if (aba == R.id.nav_a_home) return new AdminHomeFragment();
        if (aba == R.id.nav_a_contas) return AdminListaFragment.nova(AdminListaFragment.CONTAS);
        if (aba == R.id.nav_a_perfil) return new AdminPerfilFragment();
        return null;
    }
}
