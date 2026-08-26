package com.example.renovai.dto.response;

import java.math.BigDecimal;

/** Espelha Responses.EstoqueResponse. */
public class EstoqueResponse {
    private String estoqueId;
    private String cooperativaId;
    private String cooperativaNome;
    private String materialId;
    private String materialCategoria;
    private BigDecimal quantidadeKg;
    private String dataAtualizacao; // ISO-8601, ex: "2026-08-20T14:30:00"

    public String getEstoqueId() { return estoqueId; }
    public String getCooperativaId() { return cooperativaId; }
    public String getCooperativaNome() { return cooperativaNome; }
    public String getMaterialId() { return materialId; }
    public String getMaterialCategoria() { return materialCategoria; }
    public BigDecimal getQuantidadeKg() { return quantidadeKg; }
    public String getDataAtualizacao() { return dataAtualizacao; }
}
