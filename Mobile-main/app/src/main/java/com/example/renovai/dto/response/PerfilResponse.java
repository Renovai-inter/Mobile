package com.example.renovai.dto.response;


public class PerfilResponse {
    private String perfilId;
    private String email;
    private String cnpj;
    private Boolean estaAtivo;
    private String dataCriacao;
    private String empresaId;
    private String empresaNome;
    private String cooperativaId;
    private String cooperativaNome;

    public String getPerfilId() { return perfilId; }
    public String getEmail() { return email; }
    public String getCnpj() { return cnpj; }
    public Boolean getEstaAtivo() { return estaAtivo; }
    public String getEmpresaId() { return empresaId; }
    public String getEmpresaNome() { return empresaNome; }
    public String getCooperativaId() { return cooperativaId; }
    public String getCooperativaNome() { return cooperativaNome; }
}
