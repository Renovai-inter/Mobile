package com.example.renovai;

import android.app.Activity;
import android.content.Intent;

import com.example.renovai.view.EmpresaCooperativasActivity;
import com.example.renovai.view.EmpresaFavoritasActivity;
import com.example.renovai.view.EmpresaHomeActivity;
import com.example.renovai.view.EmpresaPedidosActivity;
import com.example.renovai.view.EmpresaPerfilActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Navegação inferior da Empresa (Favoritas · Cooperativas · Home · Pedidos · Perfil) — mesmo modelo
 * do GestorBottomNav.
 */
public final class EmpresaBottomNav {

    public enum Aba {
        FAVORITAS,
        COOPERATIVAS,
        HOME,
        PEDIDOS,
        PERFIL,
        NENHUMA
    }

    private EmpresaBottomNav() {}

    public static void configurar(Activity activity, BottomNavigationView nav, Aba atual) {
        if (atual == Aba.NENHUMA) {
            nav.getMenu().setGroupCheckable(0, false, true);
        } else {
            nav.setSelectedItemId(idDaAba(atual));
        }
        nav.setOnItemSelectedListener(
                item -> {
                    Aba destino = abaDoId(item.getItemId());
                    if (destino == null || destino == atual) return true;
                    activity.startActivity(new Intent(activity, classeDa(destino)));
                    activity.finish();
                    return true;
                });
    }

    private static int idDaAba(Aba a) {
        switch (a) {
            case FAVORITAS:
                return R.id.nav_favoritas;
            case COOPERATIVAS:
                return R.id.nav_cooperativas;
            case PEDIDOS:
                return R.id.nav_pedidos;
            case PERFIL:
                return R.id.nav_perfil;
            default:
                return R.id.nav_home;
        }
    }

    private static Aba abaDoId(int id) {
        if (id == R.id.nav_favoritas) return Aba.FAVORITAS;
        if (id == R.id.nav_cooperativas) return Aba.COOPERATIVAS;
        if (id == R.id.nav_home) return Aba.HOME;
        if (id == R.id.nav_pedidos) return Aba.PEDIDOS;
        if (id == R.id.nav_perfil) return Aba.PERFIL;
        return null;
    }

    private static Class<?> classeDa(Aba a) {
        switch (a) {
            case FAVORITAS:
                return EmpresaFavoritasActivity.class;
            case COOPERATIVAS:
                return EmpresaCooperativasActivity.class;
            case PEDIDOS:
                return EmpresaPedidosActivity.class;
            case PERFIL:
                return EmpresaPerfilActivity.class;
            default:
                return EmpresaHomeActivity.class;
        }
    }
}
