package com.example.renovai;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.LruCache;
import com.example.renovai.dto.response.ColetaResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Resolve a URL pelo backend, sem consultar o PostgreSQL ou a Admin API no APK. */
public final class FotoColetaRepository {
    public interface Resultado { void receber(String url, boolean falhou); }
    private static FotoColetaRepository instance;
    private final SharedPreferences prefs;
    private final ColetaApiService api;
    private final Map<String, List<Resultado>> pendentes = new HashMap<>();
    private final LruCache<String, Long> semFoto = new LruCache<>(128);

    FotoColetaRepository(Context context, ColetaApiService api) {
        prefs = context.getApplicationContext().getSharedPreferences("coleta-fotos", Context.MODE_PRIVATE);
        this.api = api;
    }

    public static synchronized FotoColetaRepository get(Context context) {
        if (instance == null) instance = new FotoColetaRepository(context, ApiClient.createService(ColetaApiService.class));
        return instance;
    }

    private String conta() { return SessionManager.getEmail() + "|" + SessionManager.getRole(); }
    private String chave(String id) { return conta() + "|COLETA|" + id; }

    public void guardar(String id, String url) {
        String normalized = ImagemUrl.normalizar(url);
        if (id == null || normalized == null) return;
        semFoto.remove(chave(id));
        prefs.edit().putString(chave(id), normalized).apply();
    }

    public void resolver(String id, Resultado resultado) {
        if (id == null || id.trim().isEmpty()) { resultado.receber(null, false); return; }
        String account = conta(), key = chave(id);
        String local = ImagemUrl.normalizar(prefs.getString(key, null));
        if (local != null) { resultado.receber(local, false); return; }
        Long absent = semFoto.get(key);
        if (absent != null && SystemClock.elapsedRealtime() - absent < 60_000) {
            resultado.receber(null, false); return;
        }
        List<Resultado> waiting = pendentes.get(key);
        if (waiting != null) { waiting.add(resultado); return; }
        waiting = new ArrayList<>();
        waiting.add(resultado);
        pendentes.put(key, waiting);
        api.buscarPorId(id).enqueue(new Callback<ColetaResponse>() {
            @Override public void onResponse(Call<ColetaResponse> call, Response<ColetaResponse> response) {
                boolean failed = !response.isSuccessful() || response.body() == null;
                String url = failed ? null : ImagemUrl.normalizar(response.body().getImagemUrl());
                if (!account.equals(conta())) { terminar(key, null, true); return; }
                // A listagem pode ter recebido uma foto mais nova enquanto o detalhe era buscado.
                String atualizado = ImagemUrl.normalizar(prefs.getString(key, null));
                if (atualizado != null) { terminar(key, atualizado, false); return; }
                if (!failed) {
                    if (url != null) prefs.edit().putString(key, url).apply();
                    else {
                        prefs.edit().remove(key).apply();
                        semFoto.put(key, SystemClock.elapsedRealtime());
                    }
                }
                terminar(key, url, failed);
            }
            @Override public void onFailure(Call<ColetaResponse> call, Throwable error) {
                terminar(key, null, true);
            }
        });
    }

    private void terminar(String key, String url, boolean failed) {
        List<Resultado> waiting = pendentes.remove(key);
        if (waiting != null) for (Resultado result : waiting) result.receber(url, failed);
    }
}
