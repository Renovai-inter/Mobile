package com.example.renovai;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Guarda, só neste aparelho, a senha do Renovaí de cada e-mail que já entrou com e-mail e senha.
 * É o que permite o "Entrar com Google" sem mudar a API: o app confere a conta Google
 * (GoogleTokenValidator), pega o e-mail confirmado e faz o login normal (/auth/login) com a senha
 * guardada aqui.
 *
 * <p>A senha é cifrada com AES-256/GCM e a chave fica no Android Keystore (não sai do aparelho e
 * não aparece no SharedPreferences). Desinstalar o app ou limpar os dados apaga tudo.
 */
public final class CofreCredenciais {

    private static final String TAG = "CofreCredenciais";
    private static final String PREFS = "renovai_cofre";
    private static final String ALIAS = "renovai_cofre_credenciais";
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String CIFRA = "AES/GCM/NoPadding";

    private static SharedPreferences prefs;

    private CofreCredenciais() {}

    public static void init(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String chave(String email) {
        return "senha_" + email.trim().toLowerCase(Locale.ROOT);
    }

    /** Guarda (ou troca) a senha deste e-mail. Falhas são ignoradas: o login normal continua. */
    public static void salvar(String email, String senha) {
        if (prefs == null || email == null || senha == null) return;
        try {
            Cipher cipher = Cipher.getInstance(CIFRA);
            cipher.init(Cipher.ENCRYPT_MODE, chaveSecreta());
            byte[] cifrado = cipher.doFinal(senha.getBytes(StandardCharsets.UTF_8));
            String valor =
                    Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP)
                            + ":"
                            + Base64.encodeToString(cifrado, Base64.NO_WRAP);
            prefs.edit().putString(chave(email), valor).apply();
        } catch (Exception e) {
            Log.w(TAG, "Não foi possível guardar a senha", e);
        }
    }

    /** Senha guardada deste e-mail, ou null se a pessoa nunca entrou com senha neste aparelho. */
    public static String ler(String email) {
        if (prefs == null || email == null) return null;
        String valor = prefs.getString(chave(email), null);
        if (valor == null) return null;
        try {
            String[] partes = valor.split(":", 2);
            byte[] iv = Base64.decode(partes[0], Base64.NO_WRAP);
            byte[] cifrado = Base64.decode(partes[1], Base64.NO_WRAP);
            Cipher cipher = Cipher.getInstance(CIFRA);
            cipher.init(Cipher.DECRYPT_MODE, chaveSecreta(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // chave apagada/trocada (ex.: backup restaurado em outro aparelho): descarta
            Log.w(TAG, "Senha guardada ilegível; descartando", e);
            remover(email);
            return null;
        }
    }

    public static boolean tem(String email) {
        return prefs != null && email != null && prefs.contains(chave(email));
    }

    /** Depois de redefinir a senha: atualiza só se este e-mail já estava ligado ao Google. */
    public static void atualizarSeExistir(String email, String novaSenha) {
        if (tem(email)) salvar(email, novaSenha);
    }

    public static void remover(String email) {
        if (prefs == null || email == null) return;
        prefs.edit().remove(chave(email)).apply();
    }

    private static SecretKey chaveSecreta() throws Exception {
        KeyStore ks = KeyStore.getInstance(KEYSTORE);
        ks.load(null);
        if (ks.containsAlias(ALIAS)) {
            return ((KeyStore.SecretKeyEntry) ks.getEntry(ALIAS, null)).getSecretKey();
        }
        KeyGenerator gerador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        gerador.init(
                new KeyGenParameterSpec.Builder(
                                ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build());
        return gerador.generateKey();
    }
}
