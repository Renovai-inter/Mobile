package com.example.renovai.model;

public class Empresa {

    private String empresaId;
    private String nome;
    private String imagemUrl;
    private String descricao;

    public Empresa() {
    }

    public Empresa(String empresaId, String nome, String imagemUrl, String descricao) {
        this.empresaId = empresaId;
        this.nome = nome;
        this.imagemUrl = imagemUrl;
        this.descricao = descricao;
    }

    public String getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(String empresaId) {
        this.empresaId = empresaId;
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

    @Override
    public String toString() {
        return "Empresa{" +
                "empresaId='" + empresaId + '\'' +
                ", nome='" + nome + '\'' +
                '}';
    }
}
