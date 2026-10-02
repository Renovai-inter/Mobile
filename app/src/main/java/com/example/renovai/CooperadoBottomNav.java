package com.example.renovai;

import android.app.Activity;
import android.content.Intent;

import com.example.renovai.view.CooperadoHomeActivity;
import com.example.renovai.view.ListagemColetasActivity;
import com.example.renovai.view.ListagemTriagensActivity;
import com.example.renovai.view.PerfilCooperadoActivity;
import com.example.renovai.view.RateiRecebidosActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public final class CooperadoBottomNav {

    public enum Aba {
        RATEIOS,
        COLETAS,
        HOME,
        TRIAGENS,
        PERFIL
    }

    private CooperadoBottomNav() {}

    public static void configurar(Activity activity, BottomNavigationView nav, Aba abaAtual) {

        TelaInsets.conteudo(activity, true);
        TelaInsets.navegacao(nav);

        nav.setSelectedItemId(idParaAba(abaAtual));

        nav.setOnItemSelectedListener(
                item -> {
                    Aba destino = abaParaId(item.getItemId());

                    if (destino == null || destino == abaAtual) {
                        return true;
                    }

                    navegar(activity, destino);
                    return true;
                });
    }

    private static int idParaAba(Aba aba) {
        switch (aba) {
            case RATEIOS:
                return R.id.nav_rateios;
            case COLETAS:
                return R.id.nav_coletas;
            case TRIAGENS:
                return R.id.nav_triagens;
            case PERFIL:
                return R.id.nav_perfil_cooperado;
            case HOME:
            default:
                return R.id.nav_home_cooperado;
        }
    }

    private static Aba abaParaId(int id) {
        if (id == R.id.nav_rateios) return Aba.RATEIOS;
        if (id == R.id.nav_coletas) return Aba.COLETAS;
        if (id == R.id.nav_home_cooperado) return Aba.HOME;
        if (id == R.id.nav_triagens) return Aba.TRIAGENS;
        if (id == R.id.nav_perfil_cooperado) return Aba.PERFIL;

        return null;
    }

    private static void navegar(Activity activity, Aba destino) {
        Class<?> destinoClasse;

        switch (destino) {
            case RATEIOS:
                destinoClasse = RateiRecebidosActivity.class;
                break;

            case COLETAS:
                destinoClasse = ListagemColetasActivity.class;
                break;

            case TRIAGENS:
                destinoClasse = ListagemTriagensActivity.class;
                break;

            case PERFIL:
                destinoClasse = PerfilCooperadoActivity.class;
                break;

            case HOME:
            default:
                destinoClasse = CooperadoHomeActivity.class;
                break;
        }

        Intent intent = new Intent(activity, destinoClasse);
        activity.startActivity(intent);
        activity.finish();
    }
}
