package com.example.renovai.view;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renovai.CooperadoSession;
import com.example.renovai.GestorBottomNav;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Base das telas do Gestor: confere a sessão, preenche o cabeçalho (nome da cooperativa + avatar),
 * liga a seta de voltar e a barra de navegação, e trata os insets (edge-to-edge).
 */
public abstract class GestorBaseActivity extends AppCompatActivity {

    /**
     * @return false se a sessão não existe (a Activity já foi encerrada e o login aberto).
     */
    protected boolean preparar(int layout, GestorBottomNav.Aba aba) {
        setContentView(layout);

        if (!CooperadoSession.temCooperadoResolvido()) {
            Toast.makeText(
                            this,
                            "Não foi possível identificar o gestor. Faça login novamente.",
                            Toast.LENGTH_LONG)
                    .show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return false;
        }

        View header = findViewById(R.id.gestorHeader);
        View topbar = findViewById(R.id.gestorTopbar);
        View nav = findViewById(R.id.bottomNavigationGestor);
        com.example.renovai.TelaInsets.conteudo(this, nav != null);
        if (nav != null) com.example.renovai.TelaInsets.navegacao(nav);

        if (header != null) {
            String coop = CooperadoSession.getCooperativaNome();
            ((TextView) findViewById(R.id.txtNomeCooperativaHeader))
                    .setText(coop != null ? coop : "Nome Cooperativa");
            ((TextView) findViewById(R.id.txtIniciaisHeader))
                    .setText(GestorUi.iniciais(CooperadoSession.getUsuarioNome()));
            findViewById(R.id.btnPerfilHeader)
                    .setOnClickListener(
                            v -> startActivity(new Intent(this, GestorPerfilActivity.class)));
        }
        View voltar = findViewById(R.id.btnVoltar);
        if (voltar != null) voltar.setOnClickListener(v -> finish());

        if (nav instanceof BottomNavigationView) {
            GestorBottomNav.configurar(this, (BottomNavigationView) nav, aba);
        }
        return true;
    }

    protected void titulo(String t) {
        TextView tv = findViewById(R.id.txtTitulo);
        if (tv != null) tv.setText(t);
    }

    protected void subtitulo(String t) {
        TextView tv = findViewById(R.id.txtSubtitulo);
        if (tv != null) {
            tv.setText(t);
            tv.setVisibility(View.VISIBLE);
        }
    }

    protected boolean vivo() {
        return !isFinishing() && !isDestroyed();
    }

    protected void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    protected String coopId() {
        return CooperadoSession.getCooperativaId();
    }

    protected String funcId() {
        return CooperadoSession.getFuncionarioId();
    }

    /**
     * Preenche um campo "rótulo + valor + lápis" (layout view_gestor_campo incluído com o id
     * informado).
     */
    protected View campo(
            int includeId, String rotulo, String valor, View.OnClickListener aoEditar) {
        View c = findViewById(includeId);
        ((TextView) c.findViewById(R.id.txtCampoRotulo)).setText(rotulo);
        ((TextView) c.findViewById(R.id.txtCampoValor))
                .setText(valor == null || valor.isEmpty() ? "—" : valor);
        View lapis = c.findViewById(R.id.btnCampoEditar);
        if (aoEditar == null) lapis.setVisibility(View.GONE);
        else {
            lapis.setVisibility(View.VISIBLE);
            lapis.setOnClickListener(aoEditar);
            c.findViewById(R.id.txtCampoValor).setOnClickListener(aoEditar);
        }
        return c;
    }

    protected void valorCampo(int includeId, String valor) {
        ((TextView) findViewById(includeId).findViewById(R.id.txtCampoValor))
                .setText(valor == null || valor.isEmpty() ? "—" : valor);
    }

    protected void topoPerfil(String tipo) {
        String coop = com.example.renovai.CooperadoSession.getCooperativaNome();
        ((TextView) findViewById(R.id.txtIniciaisPerfil)).setText(GestorUi.iniciais(coop));
        ((TextView) findViewById(R.id.txtPerfilCooperativa))
                .setText(coop != null ? coop : "Nome da cooperativa");
        ((TextView) findViewById(R.id.txtPerfilTipo)).setText(tipo);
    }
}
