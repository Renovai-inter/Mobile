package com.example.renovai;

import android.app.Activity;

import com.example.renovai.view.EmpresaHomeActivity;
import com.example.renovai.view.MenuActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Menu inferior das telas secundárias da Empresa (chat, pedido, perfil público...): a barra aparece sem aba marcada e tocar
 * numa aba abre a casca (EmpresaHomeActivity) nela, por cima desta tela, então o voltar retorna para cá.
 * As abas em si são Fragments da casca (ver MenuActivity).
 */
public final class EmpresaBottomNav {

    /** As telas secundárias não são abas: a barra aparece sem nenhuma marcada. */
    public enum Aba { NENHUMA }

    private EmpresaBottomNav() {}

    public static void configurar(Activity activity, BottomNavigationView nav, Aba atual) {
        nav.getMenu().setGroupCheckable(0, false, true);
        nav.setOnItemSelectedListener(
                item -> {
                    MenuActivity.abrir(activity, EmpresaHomeActivity.class, item.getItemId());
                    return false;
                });
    }
}
