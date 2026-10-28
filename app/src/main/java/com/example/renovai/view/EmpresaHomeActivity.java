package com.example.renovai.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.renovai.EmpresaCarrinho;
import com.example.renovai.GestorUi;
import com.example.renovai.Notificacoes;
import com.example.renovai.R;
import com.example.renovai.SessionManager;

/**
 * Casca da Empresa: as abas do menu (Favoritas · Cooperativas · Home · Pedidos · Perfil) são
 * Fragments trocados aqui dentro, com histórico (ver MenuActivity).
 */
public class EmpresaHomeActivity extends MenuActivity {

    @Override
    protected void onCreate(Bundle b) {
        boolean logado = SessionManager.estaLogado() && SessionManager.getEmpresaId() != null;
        super.onCreate(logado ? b : null);
        if (!logado) {
            Toast.makeText(
                            this,
                            "Não foi possível identificar a empresa. Faça login novamente.",
                            Toast.LENGTH_LONG)
                    .show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        montar(R.layout.activity_empresa_menu, R.id.bottomNavigationEmpresa, b);
        // o cabeçalho de cada aba desce sozinho para baixo da barra de status (EmpresaAbaFragment)
        GestorUi.insets(null, findViewById(R.id.bottomNavigationEmpresa));

        if (b == null) {
            // pedido que ficou no meio do envio (app fechado): retoma a finalização
            EmpresaCarrinho.restaurarPendente(this);
            if (EmpresaCarrinho.envio != null) {
                startActivity(new Intent(this, EmpresaFinalizarPedidoActivity.class));
            }
        }

        Notificacoes.iniciar(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isFinishing()) Notificacoes.verificarAgora(this);
    }

    @Override
    protected int abaInicial() {
        return R.id.nav_home;
    }

    @Override
    protected Fragment criarAba(int aba) {
        if (aba == R.id.nav_favoritas) return new EmpresaFavoritasFragment();
        if (aba == R.id.nav_cooperativas) return new EmpresaCooperativasFragment();
        if (aba == R.id.nav_home) return new EmpresaHomeFragment();
        if (aba == R.id.nav_pedidos) return new EmpresaPedidosFragment();
        if (aba == R.id.nav_perfil) return new EmpresaPerfilFragment();
        return null;
    }
}
