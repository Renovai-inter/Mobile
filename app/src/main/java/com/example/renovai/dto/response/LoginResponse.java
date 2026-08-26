package com.example.renovai.dto.response;

/** Espelha Responses.LoginResponse. Retorno de POST /auth/login. */
public class LoginResponse {
    private String token;
    private String tipo;
    private String email;
    private String role;

    public String getToken() { return token; }
    public String getTipo() { return tipo; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}
