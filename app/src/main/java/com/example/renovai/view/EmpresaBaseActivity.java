package com.example.renovai.view;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renovai.EmpresaBottomNav;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Base das telas da Empresa — mesmo papel do GestorBaseActivity, mas a sessão é por Perfil
 * (SessionManager), não Funcionario.
 */
public abstract class EmpresaBaseActivity extends AppCompatActivity {

    protected boolean preparar(int layout, EmpresaBottomNav.Aba aba) {
        setContentView(layout);

        if (!SessionManager.estaLogado() || empresaId() == null) {
            Toast.makeText(
                            this,
                            "Não foi possível identificar a empresa. Faça login novamente.",
                            Toast.LENGTH_LONG)
                    .show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return false;
        }

        View header = findViewById(R.id.empresaHeader);
        View topbar = findViewById(R.id.empresaTopbar);
        View nav = findViewById(R.id.bottomNavigationEmpresa);
        GestorUi.insets(header != null ? header : topbar, nav);
        if (nav == null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
                    findViewById(android.R.id.content),
                    (v, ins) -> {
                        androidx.core.graphics.Insets bars =
                                ins.getInsets(
                                        androidx.core.view.WindowInsetsCompat.Type.systemBars());
                        v.setPadding(0, 0, 0, bars.bottom);
                        return ins;
                    });
        }

        if (header != null) {
            findViewById(R.id.btnPerfilHeaderE)
                    .setOnClickListener(
                            v -> startActivity(new Intent(this, EmpresaPerfilActivity.class)));
        }
        View voltar = findViewById(R.id.btnVoltar);
        if (voltar != null) voltar.setOnClickListener(v -> finish());

        if (nav instanceof BottomNavigationView) {
            EmpresaBottomNav.configurar(this, (BottomNavigationView) nav, aba);
        }
        return true;
    }

    /**
     * Preenche nome/iniciais do cabeçalho a partir dos dados já carregados (chame quando o "meu
     * perfil" chegar).
     */
    protected void preencherHeader(String nomeEmpresa) {
        View header = findViewById(R.id.empresaHeader);
        if (header == null) return;
        ((TextView) findViewById(R.id.txtNomeEmpresaHeader))
                .setText(nomeEmpresa != null ? nomeEmpresa : "Nome empresa");
        ((TextView) findViewById(R.id.txtIniciaisHeaderE)).setText(GestorUi.iniciais(nomeEmpresa));
    }

    protected void titulo(String t) {
        TextView tv = findViewById(R.id.txtTitulo);
        if (tv != null) tv.setText(t);
    }

    protected boolean vivo() {
        return !isFinishing() && !isDestroyed();
    }

    protected void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    protected String empresaId() {
        return SessionManager.getEmpresaId();
    }
}
