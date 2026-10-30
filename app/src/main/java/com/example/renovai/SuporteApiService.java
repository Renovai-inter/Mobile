package com.example.renovai;

import com.google.gson.annotations.SerializedName;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Chat de suporte com o NIMO, direto na API de IA (repo IA, FastAPI). A IA só responde sobre a
 * cooperativa ou a empresa do id enviado, e consulta o banco por conta própria.
 */
public interface SuporteApiService {

    /** A IA fica no mesmo gateway da AWS que a Spring API, mas na raiz (/chat, /sessions). */
    String BASE_URL = "http://ad0d8c99545a84713891def8a28f1d46-c7cd4a13dc3dd2e8.elb.us-east-1.amazonaws.com/";

    /**
     * Vai nos dois formatos da IA: o antigo (só perfil_id, o perfis.perfil_id da conta) e o novo
     * (cooperativa_id ou empresa_id). Cada versão ignora os campos que não conhece.
     */
    class Pergunta {
        @SerializedName("session_id")
        public final String sessaoId;

        @SerializedName("perfil_id")
        public final String perfilId;

        @SerializedName("cooperativa_id")
        public final String cooperativaId;

        @SerializedName("empresa_id")
        public final String empresaId;

        /** Chave da memória da conversa na IA (o e-mail do login). */
        @SerializedName("user_id")
        public final String usuarioId;

        public final String pergunta;

        public Pergunta(String sessaoId, String perfilId, String cooperativaId, String empresaId,
                        String usuarioId, String pergunta) {
            this.sessaoId = sessaoId;
            this.perfilId = perfilId;
            this.cooperativaId = cooperativaId;
            this.empresaId = empresaId;
            this.usuarioId = usuarioId;
            this.pergunta = pergunta;
        }
    }

    class Resposta {
        public String resposta;
    }

    @POST("chat")
    Call<Resposta> perguntar(@Body Pergunta body);

    /** Fim da conversa: a IA resume e guarda na memória da conta. */
    @POST("sessions/{sessaoId}/encerrar")
    Call<Void> encerrar(@Path("sessaoId") String sessaoId);

    /** Cliente próprio: a IA passa por vários agentes e pode levar bem mais que a Spring API. */
    static SuporteApiService criar() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .callTimeout(200, TimeUnit.SECONDS)
                .build();
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SuporteApiService.class);
    }
}
