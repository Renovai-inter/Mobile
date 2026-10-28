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
 * Casca do Cooperado: as abas do menu (Rateios · Coletas · Home · Triagens · Perfil) são
 * Fragments trocados aqui dentro, com histórico (ver MenuActivity).
 */
public class CooperadoHomeActivity extends MenuActivity {

    @Override
    protected void onCreate(Bundle b) {
        boolean logado = CooperadoSession.temCooperadoResolvido();
        super.onCreate(logado ? b : null);
        if (!logado) {
            Toast.makeText(
                            this,
                            "Não foi possível identificar o cooperado. Faça login novamente.",
                            Toast.LENGTH_LONG)
                    .show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        montar(R.layout.activity_cooperado_menu, R.id.bottomNavigationCooperado, b);
        TelaInsets.conteudo(this, true);
        TelaInsets.navegacao(findViewById(R.id.bottomNavigationCooperado));

        Notificacoes.iniciar(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isFinishing()) Notificacoes.verificarAgora(this);
    }

    @Override
    protected int abaInicial() {
        return R.id.nav_home_cooperado;
    }

    @Override
    protected Fragment criarAba(int aba) {
        if (aba == R.id.nav_rateios) return new CooperadoRateiosFragment();
        if (aba == R.id.nav_coletas) return new CooperadoColetasFragment();
        if (aba == R.id.nav_home_cooperado) return new CooperadoHomeFragment();
        if (aba == R.id.nav_triagens) return new CooperadoTriagensFragment();
        if (aba == R.id.nav_perfil_cooperado) return new CooperadoPerfilFragment();
        return null;
    }
}
