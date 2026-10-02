package com.example.renovai.model;

public class TipoRateio {

    private String tipoRateioId;
    private String tipoRateio;
    private String descricao;

    public TipoRateio() {
    }

    public TipoRateio(String tipoRateioId, String tipoRateio, String descricao) {
        this.tipoRateioId = tipoRateioId;
        this.tipoRateio = tipoRateio;
        this.descricao = descricao;
    }

    public String getTipoRateioId() {
        return tipoRateioId;
    }

    public void setTipoRateioId(String tipoRateioId) {
        this.tipoRateioId = tipoRateioId;
    }

    public String getTipoRateio() {
        return tipoRateio;
    }

    public void setTipoRateio(String tipoRateio) {
        this.tipoRateio = tipoRateio;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String toString() {
        return "TipoRateio{" +
                "tipoRateioId='" + tipoRateioId + '\'' +
                ", tipoRateio='" + tipoRateio + '\'' +
                '}';
    }
}
