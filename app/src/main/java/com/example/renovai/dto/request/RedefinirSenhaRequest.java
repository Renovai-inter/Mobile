package com.example.renovai.dto.request;

public class RedefinirSenhaRequest {
    private final String token, novaSenha;
    public RedefinirSenhaRequest(String token, String novaSenha) { this.token = token; this.novaSenha = novaSenha; }
    public String getToken() { return token; }
    public String getNovaSenha() { return novaSenha; }
}
