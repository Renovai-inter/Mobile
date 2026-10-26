package com.example.renovai.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.renovai.CooperadoSession;
import com.example.renovai.R;
import com.example.renovai.TelaInsets;

/**
 * Casca do Motorista: as abas do menu (Rotas · Home · Perfil) são Fragments trocados aqui dentro,
 * com histórico (ver MenuActivity).
 */
public class MotoristaHomeActivity extends MenuActivity {

    @Override
    protected void onCreate(Bundle b) {
        boolean logado = CooperadoSession.temCooperadoResolvido();
        super.onCreate(logado ? b : null);
        if (!logado) {
            Toast.makeText(
                            this,
                            "Não foi possível identificar o motorista. Faça login novamente.",
                            Toast.LENGTH_LONG)
                    .show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        montar(R.layout.activity_motorista_menu, R.id.bottomNavigationMotorista, b);
        TelaInsets.conteudo(this, true);
        TelaInsets.navegacao(findViewById(R.id.bottomNavigationMotorista));
    }

    @Override
    protected int abaInicial() {
        return R.id.nav_m_home;
    }

    @Override
    protected Fragment criarAba(int aba) {
        if (aba == R.id.nav_m_rotas) return new MotoristaRotasFragment();
        if (aba == R.id.nav_m_home) return new MotoristaHomeFragment();
        if (aba == R.id.nav_m_perfil) return new MotoristaPerfilFragment();
        return null;
    }
}
