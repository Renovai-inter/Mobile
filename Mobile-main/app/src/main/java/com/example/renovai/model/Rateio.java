package com.example.renovai.model;

import java.util.Date;

public class Rateio {

    private String rateioId;
    private String cooperativaId;
    private String gestorId;
    private String tipoRateioId;
    private Date mesReferencia;
    private Date dataRateio;

    public Rateio() {
    }

    public Rateio(String rateioId, String cooperativaId, String gestorId, String tipoRateioId,
                   Date mesReferencia, Date dataRateio) {
        this.rateioId = rateioId;
        this.cooperativaId = cooperativaId;
        this.gestorId = gestorId;
        this.tipoRateioId = tipoRateioId;
        this.mesReferencia = mesReferencia;
        this.dataRateio = dataRateio;
    }

    public String getRateioId() {
        return rateioId;
    }

    public void setRateioId(String rateioId) {
        this.rateioId = rateioId;
    }

    public String getCooperativaId() {
        return cooperativaId;
    }

    public void setCooperativaId(String cooperativaId) {
        this.cooperativaId = cooperativaId;
    }

    public String getGestorId() {
        return gestorId;
    }

    public void setGestorId(String gestorId) {
        this.gestorId = gestorId;
    }

    public String getTipoRateioId() {
        return tipoRateioId;
    }

    public void setTipoRateioId(String tipoRateioId) {
        this.tipoRateioId = tipoRateioId;
    }

    public Date getMesReferencia() {
        return mesReferencia;
    }

    public void setMesReferencia(Date mesReferencia) {
        this.mesReferencia = mesReferencia;
    }

    public Date getDataRateio() {
        return dataRateio;
    }

    public void setDataRateio(Date dataRateio) {
        this.dataRateio = dataRateio;
    }

    @Override
    public String toString() {
        return "Rateio{" +
                "rateioId='" + rateioId + '\'' +
                ", cooperativaId='" + cooperativaId + '\'' +
                ", mesReferencia=" + mesReferencia +
                '}';
    }
}
