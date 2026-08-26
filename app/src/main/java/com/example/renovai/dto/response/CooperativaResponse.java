package com.example.renovai.dto.response;

/** Espelha Responses.CooperativaResponse. */
public class CooperativaResponse {
    private String cooperativaId;
    private String nome;
    private String descricao;
    private Integer numeroCooperados;
    private String horarioFuncionamento;
    private String imagemUrl;
    private String contatoPreferencial;
    private String cidade;

    public String getCooperativaId() { return cooperativaId; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public Integer getNumeroCooperados() { return numeroCooperados; }
    public String getHorarioFuncionamento() { return horarioFuncionamento; }
    public String getImagemUrl() { return imagemUrl; }
    public String getContatoPreferencial() { return contatoPreferencial; }
    public String getCidade() { return cidade; }
}
