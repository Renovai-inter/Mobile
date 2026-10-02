package com.example.renovai.dto.request;

public class EsqueciSenhaRequest {
    private final String email;
    public EsqueciSenhaRequest(String email) { this.email = email; }
    public String getEmail() { return email; }
}
