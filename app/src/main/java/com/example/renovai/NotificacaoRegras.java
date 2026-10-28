package com.example.renovai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Regras das notificações, sem Android: comparam o retrato salvo na última verificação com o
 * retrato atual da API e dizem o que mudou. Ver Notificacoes (quem agenda e mostra) e
 * NotificacaoVerificador (quem busca na API e monta os textos).
 */
public final class NotificacaoRegras {

    /** Abaixo disso o material do estoque gera aviso para o cooperado. */
    public static final double LIMITE_ESTOQUE_KG = 1000;

    /** Um item que apareceu (antigo == null) ou mudou de valor desde a última verificação. */
    public static final class Mudanca {
        public final String id, antigo, novo;

        Mudanca(String id, String antigo, String novo) {
            this.id = id;
            this.antigo = antigo;
            this.novo = novo;
        }

        public boolean ehNovo() {
            return antigo == null;
        }
    }

    private NotificacaoRegras() {}

    /**
     * O que apareceu ou mudou. Sem retrato anterior (primeira verificação da conta) não há
     * mudança: só se grava o retrato, para não avisar de tudo que já existia.
     */
    public static List<Mudanca> comparar(Map<String, String> antes, Map<String, String> agora) {
        List<Mudanca> r = new ArrayList<>();
        if (antes == null || agora == null) return r;
        for (Map.Entry<String, String> e : agora.entrySet()) {
            if (!antes.containsKey(e.getKey())) r.add(new Mudanca(e.getKey(), null, e.getValue()));
            else if (!Objects.equals(antes.get(e.getKey()), e.getValue()))
                r.add(new Mudanca(e.getKey(), antes.get(e.getKey()), e.getValue()));
        }
        return r;
    }

    /** O peso passou de >= limite para < limite (avisa uma vez por queda; rearma ao repor). */
    public static boolean caiuAbaixoDoLimite(String kgAntes, String kgAgora) {
        Double antes = numero(kgAntes), agora = numero(kgAgora);
        return antes != null
                && agora != null
                && antes >= LIMITE_ESTOQUE_KG
                && agora < LIMITE_ESTOQUE_KG;
    }

    /** A negociação chegou agora ao estado (EmpresaStatus) informado. */
    public static boolean chegouAoEstado(Mudanca m, int estado) {
        return m != null && !m.ehNovo() && estado(m.novo) == estado && estado(m.antigo) != estado;
    }

    /**
     * Contraproposta: a negociação já existia, segue em aberto e o valor proposto mudou. O retrato
     * de negociação é "estado|valor" (ver retratoNegociacao).
     */
    public static boolean ehContraproposta(Mudanca m) {
        if (m == null || m.ehNovo()) return false;
        int estado = estado(m.novo);
        if (estado == EmpresaStatus.FINALIZADO || estado == EmpresaStatus.RECUSADO) return false;
        String vAntes = valor(m.antigo), vAgora = valor(m.novo);
        return vAgora != null && !vAgora.equals(vAntes);
    }

    public static String retratoNegociacao(int estado, Double valor) {
        return estado + "|" + (valor == null ? "" : String.valueOf(valor));
    }

    static int estado(String retrato) {
        if (retrato == null) return -1;
        int i = retrato.indexOf('|');
        try {
            return Integer.parseInt(i < 0 ? retrato : retrato.substring(0, i));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String valor(String retrato) {
        if (retrato == null) return null;
        int i = retrato.indexOf('|');
        String v = i < 0 ? "" : retrato.substring(i + 1);
        return v.isEmpty() ? null : v;
    }

    private static Double numero(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
