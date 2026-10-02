package com.example.renovai.model;

import java.math.BigDecimal;
import java.util.Date;

public class LancamentoDespesa {

    private String lancamentoId;
    private String despesaId;
    private BigDecimal valor;
    private Date mesReferencia;
    private Date dataLancamento;

    public LancamentoDespesa() {
    }

    public LancamentoDespesa(String lancamentoId, String despesaId, BigDecimal valor,
                              Date mesReferencia, Date dataLancamento) {
        this.lancamentoId = lancamentoId;
        this.despesaId = despesaId;
        this.valor = valor;
        this.mesReferencia = mesReferencia;
        this.dataLancamento = dataLancamento;
    }

    public String getLancamentoId() {
        return lancamentoId;
    }

    public void setLancamentoId(String lancamentoId) {
        this.lancamentoId = lancamentoId;
    }

    public String getDespesaId() {
        return despesaId;
    }

    public void setDespesaId(String despesaId) {
        this.despesaId = despesaId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public Date getMesReferencia() {
        return mesReferencia;
    }

    public void setMesReferencia(Date mesReferencia) {
        this.mesReferencia = mesReferencia;
    }

    public Date getDataLancamento() {
        return dataLancamento;
    }

    public void setDataLancamento(Date dataLancamento) {
        this.dataLancamento = dataLancamento;
    }

    @Override
    public String toString() {
        return "LancamentoDespesa{" +
                "lancamentoId='" + lancamentoId + '\'' +
                ", despesaId='" + despesaId + '\'' +
                ", valor=" + valor +
                ", mesReferencia=" + mesReferencia +
                '}';
    }
}
