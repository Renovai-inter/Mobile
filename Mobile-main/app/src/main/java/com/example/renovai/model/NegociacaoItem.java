package com.example.renovai.model;

import java.math.BigDecimal;

public class NegociacaoItem {

    private String negociacaoItemId;
    private String negociacaoId;
    private String materialId;
    private BigDecimal quantidadeKg;
    private BigDecimal precoUnitario;

    public NegociacaoItem() {
    }

    public NegociacaoItem(String negociacaoItemId, String negociacaoId, String materialId,
                           BigDecimal quantidadeKg, BigDecimal precoUnitario) {
        this.negociacaoItemId = negociacaoItemId;
        this.negociacaoId = negociacaoId;
        this.materialId = materialId;
        this.quantidadeKg = quantidadeKg;
        this.precoUnitario = precoUnitario;
    }

    public String getNegociacaoItemId() {
        return negociacaoItemId;
    }

    public void setNegociacaoItemId(String negociacaoItemId) {
        this.negociacaoItemId = negociacaoItemId;
    }

    public String getNegociacaoId() {
        return negociacaoId;
    }

    public void setNegociacaoId(String negociacaoId) {
        this.negociacaoId = negociacaoId;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public BigDecimal getQuantidadeKg() {
        return quantidadeKg;
    }

    public void setQuantidadeKg(BigDecimal quantidadeKg) {
        this.quantidadeKg = quantidadeKg;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    @Override
    public String toString() {
        return "NegociacaoItem{" +
                "negociacaoItemId='" + negociacaoItemId + '\'' +
                ", negociacaoId='" + negociacaoId + '\'' +
                ", materialId='" + materialId + '\'' +
                ", quantidadeKg=" + quantidadeKg +
                '}';
    }
}
