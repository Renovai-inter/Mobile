package com.example.renovai;

import java.util.ArrayList;
import java.util.List;

/** Carrinho em memória; solicitações iniciadas são persistidas por empresa para retomar o envio. */
public final class EmpresaCarrinho {

    public static class Item {
        public final String materialId, categoriaNome;
        public final double quantidadeKg, precoUnitario;

        public Item(
                String materialId,
                String categoriaNome,
                double quantidadeKg,
                double precoUnitario) {
            this.materialId = materialId;
            this.categoriaNome = categoriaNome;
            this.quantidadeKg = quantidadeKg;
            this.precoUnitario = precoUnitario;
        }

        public double total() {
            return quantidadeKg * precoUnitario;
        }
    }

    public static String cooperativaId, cooperativaNome;
    public static String chaveSolicitacao = java.util.UUID.randomUUID().toString();
    public static com.example.renovai.dto.request.EmpresaRequests.EnviarPedido envio;
    public static final List<Item> itens = new ArrayList<>();

    public static boolean salvarPendente(android.content.Context context) {
        if (envio == null || SessionManager.getEmpresaId() == null) return false;
        return context.getSharedPreferences(
                        "empresa_pedido_pendente", android.content.Context.MODE_PRIVATE)
                .edit()
                .putString(SessionManager.getEmpresaId(), new com.google.gson.Gson().toJson(envio))
                .putString(SessionManager.getEmpresaId() + ":nome", cooperativaNome)
                .commit();
    }

    public static void restaurarPendente(android.content.Context context) {
        if (envio != null || SessionManager.getEmpresaId() == null) return;
        android.content.SharedPreferences prefs =
                context.getSharedPreferences(
                        "empresa_pedido_pendente", android.content.Context.MODE_PRIVATE);
        String json = prefs.getString(SessionManager.getEmpresaId(), null);
        if (json == null) return;
        try {
            envio =
                    new com.google.gson.Gson()
                            .fromJson(
                                    json,
                                    com.example.renovai.dto.request.EmpresaRequests.EnviarPedido
                                            .class);
            cooperativaId = envio.cooperativaId;
            cooperativaNome =
                    prefs.getString(SessionManager.getEmpresaId() + ":nome", "Cooperativa");
            chaveSolicitacao = envio.chaveSolicitacao;
            itens.clear();
            for (com.example.renovai.dto.request.EmpresaRequests.PedidoItem it : envio.itens)
                itens.add(
                        new Item(
                                it.materialId,
                                "Material",
                                it.quantidadeKg.doubleValue(),
                                it.precoUnitario.doubleValue()));
        } catch (RuntimeException e) {
            limpar();
            apagarPendente(context);
        }
    }

    public static void apagarPendente(android.content.Context context) {
        if (SessionManager.getEmpresaId() == null) return;
        context.getSharedPreferences(
                        "empresa_pedido_pendente", android.content.Context.MODE_PRIVATE)
                .edit()
                .remove(SessionManager.getEmpresaId())
                .remove(SessionManager.getEmpresaId() + ":nome")
                .commit();
    }

    private EmpresaCarrinho() {}

    public static void iniciar(String coopId, String coopNome) {
        if (!coopId.equals(cooperativaId)) limpar();
        cooperativaId = coopId;
        cooperativaNome = coopNome;
    }

    public static double total() {
        double t = 0;
        for (Item i : itens) t += i.total();
        return t;
    }

    public static void limpar() {
        itens.clear();
        chaveSolicitacao = java.util.UUID.randomUUID().toString();
        envio = null;
        cooperativaId = null;
        cooperativaNome = null;
    }
}
