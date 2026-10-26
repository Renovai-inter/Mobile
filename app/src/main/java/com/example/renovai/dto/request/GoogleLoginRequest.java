package com.example.renovai.dto.request;

/** POST /auth/google: o ID token que o Google devolveu ao app. */
public class GoogleLoginRequest {
    private final String idToken;
    public GoogleLoginRequest(String idToken) { this.idToken = idToken; }
    public String getIdToken() { return idToken; }
}
