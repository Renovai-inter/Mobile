package com.example.renovai.dto.request;

import java.math.BigDecimal;

/**
 * Espelha Requests.CooperativaRequest do backend.
 * Usado em POST /cooperativas e PUT /cooperativas/{id}.
 */
public class CooperativaRequest {

    private String nome;
    private String descricao;
    private Integer numeroCooperados;
    private String horarioFuncionamento;
    private String imagemUrl;
    private String contatoPreferencial; // "WHATSAPP" | "TELEFONE" | "EMAIL"

    public CooperativaRequest() {
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

    public Integer getNumeroCooperados() {
        return numeroCooperados;
    }

    public void setNumeroCooperados(Integer numeroCooperados) {
        this.numeroCooperados = numeroCooperados;
    }

    public String getHorarioFuncionamento() {
        return horarioFuncionamento;
    }

    public void setHorarioFuncionamento(String horarioFuncionamento) {
        this.horarioFuncionamento = horarioFuncionamento;
    }

    public String getImagemUrl() {
        return imagemUrl;
    }

    public void setImagemUrl(String imagemUrl) {
        this.imagemUrl = imagemUrl;
    }

    public String getContatoPreferencial() {
        return contatoPreferencial;
    }

    public void setContatoPreferencial(String contatoPreferencial) {
        this.contatoPreferencial = contatoPreferencial;
    }
}
