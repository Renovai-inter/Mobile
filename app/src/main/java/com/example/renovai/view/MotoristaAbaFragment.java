package com.example.renovai.view;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.renovai.CooperadoSession;
import com.example.renovai.GestorUi;
import com.example.renovai.R;

/** Base das abas do Motorista — o mesmo papel do MotoristaBaseActivity. */
public abstract class MotoristaAbaFragment extends TelaFragment {

    protected MotoristaAbaFragment(int layout) {
        super(layout);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (findViewById(R.id.motoristaHeader) != null) {
            String coop = CooperadoSession.getCooperativaNome();
            ((TextView) findViewById(R.id.txtNomeCooperativaHeaderM))
                    .setText(GestorUi.nomeCabecalho(coop));
            ((TextView) findViewById(R.id.txtIniciaisHeaderM))
                    .setText(GestorUi.iniciais(CooperadoSession.getUsuarioNome()));
            findViewById(R.id.btnPerfilHeaderM)
                    .setOnClickListener(v -> abrirAba(R.id.nav_m_perfil));
        }
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

    protected void topoPerfil(String tipo) {
        String nome = CooperadoSession.getCooperativaNome();
        ((TextView) findViewById(R.id.txtIniciaisPerfil))
                .setText(GestorUi.iniciais(CooperadoSession.getUsuarioNome()));
        ((TextView) findViewById(R.id.txtPerfilCooperativa))
                .setText(nome != null ? nome : "Nome Cooperativa");
        ((TextView) findViewById(R.id.txtPerfilTipo)).setText(tipo);
    }
}
