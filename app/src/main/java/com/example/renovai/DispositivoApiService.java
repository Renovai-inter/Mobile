package com.example.renovai;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

/** Registro do aparelho para as notificações push (DispositivoController da API). */
public interface DispositivoApiService {

    class Dispositivo {
        public final String token;

        public Dispositivo(String token) {
            this.token = token;
        }
    }

    /** Liga o aparelho à conta logada (se outra conta usava o aparelho, ela deixa de receber). */
    @POST("dispositivos")
    Call<Void> registrar(@Body Dispositivo body);

    /** No logout a sessão já foi limpa, então o JWT vai explícito. */
    @DELETE("dispositivos")
    Call<Void> remover(@Header("Authorization") String bearer, @Query("token") String token);
}
