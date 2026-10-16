package com.example.renovai;

import com.example.renovai.dto.response.GestorResponses;

import java.text.Normalizer;
import java.util.Locale;

/** Estados da Empresa preservam a distinção entre aceite e conclusão da cooperativa. */
public final class EmpresaStatus {
    public static final int EM_NEGOCIACAO = 0, ACEITO = 1, RECUSADO = 2, ABERTO = 3, FINALIZADO = 4;

    private EmpresaStatus() {}

    private static String normalizar(String s) {
        return s == null
                ? ""
                : Normalizer.normalize(s, Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "")
                        .toUpperCase(Locale.ROOT)
                        .replace('_', ' ');
    }

    public static int estado(String status) {
        String s = normalizar(status);
        if (s.contains("RECUS") || s.contains("CANCEL")) return RECUSADO;
        if (s.contains("FINALIZ") || s.contains("CONCLU")) return FINALIZADO;
        if (s.contains("ACEIT") || s.contains("ACORDO FECHADO") || s.contains("FECHAD"))
            return ACEITO;
        if (s.contains("EM NEGOCIACAO") || s.contains("ANDAMENTO")) return EM_NEGOCIACAO;
        return ABERTO;
    }

    public static int estado(GestorResponses.Negociacao n, String vinculo, String dataConclusao) {
        int pedido = estado(vinculo);
        int negociacao = n == null ? ABERTO : estado(n.statusAtual);
        if (negociacao == RECUSADO || pedido == RECUSADO) return RECUSADO;
        if (pedido == FINALIZADO || (dataConclusao != null && !dataConclusao.isBlank()))
            return FINALIZADO;
        if (n != null
                && aprovado(negociacao)
                && n.dataFechamento != null
                && !n.dataFechamento.isBlank()) return FINALIZADO;
        if (pedido == ACEITO || aprovado(negociacao)) return ACEITO;
        if (negociacao != ABERTO) return negociacao;
        return pedido;
    }

    public static boolean aprovado(int estado) {
        return estado == ACEITO || estado == FINALIZADO;
    }

    public static int cor(int estado) {
        return aprovado(estado) ? ACEITO : estado;
    }

    public static String rotulo(int estado) {
        switch (estado) {
            case ACEITO:
                return "Aceito";
            case FINALIZADO:
                return "Finalizado";
            case RECUSADO:
                return "Recusado";
            case EM_NEGOCIACAO:
                return "Em negociação";
            default:
                return "Aberto";
        }
    }
}
