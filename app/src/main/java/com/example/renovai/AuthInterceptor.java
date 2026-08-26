package com.example.renovai;

import androidx.annotation.NonNull;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Adiciona automaticamente o header "Authorization: Bearer <token>" em toda
 * requisição, quando existir um token salvo. Espelha exatamente o que o
 * JwtAuthenticationFilter da API espera (ver com.renovai.api.security).
 *
 * A rota /auth/login não precisa de token (é pública no SecurityConfig),
 * então não tem problema mandar o header vazio nela também — o filtro do
 * backend simplesmente ignora quando não há token.
 */
public class AuthInterceptor implements Interceptor {

    @NonNull
    @Override
    public Response intercept(@NonNull Interceptor.Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String token = SessionManager.getToken();

        if (token == null) {
            return chain.proceed(originalRequest);
        }

        Request authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();

        return chain.proceed(authenticatedRequest);
    }
}
