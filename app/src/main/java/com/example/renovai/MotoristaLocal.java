package com.example.renovai;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.renovai.controller.ViagemRota;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Lembrança local da viagem neste aparelho. O andamento de verdade fica na API-Redis
 * (RotaViagemApiService), que apaga a sessão depois de gravar a rota finalizada no Postgres; por
 * isso a tela usa {@link #finalizadaHoje} para continuar mostrando "Finalizada hoje".
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
     * Coletas marcadas sem conexão, aguardando envio à API-Redis (ver ViagemRota). Ficam no
     * aparelho até a API aceitar, mesmo que o app seja fechado.
     */
    public static ViagemRota.Pendencias pendencias(Context c) {
        SharedPreferences p = prefs(c);
        return new ViagemRota.Pendencias() {
            @Override
            public Map<Integer, String> ler(String rotaId) {
                String prefixo = "envio_" + rotaId + "_";
                Map<Integer, String> out = new HashMap<>();
                for (Map.Entry<String, ?> e : p.getAll().entrySet()) {
                    if (!e.getKey().startsWith(prefixo) || !(e.getValue() instanceof String)) continue;
                    try {
                        out.put(Integer.parseInt(e.getKey().substring(prefixo.length())),
                                (String) e.getValue());
                    } catch (NumberFormatException ignorado) {
                        // chave fora do formato
                    }
                }
                return out;
            }

            @Override
            public void guardar(String rotaId, int indice, String status) {
                p.edit().putString("envio_" + rotaId + "_" + indice, status).apply();
            }

            @Override
            public void remover(String rotaId, int indice) {
                p.edit().remove("envio_" + rotaId + "_" + indice).apply();
            }
        };
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
