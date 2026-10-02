package com.example.renovai.dto.response;


public class CooperativaPerfilPublicoResponse {
    private String cooperativaId;
    private String nome;
    private String imagemUrl;
    private Double mediaAvaliacoes;
    private Long totalAvaliacoes;

    public String getCooperativaId() { return cooperativaId; }
    public String getNome() { return nome; }
    public String getImagemUrl() { return imagemUrl; }
    public Double getMediaAvaliacoes() { return mediaAvaliacoes; }
    public Long getTotalAvaliacoes() { return totalAvaliacoes; }
}
