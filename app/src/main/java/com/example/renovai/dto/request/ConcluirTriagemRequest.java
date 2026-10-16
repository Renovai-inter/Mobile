package com.example.renovai.dto.request;

import java.math.BigDecimal;

/**
 * Espelha Requests.ConcluirTriagemRequest (backend) — corpo de
 * PATCH /triagens/{id}/concluir (tela 2.3, botão "marcar como concluída").
 * O backend resolve o status "CONCLUIDA" sozinho (ver TriagemService.concluir()).
 */
public class ConcluirTriagemRequest {
    private final BigDecimal quantidadeFinalKg;
    private final String observacao;

    public ConcluirTriagemRequest(BigDecimal quantidadeFinalKg, String observacao) {
        this.quantidadeFinalKg = quantidadeFinalKg;
        this.observacao = observacao;
    }

    public BigDecimal getQuantidadeFinalKg() { return quantidadeFinalKg; }
    public String getObservacao() { return observacao; }
}
