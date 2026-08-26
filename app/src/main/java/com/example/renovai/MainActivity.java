package com.example.renovai;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.renovai.dto.request.LoginRequest;
import com.example.renovai.dto.response.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Proteção contra NullPointerException se o layout 'main' não existir
        if (findViewById(R.id.main) != null) {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        AuthApiService authService = ApiClient.createService(AuthApiService.class);

        LoginRequest request = new LoginRequest("gestor@cooperativa.com", "senha123");

        // Corrigido: usando retrofit2.Callback e o DTO correto
        authService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("RENOVAI_TESTE", "Login OK! Token: " + response.body().getToken());
                } else {
                    Log.e("RENOVAI_TESTE", "Erro: código " + response.code());
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.e("RENOVAI_TESTE", "Falha de conexão: " + t.getMessage());
            }
        });
    }
}
