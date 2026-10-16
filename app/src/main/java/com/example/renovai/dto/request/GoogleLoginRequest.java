package com.example.renovai.dto.request;

/** Corpo do POST /auth/google: o ID token que o app recebeu do Google. */
public class GoogleLoginRequest {
    private final String idToken;
    public GoogleLoginRequest(String idToken) { this.idToken = idToken; }
    public String getIdToken() { return idToken; }
}
