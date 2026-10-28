package com.example.renovai.view;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.renovai.CooperadoSession;
import com.example.renovai.GestorUi;
import com.example.renovai.R;

/**
 * Base das abas do Gestor — o mesmo papel do GestorBaseActivity: preenche o cabeçalho (nome da
 * cooperativa + avatar, que abre a aba Perfil) e oferece os atalhos de campo/perfil.
 */
public abstract class GestorAbaFragment extends TelaFragment {

    protected GestorAbaFragment(int layout) {
        super(layout);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (findViewById(R.id.gestorHeader) != null) {
            ((TextView) findViewById(R.id.txtNomeCooperativaHeader)).setText(nomeNoCabecalho());
            ((TextView) findViewById(R.id.txtIniciaisHeader))
                    .setText(GestorUi.iniciais(CooperadoSession.getUsuarioNome()));
            findViewById(R.id.btnPerfilHeader).setOnClickListener(v -> abrirAba(abaPerfil()));
        }
    }

    /** Texto do cabeçalho: o nome da cooperativa (sem o "Cooperativa" do começo). */
    protected String nomeNoCabecalho() {
        return GestorUi.nomeCabecalho(CooperadoSession.getCooperativaNome());
    }

    /** Aba que o avatar do cabeçalho abre. */
    protected int abaPerfil() {
        return R.id.nav_g_perfil;
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

    protected void topoPerfil(String tipo) {
        String coop = CooperadoSession.getCooperativaNome();
        ((TextView) findViewById(R.id.txtIniciaisPerfil)).setText(GestorUi.iniciais(coop));
        ((TextView) findViewById(R.id.txtPerfilCooperativa))
                .setText(coop != null ? coop : "Nome da cooperativa");
        ((TextView) findViewById(R.id.txtPerfilTipo)).setText(tipo);
    }
}
