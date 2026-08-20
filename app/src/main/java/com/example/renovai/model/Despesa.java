package com.example.renovai.model;

import com.renovai.model.enums.TipoDespesa;

public class Despesa {

    private String despesaId;
    private String cooperativaId;
    private String nome;
    private TipoDespesa tipoDespesa;
    private boolean estaAtiva = true;

    public Despesa() {
    }

    public Despesa(String despesaId, String cooperativaId, String nome, TipoDespesa tipoDespesa, boolean estaAtiva) {
        this.despesaId = despesaId;
        this.cooperativaId = cooperativaId;
        this.nome = nome;
        this.tipoDespesa = tipoDespesa;
        this.estaAtiva = estaAtiva;
    }

    public String getDespesaId() {
        return despesaId;
    }

    public void setDespesaId(String despesaId) {
        this.despesaId = despesaId;
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

    public TipoDespesa getTipoDespesa() {
        return tipoDespesa;
    }

    public void setTipoDespesa(TipoDespesa tipoDespesa) {
        this.tipoDespesa = tipoDespesa;
    }

    public boolean isEstaAtiva() {
        return estaAtiva;
    }

    public void setEstaAtiva(boolean estaAtiva) {
        this.estaAtiva = estaAtiva;
    }

    @Override
    public String toString() {
        return "Despesa{" +
                "despesaId='" + despesaId + '\'' +
                ", nome='" + nome + '\'' +
                ", tipoDespesa=" + tipoDespesa +
                '}';
    }
}
