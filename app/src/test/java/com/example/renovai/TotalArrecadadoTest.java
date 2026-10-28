package com.example.renovai;

import static org.junit.Assert.*;

import com.example.renovai.dto.response.GestorResponses;

import org.junit.Test;

import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;

/** Card "Total arrecadado no mês" (GestorPedidosActivity): só pedidos concluídos, pelo mês do fechamento. */
public class TotalArrecadadoTest {
    private static final YearMonth OUT = YearMonth.of(2026, 10);

    private static GestorResponses.Negociacao neg(
            String status, Double valor, String inicio, String fechamento) {
        GestorResponses.Negociacao n = new GestorResponses.Negociacao();
        n.statusAtual = status;
        n.valorTotal = valor;
        n.dataInicio = inicio;
        n.dataFechamento = fechamento;
        return n;
    }

    @Test
    public void somaApenasConcluidasNoMesDoFechamento() {
        double total =
                GestorUi.somaConcluidas(
                        Arrays.asList(
                                neg("Concluído", 100.0, "2026-10-01T10:00:00", "2026-10-05T10:00:00"),
                                neg("Acordo fechado", 50.5, "2026-09-20T10:00:00", "2026-10-02T10:00:00"),
                                // aceito, mas ainda não concluído: não entra
                                neg("Acordo fechado", 999.0, "2026-10-03T10:00:00", null),
                                neg("Em andamento", 999.0, "2026-10-03T10:00:00", null),
                                neg("Negociação recusada", 999.0, "2026-10-03T10:00:00", "2026-10-04T10:00:00"),
                                // concluída em setembro: não entra em outubro
                                neg("Concluído", 999.0, "2026-09-01T10:00:00", "2026-09-30T23:59:59"),
                                neg("Concluído", null, "2026-10-01T10:00:00", "2026-10-06T10:00:00")),
                        OUT);
        assertEquals(150.5, total, 0.0001);
    }

    @Test
    public void iniciadaNoMesAnteriorContaNoMesEmQueFoiConcluida() {
        GestorResponses.Negociacao n =
                neg("Concluído", 80.0, "2026-09-25T10:00:00", "2026-10-01T08:00:00.123456");
        assertEquals(80.0, GestorUi.somaConcluidas(Collections.singletonList(n), OUT), 0.0001);
        assertEquals(0.0, GestorUi.somaConcluidas(Collections.singletonList(n), OUT.minusMonths(1)), 0.0001);
    }

    @Test
    public void listaVaziaOuNulaDaZero() {
        assertEquals(0.0, GestorUi.somaConcluidas(null, OUT), 0.0);
        assertEquals(0.0, GestorUi.somaConcluidas(Collections.emptyList(), OUT), 0.0);
    }

    @Test
    public void valorDoCardSemPrefixoDeMoeda() {
        assertEquals("1.234,56", GestorUi.dinheiro(1234.56).replace("R$", "").trim());
    }
}
