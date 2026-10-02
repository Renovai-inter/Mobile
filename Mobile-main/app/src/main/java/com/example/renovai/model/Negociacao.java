package com.example.renovai.model;

import java.math.BigDecimal;
import java.util.Date;

public class Negociacao {

    private String negociacaoId;
    private String pedidoId;
    private String cooperativaId;
    private String empresaId;
    private String statusId;
    private BigDecimal valorTotal;
    private Date dataInicio;
    private Date dataFechamento;

    public Negociacao() {
    }

    public Negociacao(String negociacaoId, String pedidoId, String cooperativaId, String empresaId,
                       String statusId, BigDecimal valorTotal, Date dataInicio, Date dataFechamento) {
        this.negociacaoId = negociacaoId;
        this.pedidoId = pedidoId;
        this.cooperativaId = cooperativaId;
        this.empresaId = empresaId;
        this.statusId = statusId;
        this.valorTotal = valorTotal;
        this.dataInicio = dataInicio;
        this.dataFechamento = dataFechamento;
    }

    public String getNegociacaoId() {
        return negociacaoId;
    }

    public void setNegociacaoId(String negociacaoId) {
        this.negociacaoId = negociacaoId;
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(String pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getCooperativaId() {
        return cooperativaId;
    }

    public void setCooperativaId(String cooperativaId) {
        this.cooperativaId = cooperativaId;
    }

    public String getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(String empresaId) {
        this.empresaId = empresaId;
    }

    public String getStatusId() {
        return statusId;
    }

    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public Date getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(Date dataInicio) {
        this.dataInicio = dataInicio;
    }

    public Date getDataFechamento() {
        return dataFechamento;
    }

    public void setDataFechamento(Date dataFechamento) {
        this.dataFechamento = dataFechamento;
    }

    @Override
    public String toString() {
        return "Negociacao{" +
                "negociacaoId='" + negociacaoId + '\'' +
                ", pedidoId='" + pedidoId + '\'' +
                ", cooperativaId='" + cooperativaId + '\'' +
                ", valorTotal=" + valorTotal +
                '}';
    }
}
