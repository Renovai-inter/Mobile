package com.example.renovai.model;

public class Rota {

    private String rotaId;
    private String cooperativaId;
    private String nome;
    private boolean estaAtiva = true;

    public Rota() {
    }

    public Rota(String rotaId, String cooperativaId, String nome, boolean estaAtiva) {
        this.rotaId = rotaId;
        this.cooperativaId = cooperativaId;
        this.nome = nome;
        this.estaAtiva = estaAtiva;
    }

    public String getRotaId() {
        return rotaId;
    }

    public void setRotaId(String rotaId) {
        this.rotaId = rotaId;
    }

    public String getCooperativaId() {
        return cooperativaId;
    }

    public void setCooperativaId(String cooperativaId) {
        this.cooperativaId = cooperativaId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public boolean isEstaAtiva() {
        return estaAtiva;
    }

    public void setEstaAtiva(boolean estaAtiva) {
        this.estaAtiva = estaAtiva;
    }

    @Override
    public String toString() {
        return "Rota{" +
                "rotaId='" + rotaId + '\'' +
                ", nome='" + nome + '\'' +
                ", estaAtiva=" + estaAtiva +
                '}';
    }
}
