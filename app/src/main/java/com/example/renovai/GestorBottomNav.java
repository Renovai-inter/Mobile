package com.example.renovai;

import android.app.Activity;
import android.content.Intent;

import com.example.renovai.view.GestorColetasActivity;
import com.example.renovai.view.GestorEstoqueActivity;
import com.example.renovai.view.GestorFuncionariosActivity;
import com.example.renovai.view.GestorHomeActivity;
import com.example.renovai.view.GestorPerfilActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Navegação inferior do Gestor (Equipe · Coletas · Home · Estoque · Perfil), no mesmo modelo do
 * CooperadoBottomNav: cada aba é uma Activity e a troca é startActivity + finish().
 * Telas secundárias (Pedidos, Rateios...) usam Aba.NENHUMA: a barra aparece sem aba marcada.
 */
public final class GestorBottomNav {

    public enum Aba { EQUIPE, COLETAS, HOME, ESTOQUE, PERFIL, NENHUMA }

    private GestorBottomNav() {
    }

    public static void configurar(Activity activity, BottomNavigationView nav, Aba atual) {
        if (atual == Aba.NENHUMA) {
            nav.getMenu().setGroupCheckable(0, false, true);
        } else {
            nav.setSelectedItemId(idDaAba(atual));
        }
        nav.setOnItemSelectedListener(item -> {
            Aba destino = abaDoId(item.getItemId());
            if (destino == null || destino == atual) return true;
            Intent intent = new Intent(activity, classeDa(destino));
            activity.startActivity(intent);
            activity.finish();
            return true;
        });
    }

    private static int idDaAba(Aba a) {
        switch (a) {
            case EQUIPE: return R.id.nav_g_equipe;
            case COLETAS: return R.id.nav_g_coletas;
            case ESTOQUE: return R.id.nav_g_estoque;
            case PERFIL: return R.id.nav_g_perfil;
            default: return R.id.nav_g_home;
        }
    }

    private static Aba abaDoId(int id) {
        if (id == R.id.nav_g_equipe) return Aba.EQUIPE;
        if (id == R.id.nav_g_coletas) return Aba.COLETAS;
        if (id == R.id.nav_g_home) return Aba.HOME;
        if (id == R.id.nav_g_estoque) return Aba.ESTOQUE;
        if (id == R.id.nav_g_perfil) return Aba.PERFIL;
        return null;
    }

    private static Class<?> classeDa(Aba a) {
        switch (a) {
            case EQUIPE: return GestorFuncionariosActivity.class;
            case COLETAS: return GestorColetasActivity.class;
            case ESTOQUE: return GestorEstoqueActivity.class;
            case PERFIL: return GestorPerfilActivity.class;
            default: return GestorHomeActivity.class;
        }
    }
}
