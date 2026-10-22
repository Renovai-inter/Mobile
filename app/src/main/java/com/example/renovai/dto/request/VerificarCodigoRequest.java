package com.example.renovai.dto.request;

public class VerificarCodigoRequest {
    private final String email, codigo;
    public VerificarCodigoRequest(String email, String codigo) { this.email = email; this.codigo = codigo; }
    public String getEmail() { return email; }
    public String getCodigo() { return codigo; }
}
