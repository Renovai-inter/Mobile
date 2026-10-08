package com.example.renovai;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Estima distância e tempo de uma rota: geocodifica os endereços das paradas (Geocoder do Android),
 * soma a distância em linha reta entre paradas consecutivas × 1,3 (fator de ruas) e assume 30 km/h
 * de velocidade média urbana. A API não guarda distância/tempo, então é sempre uma ESTIMATIVA.
 */
public final class RotaEstimador {

    public interface Callback {
        void resultado(double km, int minutos);
        void indisponivel();
    }

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final Map<String, double[]> CACHE = new HashMap<>();
    private static final Map<String, double[]> COORDS = new HashMap<>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private RotaEstimador() {
    }

    public static void estimar(Context ctx, List<String> enderecos, Callback cb) {
        if (enderecos == null || enderecos.size() < 2 || !Geocoder.isPresent()) {
            MAIN.post(cb::indisponivel);
            return;
        }
        final String chave = String.join("|", enderecos);
        synchronized (CACHE) {
            double[] c = CACHE.get(chave);
            if (c != null) {
                MAIN.post(() -> cb.resultado(c[0], (int) c[1]));
                return;
            }
        }
        final Context app = ctx.getApplicationContext();
        EXEC.execute(() -> {
            Geocoder g = new Geocoder(app, new Locale("pt", "BR"));
            List<double[]> pontos = new ArrayList<>();
            for (String e : enderecos) {
                double[] p = coord(g, e);
                if (p != null) pontos.add(p);
            }
            if (pontos.size() < 2) {
                MAIN.post(cb::indisponivel);
                return;
            }
            double km = 0;
            for (int i = 1; i < pontos.size(); i++) km += haversine(pontos.get(i - 1), pontos.get(i));
            km *= 1.3;
            int min = (int) Math.round(km / 30.0 * 60.0);
            final double kmF = km;
            synchronized (CACHE) {
                CACHE.put(chave, new double[]{kmF, min});
            }
            MAIN.post(() -> cb.resultado(kmF, min));
        });
    }

    @SuppressWarnings("deprecation")
    private static double[] coord(Geocoder g, String endereco) {
        synchronized (COORDS) {
            if (COORDS.containsKey(endereco)) return COORDS.get(endereco);
        }
        double[] r = null;
        try {
            List<Address> l = g.getFromLocationName(endereco, 1);
            if (l != null && !l.isEmpty()) r = new double[]{l.get(0).getLatitude(), l.get(0).getLongitude()};
        } catch (Exception ignored) {
        }
        synchronized (COORDS) {
            COORDS.put(endereco, r);
        }
        return r;
    }

    private static double haversine(double[] a, double[] b) {
        double R = 6371.0;
        double dLat = Math.toRadians(b[0] - a[0]), dLon = Math.toRadians(b[1] - a[1]);
        double x = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(a[0])) * Math.cos(Math.toRadians(b[0])) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));
    }

    public static String textoKm(double km) {
        return String.format(GestorUi.BR, "%.1f km", km);
    }

    public static String textoTempo(int min) {
        if (min < 60) return min + "min";
        return (min / 60) + "h " + String.format(Locale.ROOT, "%02d", min % 60) + "min";
    }
}
