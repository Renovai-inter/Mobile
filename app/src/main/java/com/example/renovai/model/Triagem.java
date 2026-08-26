package com.example.renovai.model;

import java.math.BigDecimal;
import java.util.Date;

public class Triagem extends EventoOperacional {

    private String equipeId;
    private String coletaId;
    private String materialId;
    private String imagemUrl;
    private BigDecimal quantidadeKg;
    private BigDecimal quantidadeRejeitoKg = BigDecimal.ZERO;

    public Triagem() {
        super();
    }

    public Triagem(String eventoId, Date dataEvento, String statusId, String equipeId,
                    String coletaId, String materialId, String imagemUrl,
                    BigDecimal quantidadeKg, BigDecimal quantidadeRejeitoKg) {
        super(eventoId, dataEvento, statusId);
        this.equipeId = equipeId;
        this.coletaId = coletaId;
        this.materialId = materialId;
        this.imagemUrl = imagemUrl;
        this.quantidadeKg = quantidadeKg;
        this.quantidadeRejeitoKg = quantidadeRejeitoKg;
    }

    public String getEquipeId() {
        return equipeId;
    }

    public void setEquipeId(String equipeId) {
        this.equipeId = equipeId;
    }

    public String getColetaId() {
        return coletaId;
    }

    public void setColetaId(String coletaId) {
        this.coletaId = coletaId;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public String getImagemUrl() {
        return imagemUrl;
    }

    public void setImagemUrl(String imagemUrl) {
        this.imagemUrl = imagemUrl;
    }

    public BigDecimal getQuantidadeKg() {
        return quantidadeKg;
    }

    public void setQuantidadeKg(BigDecimal quantidadeKg) {
        this.quantidadeKg = quantidadeKg;
    }

    public BigDecimal getQuantidadeRejeitoKg() {
        return quantidadeRejeitoKg;
    }

    public void setQuantidadeRejeitoKg(BigDecimal quantidadeRejeitoKg) {
        this.quantidadeRejeitoKg = quantidadeRejeitoKg;
    }

    @Override
    public String toString() {
        return "Triagem{" +
                "eventoId='" + getEventoId() + '\'' +
                ", coletaId='" + coletaId + '\'' +
                ", materialId='" + materialId + '\'' +
                ", quantidadeKg=" + quantidadeKg +
                ", quantidadeRejeitoKg=" + quantidadeRejeitoKg +
                '}';
    }
}
