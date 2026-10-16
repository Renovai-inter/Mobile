package com.example.renovai.view;

import com.example.renovai.dto.response.GestorResponses;

/** Texto de endereço de uma parada de rota. */
final class GestorRotaTexto {
    private GestorRotaTexto() {
    }

    static String endereco(GestorResponses.RotaEnd e) {
        if (e == null) return "—";
        StringBuilder sb = new StringBuilder();
        if (e.nomeLocal != null && !e.nomeLocal.trim().isEmpty()) sb.append(e.nomeLocal.trim());
        String rua = ((e.logradouro == null ? "" : e.logradouro) + (e.numero == null || e.numero.isEmpty() ? "" : ", " + e.numero)).trim();
        if (!rua.isEmpty() && (sb.length() == 0 || !sb.toString().toLowerCase().contains(rua.toLowerCase()))) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(rua);
        }
        if (e.bairro != null && !e.bairro.isEmpty() && sb.indexOf(e.bairro) < 0) sb.append(" - ").append(e.bairro);
        return sb.length() == 0 ? "—" : sb.toString();
    }

    /** Texto usado para geocodificar a parada. */
    static String consulta(GestorResponses.RotaEnd e) {
        String rua = ((e.logradouro == null ? "" : e.logradouro) + (e.numero == null || e.numero.isEmpty() ? "" : ", " + e.numero)).trim();
        String q = rua.isEmpty() ? (e.nomeLocal == null ? "" : e.nomeLocal) : rua;
        if (e.bairro != null && !e.bairro.isEmpty()) q += ", " + e.bairro;
        if (e.cidade != null && !e.cidade.isEmpty()) q += ", " + e.cidade;
        return q;
    }

    /** Rótulo curto da parada (nome do local ou rua/número). */
    static String curto(GestorResponses.RotaEnd e) {
        if (e == null) return "—";
        if (e.nomeLocal != null && !e.nomeLocal.trim().isEmpty()) return e.nomeLocal.trim();
        String rua = ((e.logradouro == null ? "" : e.logradouro) + (e.numero == null || e.numero.isEmpty() ? "" : ", " + e.numero)).trim();
        return rua.isEmpty() ? "—" : rua;
    }
}
