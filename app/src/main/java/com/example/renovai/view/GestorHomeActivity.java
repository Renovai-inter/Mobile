package com.example.renovai.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.renovai.CooperadoSession;
import com.example.renovai.Notificacoes;
import com.example.renovai.R;
import com.example.renovai.TelaInsets;

/**
 * Casca do Gestor: as abas do menu (Equipe · Coletas · Home · Estoque · Perfil) são Fragments
 * trocados aqui dentro, com histórico (ver MenuActivity).
 */
public class GestorHomeActivity extends MenuActivity {

    @Override
    protected void onCreate(Bundle b) {
        boolean logado = CooperadoSession.temCooperadoResolvido();
        super.onCreate(logado ? b : null);
        if (!logado) {
            Toast.makeText(
                            this,
                            "Não foi possível identificar o gestor. Faça login novamente.",
                            Toast.LENGTH_LONG)
                    .show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        montar(R.layout.activity_gestor_menu, R.id.bottomNavigationGestor, b);
        TelaInsets.conteudo(this, true);
        TelaInsets.navegacao(findViewById(R.id.bottomNavigationGestor));

        Notificacoes.iniciar(this);
    }

    @Override
    protected int abaInicial() {
        return R.id.nav_g_home;
    }

    @Override
    protected Fragment criarAba(int aba) {
        if (aba == R.id.nav_g_equipe) return new GestorFuncionariosFragment();
        if (aba == R.id.nav_g_coletas) return new GestorColetasFragment();
        if (aba == R.id.nav_g_home) return new GestorHomeFragment();
        if (aba == R.id.nav_g_estoque) return new GestorEstoqueFragment();
        if (aba == R.id.nav_g_perfil) return new GestorPerfilFragment();
        return null;
    }
}
