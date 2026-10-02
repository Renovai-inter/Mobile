package com.example.renovai.dto.response;

public class CadastroEmpresaResponse {
    private String token;
    private String tipo;
    private String email;
    private String role;
    private String empresaId;

    public String getToken() { return token; }
    public String getTipo() { return tipo; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getEmpresaId() { return empresaId; }
}
