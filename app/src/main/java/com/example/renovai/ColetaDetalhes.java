package com.example.renovai;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Descrição e data prevista da triagem de cada coleta. O banco da API não tem esses campos (e não
 * pode ser alterado), então eles ficam no Firestore, em
 * cooperativas/{cooperativaId}/coletas/{coletaId} — o mesmo esquema dos relatórios — e aparecem
 * para qualquer pessoa da cooperativa, em qualquer aparelho. Regra em firebase/firestore.rules.
 */
public final class ColetaDetalhes {
    private static final String TAG = "ColetaDetalhes";
    private static final Map<String, Dados> memoria = new ConcurrentHashMap<>();

    private ColetaDetalhes() {}

    /** O que foi informado no cadastro da coleta (campos podem vir null). */
    public static final class Dados {
        public final String descricao;
        /** aaaa-mm-dd */
        public final String dataPrevistaTriagem;

        Dados(String descricao, String dataPrevistaTriagem) {
            this.descricao = descricao;
            this.dataPrevistaTriagem = dataPrevistaTriagem;
        }

        public boolean vazio() {
            return descricao == null && dataPrevistaTriagem == null;
        }
    }

    /** Guarda depois que a API criou a coleta. Sem descrição nem data, não grava nada. */
    public static void salvar(String coletaId, String descricao, LocalDate dataPrevista) {
        String texto = descricao == null || descricao.trim().isEmpty() ? null : descricao.trim();
        String data = dataPrevista == null ? null : dataPrevista.toString();
        String coop = CooperadoSession.getCooperativaId();
        if (coletaId == null || coop == null || (texto == null && data == null)) return;
        memoria.put(coletaId, new Dados(texto, data));
        if (!FirebaseCache.ativo()) return;
        FirebaseCache.garantirLogin(() -> {
            Map<String, Object> doc = new HashMap<>();
            if (texto != null) doc.put("descricao", texto);
            if (data != null) doc.put("dataPrevistaTriagem", data);
            doc.put("criadoPor", CooperadoSession.getFuncionarioId());
            doc.put("atualizadoEm", System.currentTimeMillis());
            documento(coop, coletaId).set(doc)
                    .addOnFailureListener(e -> Log.w(TAG, "Não foi possível guardar os detalhes da coleta", e));
        });
    }

    /**
     * Lê os detalhes de uma coleta da cooperativa logada. O retorno roda na thread principal, com
     * null se não houver nada (ou se não der para ler).
     */
    public static void ler(String coletaId, Consumer<Dados> retorno) {
        String coop = CooperadoSession.getCooperativaId();
        Dados conhecido = coletaId == null ? null : memoria.get(coletaId);
        if (conhecido != null || coletaId == null || coop == null || !FirebaseCache.ativo()) {
            retorno.accept(conhecido);
            return;
        }
        FirebaseCache.garantirLogin(() -> documento(coop, coletaId).get()
                .addOnSuccessListener(d -> {
                    Dados dados = d.exists()
                            ? new Dados(d.getString("descricao"), d.getString("dataPrevistaTriagem"))
                            : null;
                    if (dados != null) memoria.put(coletaId, dados);
                    new Handler(Looper.getMainLooper()).post(() -> retorno.accept(dados));
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Não foi possível ler os detalhes da coleta", e);
                    new Handler(Looper.getMainLooper()).post(() -> retorno.accept(null));
                }));
    }

    /** "2026-11-10" → "10/11/2026" (ou o texto como veio). */
    public static String dataFormatada(String iso) {
        if (iso == null || iso.length() < 10) return iso;
        return iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4);
    }

    private static com.google.firebase.firestore.DocumentReference documento(String coop, String coletaId) {
        return FirebaseFirestore.getInstance()
                .collection("cooperativas")
                .document(coop)
                .collection("coletas")
                .document(coletaId);
    }
}
