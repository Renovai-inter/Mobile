package com.example.renovai.model;

import java.math.BigDecimal;

public class RateioFuncionario {

    private String rateioFuncionarioId;
    private String rateioId;
    private String cooperadoId;
    private BigDecimal valorRateio;

    public RateioFuncionario() {
    }

    public RateioFuncionario(String rateioFuncionarioId, String rateioId, String cooperadoId, BigDecimal valorRateio) {
        this.rateioFuncionarioId = rateioFuncionarioId;
        this.rateioId = rateioId;
        this.cooperadoId = cooperadoId;
        this.valorRateio = valorRateio;
    }

    public String getRateioFuncionarioId() {
        return rateioFuncionarioId;
    }

    public void setRateioFuncionarioId(String rateioFuncionarioId) {
        this.rateioFuncionarioId = rateioFuncionarioId;
    }

    public String getRateioId() {
        return rateioId;
    }

    public void setRateioId(String rateioId) {
        this.rateioId = rateioId;
    }

    public String getCooperadoId() {
        return cooperadoId;
    }

    public void setCooperadoId(String cooperadoId) {
        this.cooperadoId = cooperadoId;
    }

    public BigDecimal getValorRateio() {
        return valorRateio;
    }

    public void setValorRateio(BigDecimal valorRateio) {
        this.valorRateio = valorRateio;
    }

    @Override
    public String toString() {
        return "RateioFuncionario{" +
                "rateioFuncionarioId='" + rateioFuncionarioId + '\'' +
                ", rateioId='" + rateioId + '\'' +
                ", cooperadoId='" + cooperadoId + '\'' +
                ", valorRateio=" + valorRateio +
                '}';
    }
}
