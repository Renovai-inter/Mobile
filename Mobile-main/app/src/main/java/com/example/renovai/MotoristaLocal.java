package com.example.renovai;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Estado legado do protótipo local. As telas atuais não usam estes registros como andamento de
 * viagem. O acompanhamento será confirmado pela futura API com Redis; este arquivo não grava no
 * SQL.
 */
public final class MotoristaLocal {

    private MotoristaLocal() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext()
                .getSharedPreferences("renovai_motorista_local", Context.MODE_PRIVATE);
    }

    public static void marcarIniciada(Context c, String rotaId) {
        prefs(c).edit().putString("iniciada_" + rotaId, LocalDateTime.now().toString()).apply();
    }

    public static String iniciadaEm(Context c, String rotaId) {
        return prefs(c).getString("iniciada_" + rotaId, null);
    }

    public static boolean estaIniciada(Context c, String rotaId) {
        return iniciadaEm(c, rotaId) != null;
    }

    public static void marcarFinalizada(Context c, String rotaId) {
        prefs(c).edit().putString("finalizada_" + rotaId, LocalDateTime.now().toString()).apply();
    }

    public static String finalizadaEm(Context c, String rotaId) {
        return prefs(c).getString("finalizada_" + rotaId, null);
    }

    public static boolean finalizadaHoje(Context c, String rotaId) {
        String f = finalizadaEm(c, rotaId);
        return f != null && f.startsWith(LocalDate.now().toString());
    }

    public static void setParadaFeita(Context c, String rotaId, int ordem, boolean feita) {
        SharedPreferences.Editor e = prefs(c).edit();
        String chave = "parada_" + rotaId + "_" + ordem;
        if (feita) e.putBoolean(chave, true);
        else e.remove(chave);
        e.apply();
    }

    public static boolean paradaFeita(Context c, String rotaId, int ordem) {
        return prefs(c).getBoolean("parada_" + rotaId + "_" + ordem, false);
    }

    /** Quantas paradas "de coleta" (nem início nem fim) já foram marcadas — usado no progresso. */
    public static int contarFeitas(Context c, String rotaId, java.util.List<Integer> ordens) {
        int n = 0;
        for (int o : ordens) if (paradaFeita(c, rotaId, o)) n++;
        return n;
    }

    public static void limparRota(Context c, String rotaId, java.util.List<Integer> ordens) {
        SharedPreferences.Editor e = prefs(c).edit();
        e.remove("iniciada_" + rotaId);
        e.remove("finalizada_" + rotaId);
        for (int o : ordens) e.remove("parada_" + rotaId + "_" + o);
        e.apply();
    }

    /**
     * IDs de rota tocadas neste aparelho hoje (iniciadas OU finalizadas hoje) — para os contadores
     * da Home.
     */
    public static Set<String> tocadasHoje(Context c, java.util.List<String> rotaIds) {
        Set<String> out = new HashSet<>();
        String hoje = LocalDate.now().toString();
        for (String id : rotaIds) {
            String i = iniciadaEm(c, id), f = finalizadaEm(c, id);
            if ((i != null && i.startsWith(hoje)) || (f != null && f.startsWith(hoje))) out.add(id);
        }
        return out;
    }
}
