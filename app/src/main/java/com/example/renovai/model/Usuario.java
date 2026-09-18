package com.example.renovai.model;

import java.util.Date;

public class Usuario {

    private String usuarioId;
    private String nome;
    private String email;
    private String cpf;
    private String senha;
    private String senhaHash;
    private String tokenRedefinicao;
    private String imagemUrl;
    private Date dataNascimento;
    private Date ultimoAcesso;
    private Date dataCriacao;
    private Date dataAtualizacao;
    private Date dataTokenExpiracao;
    private boolean estaAtivo = true;

    public Usuario() {
    }

    public Usuario(String usuarioId, String nome, String email, String cpf, String senha,
                    String senhaHash, String tokenRedefinicao, String imagemUrl, Date dataNascimento,
                    Date ultimoAcesso, Date dataCriacao, Date dataAtualizacao,
                    Date dataTokenExpiracao, boolean estaAtivo) {
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.email = email;
        this.cpf = cpf;
        this.senha = senha;
        this.senhaHash = senhaHash;
        this.tokenRedefinicao = tokenRedefinicao;
        this.imagemUrl = imagemUrl;
        this.dataNascimento = dataNascimento;
        this.ultimoAcesso = ultimoAcesso;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.dataTokenExpiracao = dataTokenExpiracao;
        this.estaAtivo = estaAtivo;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public String getTokenRedefinicao() {
        return tokenRedefinicao;
    }

    public void setTokenRedefinicao(String tokenRedefinicao) {
        this.tokenRedefinicao = tokenRedefinicao;
    }

    public String getImagemUrl() {
        return imagemUrl;
    }

    public void setImagemUrl(String imagemUrl) {
        this.imagemUrl = imagemUrl;
    }

    public Date getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(Date dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public Date getUltimoAcesso() {
        return ultimoAcesso;
    }

    public void setUltimoAcesso(Date ultimoAcesso) {
        this.ultimoAcesso = ultimoAcesso;
    }

    public Date getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(Date dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public Date getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(Date dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    public Date getDataTokenExpiracao() {
        return dataTokenExpiracao;
    }

    public void setDataTokenExpiracao(Date dataTokenExpiracao) {
        this.dataTokenExpiracao = dataTokenExpiracao;
    }

    public boolean isEstaAtivo() {
        return estaAtivo;
    }

    public void setEstaAtivo(boolean estaAtivo) {
        this.estaAtivo = estaAtivo;
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "usuarioId='" + usuarioId + '\'' +
                ", nome='" + nome + '\'' +
                ", email='" + email + '\'' +
                ", estaAtivo=" + estaAtivo +
                '}';
    }
}
