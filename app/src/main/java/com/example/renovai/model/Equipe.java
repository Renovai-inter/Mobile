package com.example.renovai.model;

import java.util.Date;

public class Equipe {

    private String equipeId;
    private String gestorId;
    private String nome;
    private String descricao;
    private Date dataCriacao;
    private boolean estaAtiva = true;

    public Equipe() {
    }

    public Equipe(String equipeId, String gestorId, String nome, String descricao,
                   Date dataCriacao, boolean estaAtiva) {
        this.equipeId = equipeId;
        this.gestorId = gestorId;
        this.nome = nome;
        this.descricao = descricao;
        this.dataCriacao = dataCriacao;
        this.estaAtiva = estaAtiva;
    }

    public String getEquipeId() {
        return equipeId;
    }

    public void setEquipeId(String equipeId) {
        this.equipeId = equipeId;
    }

    public String getGestorId() {
        return gestorId;
    }

    public void setGestorId(String gestorId) {
        this.gestorId = gestorId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Date getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(Date dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public boolean isEstaAtiva() {
        return estaAtiva;
    }

    public void setEstaAtiva(boolean estaAtiva) {
        this.estaAtiva = estaAtiva;
    }

    @Override
    public String toString() {
        return "Equipe{" +
                "equipeId='" + equipeId + '\'' +
                ", nome='" + nome + '\'' +
                ", estaAtiva=" + estaAtiva +
                '}';
    }
}
