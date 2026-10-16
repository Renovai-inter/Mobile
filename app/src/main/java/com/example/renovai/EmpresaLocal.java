package com.example.renovai;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * A API não devolve, para um Pedido da própria empresa, a qual cooperativa ele foi enviado
 * (PedidoResponse não tem cooperativaId — só existe o vínculo inverso, por cooperativa). Como é a
 * própria Empresa quem cria esse vínculo (tela 5.3), guardamos aqui o nome/; id da cooperativa no
 * momento da criação, só para exibição na tela de detalhe. Uma vez que existir uma Negociação para
 * o pedido, ela já traz cooperativaId/cooperativaNome direto da API — essa cópia local deixa de ser
 * necessária e serve só de reforço/afallback.
 */
public final class EmpresaLocal {

    private EmpresaLocal() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext()
                .getSharedPreferences("renovai_empresa_local", Context.MODE_PRIVATE);
    }

    public static void lembrarCooperativa(
            Context c, String pedidoId, String cooperativaId, String nome) {
        prefs(c).edit()
                .putString("coop_" + pedidoId, cooperativaId + "|" + (nome == null ? "" : nome))
                .apply();
    }

    public static String[] cooperativaDoPedido(Context c, String pedidoId) {
        String v = prefs(c).getString("coop_" + pedidoId, null);
        if (v == null) return null;
        int i = v.indexOf('|');
        return new String[] {v.substring(0, i), v.substring(i + 1)};
    }
}
