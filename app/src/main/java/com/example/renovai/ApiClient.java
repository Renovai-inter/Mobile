package com.example.renovai;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Ponto único de acesso ao Retrofit. Troque BASE_URL conforme o ambiente:
 *
 *  - Emulador Android acessando a API rodando no SEU computador:
 *      "http://10.0.2.2:8080/api/"
 *    (10.0.2.2 é o alias que o emulador usa para "localhost" da máquina host)
 *
 *  - Celular físico na MESMA rede Wi-Fi do computador que roda a API:
 *      "http://<IP-da-sua-máquina-na-rede>:8080/api/"
 *    (descubra o IP com `ipconfig` no Windows ou `ifconfig`/`ip a` no
 *    Linux/Mac; algo como 192.168.0.x)
 *
 *  - API publicada em produção (Render, Railway, EC2, etc.):
 *      "https://sua-api.dominio.com/api/"
 *
 * Repare na barra final "/" — é exigida pelo Retrofit.
 */
public final class ApiClient {

    private static final String BASE_URL = "http://10.0.2.2:8080/api/";

    private static Retrofit retrofit;

    private ApiClient() {
    }

    public static Retrofit getRetrofit() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor())
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static <T> T createService(Class<T> serviceClass) {
        return getRetrofit().create(serviceClass);
    }
}
