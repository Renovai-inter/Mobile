package com.example.renovai.model;

import java.util.Date;

public class EmpresaCooperativaFavorita {

    private String favoritoId;
    private String empresaId;
    private String cooperativaId;
    private Date dataCriacao;

    public EmpresaCooperativaFavorita() {
    }

    public EmpresaCooperativaFavorita(String favoritoId, String empresaId, String cooperativaId, Date dataCriacao) {
        this.favoritoId = favoritoId;
        this.empresaId = empresaId;
        this.cooperativaId = cooperativaId;
        this.dataCriacao = dataCriacao;
    }

    public String getFavoritoId() {
        return favoritoId;
    }

    public void setFavoritoId(String favoritoId) {
        this.favoritoId = favoritoId;
    }

    public String getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(String empresaId) {
        this.empresaId = empresaId;
    }

    public String getCooperativaId() {
        return cooperativaId;
    }

    public void setCooperativaId(String cooperativaId) {
        this.cooperativaId = cooperativaId;
    }

    public Date getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(Date dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    @Override
    public String toString() {
        return "EmpresaCooperativaFavorita{" +
                "favoritoId='" + favoritoId + '\'' +
                ", empresaId='" + empresaId + '\'' +
                ", cooperativaId='" + cooperativaId + '\'' +
                '}';
    }
}
