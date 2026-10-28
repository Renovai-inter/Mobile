package com.example.renovai;

import android.app.Activity;

import com.example.renovai.view.GestorHomeActivity;
import com.example.renovai.view.MenuActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Menu inferior das telas secundárias do Gestor (Pedidos, Rateios, detalhes...): a barra aparece sem aba marcada e tocar
 * numa aba abre a casca (GestorHomeActivity) nela, por cima desta tela, então o voltar retorna para cá.
 * As abas em si são Fragments da casca (ver MenuActivity).
 */
public final class GestorBottomNav {

    /** As telas secundárias não são abas: a barra aparece sem nenhuma marcada. */
    public enum Aba { NENHUMA }

    private GestorBottomNav() {}

    public static void configurar(Activity activity, BottomNavigationView nav, Aba atual) {
        nav.getMenu().setGroupCheckable(0, false, true);
        nav.setOnItemSelectedListener(
                item -> {
                    MenuActivity.abrir(activity, GestorHomeActivity.class, item.getItemId());
                    return false;
                });
    }
}
