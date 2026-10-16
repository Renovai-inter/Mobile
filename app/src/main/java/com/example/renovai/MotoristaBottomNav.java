package com.example.renovai;

import android.app.Activity;
import android.content.Intent;

import com.example.renovai.view.MotoristaHomeActivity;
import com.example.renovai.view.MotoristaPerfilActivity;
import com.example.renovai.view.MotoristaRotasActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/** Navegação inferior do Motorista (Rotas · Home · Perfil), mesmo modelo do GestorBottomNav. */
public final class MotoristaBottomNav {

    public enum Aba { ROTAS, HOME, PERFIL, NENHUMA }

    private MotoristaBottomNav() {
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
            activity.startActivity(new Intent(activity, classeDa(destino)));
            activity.finish();
            return true;
        });
    }

    private static int idDaAba(Aba a) {
        switch (a) {
            case ROTAS: return R.id.nav_m_rotas;
            case PERFIL: return R.id.nav_m_perfil;
            default: return R.id.nav_m_home;
        }
    }

    private static Aba abaDoId(int id) {
        if (id == R.id.nav_m_rotas) return Aba.ROTAS;
        if (id == R.id.nav_m_home) return Aba.HOME;
        if (id == R.id.nav_m_perfil) return Aba.PERFIL;
        return null;
    }

    private static Class<?> classeDa(Aba a) {
        switch (a) {
            case ROTAS: return MotoristaRotasActivity.class;
            case PERFIL: return MotoristaPerfilActivity.class;
            default: return MotoristaHomeActivity.class;
        }
    }
}
