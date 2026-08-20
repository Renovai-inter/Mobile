package com.example.renovai.model;

import java.math.BigDecimal;
import java.util.Date;

public class Estoque {

    private String estoqueId;
    private String cooperativaId;
    private String materialId;
    private BigDecimal quantidadeKg = BigDecimal.ZERO;
    private Date dataAtualizacao;

    public Estoque() {
    }

    public Estoque(String estoqueId, String cooperativaId, String materialId,
                    BigDecimal quantidadeKg, Date dataAtualizacao) {
        this.estoqueId = estoqueId;
        this.cooperativaId = cooperativaId;
        this.materialId = materialId;
        this.quantidadeKg = quantidadeKg;
        this.dataAtualizacao = dataAtualizacao;
    }

    public String getEstoqueId() {
        return estoqueId;
    }

    public void setEstoqueId(String estoqueId) {
        this.estoqueId = estoqueId;
    }

    public String getCooperativaId() {
        return cooperativaId;
    }

    public void setCooperativaId(String cooperativaId) {
        this.cooperativaId = cooperativaId;
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

    public Date getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(Date dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    @Override
    public String toString() {
        return "Estoque{" +
                "estoqueId='" + estoqueId + '\'' +
                ", cooperativaId='" + cooperativaId + '\'' +
                ", materialId='" + materialId + '\'' +
                ", quantidadeKg=" + quantidadeKg +
                '}';
    }
}
