package com.example.renovai;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.PersistentCacheSettings;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache offline das listagens no Firestore.
 *
 * <p>A API (Postgres) continua sendo a fonte da verdade. Aqui guardamos só uma CÓPIA do último
 * JSON recebido de cada listagem, para a tela abrir na hora (inclusive sem internet). O Firestore
 * mantém essa cópia em disco no aparelho e sincroniza com a nuvem quando há conexão.
 *
 * <p>Estrutura: usuarios/{uidFirebase}/contas/{conta}/cache/{chave} → { json, atualizadoEm }
 *
 * <ul>
 *   <li>uidFirebase: login anônimo do Firebase (só para as regras de segurança).
 *   <li>conta: e-mail de quem está logado na API — assim o cache de uma
 *       conta nunca aparece para outra no mesmo aparelho.
 * </ul>
 */
public final class FirebaseCache {

    private static final String TAG = "FirebaseCache";

    /** Tamanho máximo do cache local do Firestore no aparelho. */
    private static final long TAMANHO_CACHE_BYTES = 100L * 1024 * 1024;

    /** Limite de um documento do Firestore é 1 MiB; deixamos uma folga. */
    private static final int LIMITE_JSON = 900 * 1024;

    public interface AoLer {
        /** json pode vir null (nada salvo ainda). */
        void aoLer(String json);
    }

    public interface AoLerTudo {
        /** chave → json. Nunca null (vazio se não houver nada). */
        void aoLer(Map<String, String> jsonPorChave);
    }

    private FirebaseCache() {}

    /** Chamado uma única vez, em RenovaiApplication.onCreate(). */
    public static void init(Context context) {
        try {
            FirebaseFirestore.getInstance()
                    .setFirestoreSettings(
                            new FirebaseFirestoreSettings.Builder()
                                    .setLocalCacheSettings(
                                            PersistentCacheSettings.newBuilder()
                                                    .setSizeBytes(TAMANHO_CACHE_BYTES)
                                                    .build())
                                    .build());
        } catch (Exception e) {
            // setFirestoreSettings só pode ser chamado antes do primeiro uso
            Log.w(TAG, "Firestore já estava inicializado", e);
        }
        garantirLogin(() -> {});
    }

    /**
     * Login anônimo do Firebase (necessário para as regras de segurança). O usuário anônimo fica
     * salvo no aparelho, então só precisa de internet na primeira vez.
     */
    public static void garantirLogin(Runnable depois) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            depois.run();
            return;
        }
        auth.signInAnonymously()
                .addOnCompleteListener(
                        t -> {
                            if (!t.isSuccessful())
                                Log.w(TAG, "Login anônimo do Firebase falhou", t.getException());
                            depois.run();
                        });
    }

    /**
     * Identifica a conta logada na API (para separar o cache por conta). O e-mail é salvo pelo
     * SessionManager em todo login e é único por conta (usuarios.email / perfis.email).
     */
    public static String contaAtual() {
        return SessionManager.getEmail();
    }

    private static String uid() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    private static com.google.firebase.firestore.CollectionReference colecao(String conta) {
        String uid = uid();
        if (uid == null || conta == null) return null;
        return FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .collection("contas")
                .document(sanitizar(conta))
                .collection("cache");
    }

    /** Ids de documento do Firestore não podem ter "/". */
    private static String sanitizar(String s) {
        return s.replace("/", "_");
    }

    /** Salva (ou substitui) a cópia de uma listagem. Funciona offline: sincroniza depois. */
    public static void salvar(String conta, String chave, String json) {
        if (json == null) return;
        if (json.length() > LIMITE_JSON) {
            Log.w(TAG, "Listagem '" + chave + "' grande demais para o cache (" + json.length() + ")");
            return;
        }
        garantirLogin(
                () -> {
                    com.google.firebase.firestore.CollectionReference col = colecao(conta);
                    if (col == null) return;
                    Map<String, Object> doc = new HashMap<>();
                    doc.put("json", json);
                    doc.put("atualizadoEm", FieldValue.serverTimestamp());
                    col.document(sanitizar(chave))
                            .set(doc, SetOptions.merge())
                            .addOnFailureListener(e -> Log.w(TAG, "Falha ao salvar " + chave, e));
                });
    }

    /** Lê uma listagem do cache LOCAL do aparelho (rápido e funciona sem internet). */
    public static void ler(String conta, String chave, AoLer callback) {
        garantirLogin(
                () -> {
                    com.google.firebase.firestore.CollectionReference col = colecao(conta);
                    if (col == null) {
                        callback.aoLer(null);
                        return;
                    }
                    col.document(sanitizar(chave))
                            .get(Source.CACHE)
                            .addOnCompleteListener(
                                    t -> {
                                        DocumentSnapshot d = t.isSuccessful() ? t.getResult() : null;
                                        callback.aoLer(
                                                d != null && d.exists() ? d.getString("json") : null);
                                    });
                });
    }

    /** Lê todas as listagens guardadas da conta, do cache LOCAL do aparelho. */
    public static void lerTudo(String conta, AoLerTudo callback) {
        garantirLogin(
                () -> {
                    com.google.firebase.firestore.CollectionReference col = colecao(conta);
                    if (col == null) {
                        callback.aoLer(new HashMap<>());
                        return;
                    }
                    col.get(Source.CACHE)
                            .addOnCompleteListener(
                                    t -> {
                                        Map<String, String> mapa = new HashMap<>();
                                        if (t.isSuccessful() && t.getResult() != null) {
                                            for (DocumentSnapshot d : t.getResult().getDocuments()) {
                                                String json = d.getString("json");
                                                if (json != null) mapa.put(d.getId(), json);
                                            }
                                        }
                                        callback.aoLer(mapa);
                                    });
                });
    }
}
