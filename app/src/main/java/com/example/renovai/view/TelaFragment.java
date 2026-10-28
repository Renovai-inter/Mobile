package com.example.renovai.view;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.renovai.R;
import com.example.renovai.TelaInsets;

/**
 * Base das abas dos menus (Fragments dentro da MenuActivity). Oferece os mesmos atalhos que as
 * telas tinham como Activity (findViewById, toast, vivo...), e liga a seta de voltar
 * (R.id.btnVoltar) ao voltar do sistema: volta para a aba anterior.
 */
public abstract class TelaFragment extends Fragment {

    protected TelaFragment(int layout) {
        super(layout);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // o cabeçalho da aba fica por trás da barra de status (mesma cor)
        View cabecalho = view.findViewWithTag(TelaInsets.TAG_TOPO);
        if (cabecalho != null) TelaInsets.topo(cabecalho);
        View seta = view.findViewById(R.id.btnVoltar);
        if (seta != null) seta.setOnClickListener(v -> voltar());
    }

    @SuppressWarnings("unchecked")
    protected <T extends View> T findViewById(int id) {
        View raiz = getView();
        return raiz == null ? null : (T) raiz.findViewById(id);
    }

    /** A Activity que hospeda a aba (serve como Context e como Activity para os helpers). */
    protected AppCompatActivity tela() {
        return (AppCompatActivity) requireActivity();
    }

    /** true enquanto a aba está na tela (as respostas da API podem chegar depois de sair dela). */
    protected boolean vivo() {
        return isAdded() && getView() != null && !requireActivity().isFinishing();
    }

    protected void toast(String msg) {
        Context c = getContext();
        if (c != null) Toast.makeText(c, msg, Toast.LENGTH_LONG).show();
    }

    protected void titulo(String t) {
        TextView tv = findViewById(R.id.txtTitulo);
        if (tv != null) tv.setText(t);
    }

    /** Troca para outra aba do mesmo menu (id do item, R.id.nav_...). */
    protected void abrirAba(int aba) {
        if (getActivity() instanceof MenuActivity) ((MenuActivity) getActivity()).abrirAba(aba);
    }

    /** Mesmo efeito do voltar do sistema: volta para a aba anterior ou fecha o menu. */
    protected void voltar() {
        requireActivity().getOnBackPressedDispatcher().onBackPressed();
    }
}
