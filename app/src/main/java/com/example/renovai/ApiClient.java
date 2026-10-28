package com.example.renovai;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    /** Servidor principal (AWS). */
    static final String BASE_URL =
            "http://ad0d8c99545a84713891def8a28f1d46-c7cd4a13dc3dd2e8.elb.us-east-1.amazonaws.com/api/";
    /** Reserva (Render), usado quando a AWS não responde. */
    static final String FALLBACK_URL =
            "https://spring-api-2t71.onrender.com/api/";

    private static Retrofit retrofit;
    private static OkHttpClient client;

    private ApiClient() {
    }

    public static Retrofit getRetrofit() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            client = new OkHttpClient.Builder()
                    .addInterceptor(new ServidorFallbackInterceptor(BASE_URL, FALLBACK_URL))
                    .addInterceptor(new AuthInterceptor())
                    .addInterceptor(logging)
                    .connectTimeout(100, TimeUnit.SECONDS)
                    .readTimeout(170, TimeUnit.SECONDS) // o Render gratuito pode levar mais de 1 min para acordar
                    .writeTimeout(100, TimeUnit.SECONDS)
                    .callTimeout(180, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit;
    }

    /**
     * "Acorda" o servidor sem esperar resposta. Se a AWS estiver fora, o ping cai no Render, que
     * dorme quando fica parado e leva 1 a 3 minutos para voltar; chamando isso ao abrir o app, ele
     * já está de pé quando a pessoa terminar de digitar.
     */
    public static void acordarServidor() {
        getRetrofit();
        Request ping = new Request.Builder().url(BASE_URL + "actuator/health").build();
        client.newCall(ping).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Sem internet ou servidor fora: a próxima chamada de verdade mostra o erro.
            }

            @Override
            public void onResponse(Call call, Response response) {
                response.close();
            }
        });
    }

    public static <T> T createService(Class<T> serviceClass) {
        return getRetrofit().create(serviceClass);
    }
}