package com.example.renovai.model;

import com.example.renovai.model.enums.ContatoPreferencial;
import java.util.Date;

public class Cooperativa {

    private String cooperativaId;
    private String nome;
    private String imagemUrl;
    private String descricao;
    private ContatoPreferencial contatoPreferencial;
    private String horarioFuncionamento;
    private int numeroCooperados = 0;
    private Date dataAtualizacao;

    public Cooperativa() {
    }

    public Cooperativa(String cooperativaId, String nome, String imagemUrl, String descricao,
                        ContatoPreferencial contatoPreferencial, String horarioFuncionamento,
                        int numeroCooperados, Date dataAtualizacao) {
        this.cooperativaId = cooperativaId;
        this.nome = nome;
        this.imagemUrl = imagemUrl;
        this.descricao = descricao;
        this.contatoPreferencial = contatoPreferencial;
        this.horarioFuncionamento = horarioFuncionamento;
        this.numeroCooperados = numeroCooperados;
        this.dataAtualizacao = dataAtualizacao;
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

    public String getImagemUrl() {
        return imagemUrl;
    }

    public void setImagemUrl(String imagemUrl) {
        this.imagemUrl = imagemUrl;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public ContatoPreferencial getContatoPreferencial() {
        return contatoPreferencial;
    }

    public void setContatoPreferencial(ContatoPreferencial contatoPreferencial) {
        this.contatoPreferencial = contatoPreferencial;
    }

    public String getHorarioFuncionamento() {
        return horarioFuncionamento;
    }

    public void setHorarioFuncionamento(String horarioFuncionamento) {
        this.horarioFuncionamento = horarioFuncionamento;
    }

    public int getNumeroCooperados() {
        return numeroCooperados;
    }

    public void setNumeroCooperados(int numeroCooperados) {
        this.numeroCooperados = numeroCooperados;
    }

    public Date getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(Date dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    @Override
    public String toString() {
        return "Cooperativa{" +
                "cooperativaId='" + cooperativaId + '\'' +
                ", nome='" + nome + '\'' +
                ", numeroCooperados=" + numeroCooperados +
                '}';
    }
}
