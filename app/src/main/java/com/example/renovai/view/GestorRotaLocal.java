package com.example.renovai.view;

import android.content.Context;

import java.time.LocalDate;

/** A API não guarda a data de criação da rota: guardamos localmente a das rotas criadas por este app. */
final class GestorRotaLocal {
    private GestorRotaLocal() {
    }

    private static android.content.SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences("renovai_rotas_local", Context.MODE_PRIVATE);
    }

    static void marcarCriada(Context c, String rotaId) {
        p(c).edit().putString(rotaId, LocalDate.now().toString()).apply();
    }

    static String criada(Context c, String rotaId) {
        String d = p(c).getString(rotaId, null);
        return d == null ? "—" : com.example.renovai.GestorUi.data(d);
    }
}
