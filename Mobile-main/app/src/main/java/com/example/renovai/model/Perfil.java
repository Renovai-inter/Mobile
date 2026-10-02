package com.example.renovai.model;

import java.util.Date;

/**
 * Um Perfil pertence exclusivamente a uma Empresa OU a uma Cooperativa
 * (nunca as duas), conforme a constraint chk_perfis_tipo do banco.
 */
public class Perfil {

    private String perfilId;
    private String empresaId;
    private String cooperativaId;
    private String enderecoId;
    private String email;
    private String cnpj;
    private Date dataCriacao;
    private boolean estaAtivo = true;

    public Perfil() {
    }

    public Perfil(String perfilId, String empresaId, String cooperativaId, String enderecoId,
                   String email, String cnpj, Date dataCriacao, boolean estaAtivo) {
        this.perfilId = perfilId;
        this.empresaId = empresaId;
        this.cooperativaId = cooperativaId;
        this.enderecoId = enderecoId;
        this.email = email;
        this.cnpj = cnpj;
        this.dataCriacao = dataCriacao;
        this.estaAtivo = estaAtivo;
    }

    public String getPerfilId() {
        return perfilId;
    }

    public void setPerfilId(String perfilId) {
        this.perfilId = perfilId;
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

    public String getEnderecoId() {
        return enderecoId;
    }

    public void setEnderecoId(String enderecoId) {
        this.enderecoId = enderecoId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public Date getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(Date dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public boolean isEstaAtivo() {
        return estaAtivo;
    }

    public void setEstaAtivo(boolean estaAtivo) {
        this.estaAtivo = estaAtivo;
    }

    @Override
    public String toString() {
        return "Perfil{" +
                "perfilId='" + perfilId + '\'' +
                ", email='" + email + '\'' +
                ", cnpj='" + cnpj + '\'' +
                ", estaAtivo=" + estaAtivo +
                '}';
    }
}
