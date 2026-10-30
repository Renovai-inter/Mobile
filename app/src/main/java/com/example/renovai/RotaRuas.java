package com.example.renovai;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Trajeto da rota pelas ruas, entre as paradas na ordem, pelo OSRM (OpenStreetMap). O Maps SDK
 * não traça rotas e a Directions API do Google é paga; o OSRM público é gratuito e não precisa de
 * chave, mas não tem garantia de disponibilidade — sem resposta, o mapa liga as paradas em reta.
 */
public final class RotaRuas {

    public interface Resposta {
        /** Um trecho por par de paradas consecutivas; null se não deu para traçar. Na thread principal. */
        void pronto(List<List<double[]>> trechos);
    }

    static final String OSRM = "https://router.project-osrm.org/route/v1/driving/";

    private static OkHttpClient cliente;
    private static Handler main;

    private RotaRuas() {}

    /** Busca o trajeto que passa por {@code pontos} ({lat, lng}) nessa ordem. */
    public static void buscar(List<double[]> pontos, Resposta resposta) {
        if (pontos == null || pontos.size() < 2) {
            resposta.pronto(null);
            return;
        }
        if (cliente == null)
            cliente = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();
        if (main == null) main = new Handler(Looper.getMainLooper());
        Request pedido = new Request.Builder()
                .url(url(pontos))
                .header("User-Agent", "Renovai/" + BuildConfig.VERSION_NAME + " (+https://github.com/Renovai-inter)")
                .build();
        int esperados = pontos.size() - 1;
        cliente.newCall(pedido).enqueue(new Callback() {
            @Override
            public void onFailure(Call c, IOException e) {
                main.post(() -> resposta.pronto(null));
            }

            @Override
            public void onResponse(Call c, Response r) {
                List<List<double[]>> trechos = null;
                try (ResponseBody corpo = r.body()) {
                    if (r.isSuccessful() && corpo != null) trechos = trechos(corpo.string(), esperados);
                } catch (Exception ignorado) {
                    // resposta fora do formato: fica a linha reta
                }
                List<List<double[]>> fim = trechos;
                main.post(() -> resposta.pronto(fim));
            }
        });
    }

    static String url(List<double[]> pontos) {
        StringBuilder sb = new StringBuilder(OSRM);
        for (int i = 0; i < pontos.size(); i++) {
            if (i > 0) sb.append(';');
            // o OSRM usa longitude,latitude
            sb.append(String.format(Locale.US, "%.6f,%.6f", pontos.get(i)[1], pontos.get(i)[0]));
        }
        return sb.append("?overview=false&steps=true&geometries=polyline").toString();
    }

    /** Um trecho por "leg" da resposta do OSRM, juntando a geometria dos passos de cada um. */
    static List<List<double[]>> trechos(String json, int esperados) {
        JsonObject raiz = JsonParser.parseString(json).getAsJsonObject();
        if (!raiz.has("code") || !"Ok".equals(raiz.get("code").getAsString())) return null;
        JsonArray rotas = raiz.getAsJsonArray("routes");
        if (rotas == null || rotas.size() == 0) return null;
        JsonArray legs = rotas.get(0).getAsJsonObject().getAsJsonArray("legs");
        if (legs == null || legs.size() != esperados) return null;
        List<List<double[]>> trechos = new ArrayList<>();
        for (JsonElement leg : legs) {
            List<double[]> trecho = new ArrayList<>();
            JsonArray passos = leg.getAsJsonObject().getAsJsonArray("steps");
            if (passos != null) {
                for (JsonElement passo : passos) {
                    JsonElement g = passo.getAsJsonObject().get("geometry");
                    if (g == null || !g.isJsonPrimitive()) continue;
                    for (double[] p : decodificar(g.getAsString())) {
                        double[] ultimo = trecho.isEmpty() ? null : trecho.get(trecho.size() - 1);
                        // cada passo começa onde o anterior termina
                        if (ultimo == null || ultimo[0] != p[0] || ultimo[1] != p[1]) trecho.add(p);
                    }
                }
            }
            if (trecho.size() < 2) return null;
            trechos.add(trecho);
        }
        return trechos;
    }

    /** Polyline codificada (precisão 5, formato do Google e do OSRM) → pontos {lat, lng}. */
    static List<double[]> decodificar(String s) {
        List<double[]> pontos = new ArrayList<>();
        int i = 0, lat = 0, lng = 0;
        while (i < s.length()) {
            int[] r = proximo(s, i);
            lat += r[0];
            r = proximo(s, r[1]);
            lng += r[0];
            i = r[1];
            pontos.add(new double[] {lat / 1e5, lng / 1e5});
        }
        return pontos;
    }

    /** {valor, próxima posição} */
    private static int[] proximo(String s, int i) {
        int resultado = 0, desloc = 0, b;
        do {
            b = s.charAt(i++) - 63;
            resultado |= (b & 0x1f) << desloc;
            desloc += 5;
        } while (b >= 0x20);
        return new int[] {(resultado & 1) != 0 ? ~(resultado >> 1) : resultado >> 1, i};
    }
}
