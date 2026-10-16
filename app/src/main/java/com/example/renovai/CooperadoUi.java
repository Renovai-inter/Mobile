package com.example.renovai;

/**
 * Só o gerador de iniciais (mesma lógica de EmpresaActivity.gerarIniciais, que
 * era privada lá) — extraído aqui porque as 5 telas de Cooperado com cabeçalho
 * usam a mesma coisa para o avatar circular. Não altera EmpresaActivity.
 */
public final class CooperadoUi {

    private CooperadoUi() {
    }

    public static String gerarIniciais(String nome) {
        if (nome == null || nome.trim().isEmpty()) return "--";
        String[] partes = nome.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, partes.length); i++) {
            if (!partes[i].isEmpty()) sb.append(Character.toUpperCase(partes[i].charAt(0)));
        }
        return sb.length() > 0 ? sb.toString() : "--";
    }
}
