package com.example.renovai.view;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.SessionManager;

/**
 * Base das abas da Empresa — o mesmo papel do EmpresaBaseActivity: o cabeçalho (ou a barra de
 * título) desce para baixo da barra de status, e o avatar abre a aba Perfil.
 */
public abstract class EmpresaAbaFragment extends TelaFragment {

    protected EmpresaAbaFragment(int layout) {
        super(layout);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View header = findViewById(R.id.empresaHeader);
        View topo = header != null ? header : findViewById(R.id.empresaTopbar);
        GestorUi.insets(topo, null);
        // a aba entra depois que a janela já distribuiu os insets: pede de novo
        view.post(() -> ViewCompat.requestApplyInsets(view));

        if (header != null) {
            findViewById(R.id.btnPerfilHeaderE).setOnClickListener(v -> abrirAba(R.id.nav_perfil));
        }
    }

    /**
     * Preenche nome/iniciais do cabeçalho a partir dos dados já carregados (chame quando o "meu
     * perfil" chegar).
     */
    protected void preencherHeader(String nomeEmpresa) {
        if (findViewById(R.id.empresaHeader) == null) return;
        ((TextView) findViewById(R.id.txtNomeEmpresaHeader))
                .setText(nomeEmpresa != null ? nomeEmpresa : "Nome empresa");
        ((TextView) findViewById(R.id.txtIniciaisHeaderE)).setText(GestorUi.iniciais(nomeEmpresa));
    }

    protected String empresaId() {
        return SessionManager.getEmpresaId();
    }
}
