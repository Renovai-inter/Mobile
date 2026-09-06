package com.example.renovai;

import androidx.annotation.NonNull;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    @NonNull
    @Override
    public Response intercept(@NonNull Interceptor.Chain chain)
            throws IOException {

        Request originalRequest = chain.request();

        if (originalRequest.url().encodedPath().endsWith("/auth/login")) {
            return chain.proceed(originalRequest);
        }

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