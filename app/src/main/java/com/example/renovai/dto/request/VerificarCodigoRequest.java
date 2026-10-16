package com.example.renovai.dto.request;

/** Corpo do POST /auth/verificar-codigo: confere o código de 6 dígitos sem gastá-lo. */
public class VerificarCodigoRequest {
    private final String email, codigo;
    public VerificarCodigoRequest(String email, String codigo) { this.email = email; this.codigo = codigo; }
    public String getEmail() { return email; }
    public String getCodigo() { return codigo; }
}
