package com.example.renovai.model;

import java.math.BigDecimal;

public class Material {

    private String materialId;
    private String categoriaId;
    private String cooperativaId;
    private String imagemUrl;
    private BigDecimal precoSugerido;
    private boolean estaDisponivel = true;

    public Material() {
    }

    public Material(String materialId, String categoriaId, String cooperativaId, String imagemUrl,
                     BigDecimal precoSugerido, boolean estaDisponivel) {
        this.materialId = materialId;
        this.categoriaId = categoriaId;
        this.cooperativaId = cooperativaId;
        this.imagemUrl = imagemUrl;
        this.precoSugerido = precoSugerido;
        this.estaDisponivel = estaDisponivel;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public String getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(String categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getCooperativaId() {
        return cooperativaId;
    }

    public void setCooperativaId(String cooperativaId) {
        this.cooperativaId = cooperativaId;
    }

    public String getImagemUrl() {
        return imagemUrl;
    }

    public void setImagemUrl(String imagemUrl) {
        this.imagemUrl = imagemUrl;
    }

    public BigDecimal getPrecoSugerido() {
        return precoSugerido;
    }

    public void setPrecoSugerido(BigDecimal precoSugerido) {
        this.precoSugerido = precoSugerido;
    }

    public boolean isEstaDisponivel() {
        return estaDisponivel;
    }

    public void setEstaDisponivel(boolean estaDisponivel) {
        this.estaDisponivel = estaDisponivel;
    }

    @Override
    public String toString() {
        return "Material{" +
                "materialId='" + materialId + '\'' +
                ", categoriaId='" + categoriaId + '\'' +
                ", cooperativaId='" + cooperativaId + '\'' +
                ", precoSugerido=" + precoSugerido +
                ", estaDisponivel=" + estaDisponivel +
                '}';
    }
}
