package com.example.renovai.view;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.renovai.CooperadoSession;
import com.example.renovai.GestorUi;
import com.example.renovai.MotoristaBottomNav;
import com.example.renovai.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/** Base das telas do Motorista — mesmo papel do GestorBaseActivity. */
public abstract class MotoristaBaseActivity extends AppCompatActivity {

    protected boolean preparar(int layout, MotoristaBottomNav.Aba aba) {
        setContentView(layout);

        if (!CooperadoSession.temCooperadoResolvido()) {
            Toast.makeText(
                            this,
                            "Não foi possível identificar o motorista. Faça login novamente.",
                            Toast.LENGTH_LONG)
                    .show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return false;
        }

        View header = findViewById(R.id.motoristaHeader);
        View topbar =
                findViewById(R.id.gestorTopbar); // topbar do Gestor é genérica, reaproveitada aqui
        View nav = findViewById(R.id.bottomNavigationMotorista);
        com.example.renovai.TelaInsets.conteudo(this, nav != null);
        if (nav != null) com.example.renovai.TelaInsets.navegacao(nav);

        if (header != null) {
            String coop = CooperadoSession.getCooperativaNome();
            ((TextView) findViewById(R.id.txtNomeCooperativaHeaderM))
                    .setText(coop != null ? coop : "Nome Cooperativa");
            ((TextView) findViewById(R.id.txtIniciaisHeaderM))
                    .setText(GestorUi.iniciais(CooperadoSession.getUsuarioNome()));
            findViewById(R.id.btnPerfilHeaderM)
                    .setOnClickListener(
                            v -> startActivity(new Intent(this, MotoristaPerfilActivity.class)));
        }
        View voltar = findViewById(R.id.btnVoltar);
        if (voltar != null) voltar.setOnClickListener(v -> finish());

        if (nav instanceof BottomNavigationView) {
            MotoristaBottomNav.configurar(this, (BottomNavigationView) nav, aba);
        }
        return true;
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

    protected String coopId() {
        return CooperadoSession.getCooperativaId();
    }

    protected String funcId() {
        return CooperadoSession.getFuncionarioId();
    }

    /**
     * Preenche um campo "rótulo + valor + lápis" (layout view_gestor_campo, reaproveitado do
     * Gestor).
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
        String nome = CooperadoSession.getCooperativaNome();
        ((TextView) findViewById(R.id.txtIniciaisPerfil))
                .setText(GestorUi.iniciais(CooperadoSession.getUsuarioNome()));
        ((TextView) findViewById(R.id.txtPerfilCooperativa))
                .setText(nome != null ? nome : "Nome Cooperativa");
        ((TextView) findViewById(R.id.txtPerfilTipo)).setText(tipo);
    }
}
