package com.example.renovai.controller;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Confere, no próprio app (sem passar pela API), o ID token que o Google devolveu no
 * "Entrar com Google" e extrai o e-mail confirmado.
 *
 * <p>Checagens (as mesmas recomendadas pelo Google para validar um ID token):
 *
 * <ol>
 *   <li>assinatura RS256 conferida com as chaves públicas do Google
 *       (https://www.googleapis.com/oauth2/v3/certs, guardadas em memória por 1 hora);
 *   <li>{@code iss} é accounts.google.com;
 *   <li>{@code aud} é o "ID do cliente da Web" do projeto Firebase (default_web_client_id);
 *   <li>{@code exp} ainda não passou;
 *   <li>{@code email_verified} é verdadeiro.
 * </ol>
 */
public final class GoogleTokenValidator {

    private static final String TAG = "GoogleTokenValidator";
    private static final String CHAVES_GOOGLE = "https://www.googleapis.com/oauth2/v3/certs";
    private static final long VALIDADE_CACHE_MS = TimeUnit.HOURS.toMillis(1);
    private static final long TOLERANCIA_RELOGIO_S = 120;

    public interface Callback {
        void onValido(String email);

        void onInvalido(String mensagem);
    }

    private static final OkHttpClient HTTP =
            new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();
    private static final Handler PRINCIPAL = new Handler(Looper.getMainLooper());

    private static final Map<String, PublicKey> chaves = new HashMap<>();
    private static long chavesBaixadasEm;

    private GoogleTokenValidator() {}

    public static void validar(String idToken, String clientId, Callback callback) {
        if (idToken == null || idToken.split("\\.").length != 3) {
            callback.onInvalido("Login do Google inválido. Tente de novo.");
            return;
        }
        if (clientId == null || clientId.isEmpty()) {
            callback.onInvalido("O login com Google não está configurado neste app.");
            return;
        }
        String kid;
        try {
            JSONObject cabecalho = json(idToken.split("\\.")[0]);
            if (!"RS256".equals(cabecalho.optString("alg"))) {
                callback.onInvalido("Login do Google inválido. Tente de novo.");
                return;
            }
            kid = cabecalho.optString("kid");
        } catch (Exception e) {
            callback.onInvalido("Login do Google inválido. Tente de novo.");
            return;
        }

        PublicKey chave = chaveEmCache(kid);
        if (chave != null) {
            entregar(callback, conferir(idToken, chave, clientId));
            return;
        }
        // chave não está em cache (ou cache vencido): baixa as chaves públicas do Google
        HTTP.newCall(new Request.Builder().url(CHAVES_GOOGLE).build())
                .enqueue(
                        new okhttp3.Callback() {
                            @Override
                            public void onFailure(Call call, java.io.IOException e) {
                                entregar(
                                        callback,
                                        Resultado.erro(
                                                "Sem conexão para confirmar o login com o Google."));
                            }

                            @Override
                            public void onResponse(Call call, Response response) {
                                try (ResponseBody corpo = response.body()) {
                                    if (!response.isSuccessful() || corpo == null) {
                                        entregar(
                                                callback,
                                                Resultado.erro(
                                                        "Não foi possível confirmar o login com o"
                                                                + " Google agora. Tente de novo."));
                                        return;
                                    }
                                    guardarChaves(corpo.string());
                                    PublicKey baixada = chaveEmCache(kid);
                                    entregar(
                                            callback,
                                            baixada == null
                                                    ? Resultado.erro(
                                                            "Login do Google inválido. Tente de"
                                                                    + " novo.")
                                                    : conferir(idToken, baixada, clientId));
                                } catch (Exception e) {
                                    Log.w(TAG, "Falha ao ler as chaves do Google", e);
                                    entregar(
                                            callback,
                                            Resultado.erro(
                                                    "Não foi possível confirmar o login com o"
                                                            + " Google agora. Tente de novo."));
                                }
                            }
                        });
    }

    // ── conferência do token ──

    private static Resultado conferir(String idToken, PublicKey chave, String clientId) {
        try {
            String[] partes = idToken.split("\\.");
            Signature assinatura = Signature.getInstance("SHA256withRSA");
            assinatura.initVerify(chave);
            assinatura.update((partes[0] + "." + partes[1]).getBytes(StandardCharsets.US_ASCII));
            if (!assinatura.verify(base64Url(partes[2]))) {
                return Resultado.erro("Login do Google inválido. Tente de novo.");
            }

            JSONObject dados = json(partes[1]);
            String iss = dados.optString("iss");
            if (!"accounts.google.com".equals(iss) && !"https://accounts.google.com".equals(iss)) {
                return Resultado.erro("Este login do Google não pertence ao app Renovaí.");
            }
            if (!audienciaConfere(dados.opt("aud"), clientId)) {
                return Resultado.erro("Este login do Google não pertence ao app Renovaí.");
            }
            long agora = System.currentTimeMillis() / 1000;
            if (dados.optLong("exp", 0) + TOLERANCIA_RELOGIO_S < agora) {
                return Resultado.erro("O login do Google expirou. Tente de novo.");
            }
            String email = dados.optString("email", "").trim();
            boolean verificado =
                    dados.optBoolean("email_verified", false)
                            || "true".equalsIgnoreCase(dados.optString("email_verified"));
            if (email.isEmpty() || !verificado) {
                return Resultado.erro("O e-mail desta conta Google não está verificado.");
            }
            return Resultado.ok(email);
        } catch (Exception e) {
            Log.w(TAG, "ID token do Google inválido", e);
            return Resultado.erro("Login do Google inválido. Tente de novo.");
        }
    }

    private static boolean audienciaConfere(Object aud, String clientId) {
        if (aud instanceof JSONArray) {
            JSONArray lista = (JSONArray) aud;
            for (int i = 0; i < lista.length(); i++) {
                if (clientId.equals(lista.optString(i))) return true;
            }
            return false;
        }
        return aud != null && clientId.equals(String.valueOf(aud));
    }

    // ── chaves públicas do Google ──

    private static synchronized PublicKey chaveEmCache(String kid) {
        if (System.currentTimeMillis() - chavesBaixadasEm > VALIDADE_CACHE_MS) return null;
        return chaves.get(kid);
    }

    private static synchronized void guardarChaves(String jwks) throws Exception {
        JSONArray lista = new JSONObject(jwks).getJSONArray("keys");
        Map<String, PublicKey> novas = new HashMap<>();
        KeyFactory fabrica = KeyFactory.getInstance("RSA");
        for (int i = 0; i < lista.length(); i++) {
            JSONObject k = lista.getJSONObject(i);
            if (!"RSA".equals(k.optString("kty"))) continue;
            BigInteger n = new BigInteger(1, base64Url(k.getString("n")));
            BigInteger e = new BigInteger(1, base64Url(k.getString("e")));
            novas.put(k.getString("kid"), fabrica.generatePublic(new RSAPublicKeySpec(n, e)));
        }
        chaves.clear();
        chaves.putAll(novas);
        chavesBaixadasEm = System.currentTimeMillis();
    }

    // ── utilitários ──

    private static byte[] base64Url(String texto) {
        return Base64.decode(texto, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    }

    private static JSONObject json(String parteBase64Url) throws Exception {
        return new JSONObject(new String(base64Url(parteBase64Url), StandardCharsets.UTF_8));
    }

    private static void entregar(Callback callback, Resultado r) {
        PRINCIPAL.post(
                () -> {
                    if (r.email != null) callback.onValido(r.email);
                    else callback.onInvalido(r.erro);
                });
    }

    private static final class Resultado {
        final String email;
        final String erro;

        private Resultado(String email, String erro) {
            this.email = email;
            this.erro = erro;
        }

        static Resultado ok(String email) {
            return new Resultado(email, null);
        }

        static Resultado erro(String mensagem) {
            return new Resultado(null, mensagem);
        }
    }
}
