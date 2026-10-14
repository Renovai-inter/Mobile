package com.example.renovai.dto.request;

/**
 * Corpo do POST /auth/redefinir-senha. Com {@code email}, o {@code token} é o código de 6
 * dígitos enviado por e-mail.
 */
public class RedefinirSenhaRequest {
    private final String token, novaSenha, email;
    public RedefinirSenhaRequest(String email, String token, String novaSenha) { this.email = email; this.token = token; this.novaSenha = novaSenha; }
    public String getToken() { return token; }
    public String getNovaSenha() { return novaSenha; }
    public String getEmail() { return email; }
}
