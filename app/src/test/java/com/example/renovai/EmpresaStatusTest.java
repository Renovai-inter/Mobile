package com.example.renovai;

import static org.junit.Assert.*;

import com.example.renovai.dto.response.GestorResponses;

import org.junit.Test;

public class EmpresaStatusTest {
    @Test
    public void gestorSoContabilizaConclusaoDepoisDaDataDeFechamento() {
        GestorResponses.Negociacao n = new GestorResponses.Negociacao();
        n.statusAtual = "Acordo fechado";
        assertEquals(GestorUi.EM_NEGOCIACAO, GestorUi.estadoNeg(n));
        n.dataFechamento = "2026-10-02T10:00:00";
        assertEquals(GestorUi.CONCLUIDO, GestorUi.estadoNeg(n));
        n.statusAtual = "Negociação recusada";
        assertEquals(GestorUi.RECUSADO, GestorUi.estadoNeg(n));
    }

    @Test
    public void aceiteConfirmadoNoVinculoPrevaleceSobreConsultaAntigaDaNegociacao() {
        GestorResponses.Negociacao n = new GestorResponses.Negociacao();
        n.statusAtual = "Em andamento";
        assertEquals(EmpresaStatus.ACEITO, EmpresaStatus.estado(n, "Aceito", null));
        n.statusAtual = "CONCLUIDO";
        assertEquals(EmpresaStatus.ACEITO, EmpresaStatus.estado(n, "Aceito", null));
    }

    @Test
    public void acordoFechadoSemDataEhAceite() {
        GestorResponses.Negociacao n = new GestorResponses.Negociacao();
        n.statusAtual = "Acordo fechado";
        assertEquals(EmpresaStatus.ACEITO, EmpresaStatus.estado(n, "Aceito", null));
        assertEquals("Aceito", EmpresaStatus.rotulo(EmpresaStatus.estado(n, "Aceito", null)));
    }

    @Test
    public void finalizacaoConsideraVinculoEDataDoAcordo() {
        GestorResponses.Negociacao n = new GestorResponses.Negociacao();
        n.statusAtual = "Acordo fechado";
        assertEquals(EmpresaStatus.FINALIZADO, EmpresaStatus.estado(n, "Finalizado", null));
        n.dataFechamento = "2026-10-02T10:00:00";
        assertEquals(EmpresaStatus.FINALIZADO, EmpresaStatus.estado(n, "Aceito", null));
        assertEquals(
                EmpresaStatus.FINALIZADO,
                EmpresaStatus.estado(null, "Aceito", "2026-10-02T10:00:00"));
    }

    @Test
    public void recusaComDataNaoEhFinalizacaoNemAprovacao() {
        GestorResponses.Negociacao n = new GestorResponses.Negociacao();
        n.statusAtual = "Negociação recusada";
        n.dataFechamento = "2026-10-02T10:00:00";
        int estado = EmpresaStatus.estado(n, "Recusado", "2026-10-02T10:00:00");
        assertEquals(EmpresaStatus.RECUSADO, estado);
        assertFalse(EmpresaStatus.aprovado(estado));
    }

    @Test
    public void reconheceCatalogoOriginalEStatusNormalizados() {
        assertEquals(EmpresaStatus.EM_NEGOCIACAO, EmpresaStatus.estado("Em andamento"));
        assertEquals(EmpresaStatus.EM_NEGOCIACAO, EmpresaStatus.estado("EM_NEGOCIACAO"));
        assertEquals(EmpresaStatus.ACEITO, EmpresaStatus.estado("ACORDO_FECHADO"));
        assertEquals(EmpresaStatus.FINALIZADO, EmpresaStatus.estado("Concluído"));
        assertEquals(EmpresaStatus.ABERTO, EmpresaStatus.estado("Aberto"));
        assertTrue(EmpresaStatus.aprovado(EmpresaStatus.FINALIZADO));
        assertTrue(EmpresaStatus.aprovado(EmpresaStatus.ACEITO));
        assertFalse(EmpresaStatus.aprovado(EmpresaStatus.EM_NEGOCIACAO));
    }

    @Test
    public void escolheNegociacaoMaisRecenteSemDependerDaOrdemDoServidor() {
        GestorResponses.Negociacao velha = new GestorResponses.Negociacao();
        velha.pedidoId = "p";
        velha.cooperativaId = "c";
        velha.dataInicio = "2026-10-01T10:00:00";
        GestorResponses.Negociacao nova = new GestorResponses.Negociacao();
        nova.pedidoId = "p";
        nova.cooperativaId = "c";
        nova.dataInicio = "2026-10-02T10:00:00";
        assertSame(
                nova,
                EmpresaPedidos.negociacaoDoPedido(java.util.Arrays.asList(nova, velha), "p", "c"));
        assertNull(
                EmpresaPedidos.negociacaoDoPedido(
                        java.util.Arrays.asList(nova, velha), "p", "outra"));
    }
}
