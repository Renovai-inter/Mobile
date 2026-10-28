package com.example.renovai.dto.request;

/** token = código de 6 dígitos que a API enviou por e-mail no POST /auth/esqueci-senha. */
public class RedefinirSenhaRequest {
    private final String email, token, novaSenha;
    public RedefinirSenhaRequest(String email, String token, String novaSenha) { this.email = email; this.token = token; this.novaSenha = novaSenha; }
    public String getEmail() { return email; }
    public String getToken() { return token; }
    public String getNovaSenha() { return novaSenha; }
}
