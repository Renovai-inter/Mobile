package com.example.renovai;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Andamento da viagem do motorista na API-Redis (repo API-Redis, Flask). A rota em si continua na
 * Spring API; aqui fica só a sessão da viagem (iniciada/finalizada) e o status de cada parada de
 * coleta, por 12 h. Ao finalizar, a API-Redis grava a execução no Postgres (rota_execucoes) e
 * apaga a sessão do Redis.
 *
 * <p>As paradas são numeradas pela API na ordem enviada (0, 1, 2…): o app manda só as paradas de
 * coleta, na ordem da rota, e usa esse índice para casar com a lista da tela.
 */
public interface RotaViagemApiService {

    /** O gateway da AWS manda /rotas/** para a API-Redis (a Spring API fica em /api/rotas). */
    String BASE_URL = "http://ad0d8c99545a84713891def8a28f1d46-c7cd4a13dc3dd2e8.elb.us-east-1.amazonaws.com/";

    /** SINCRONIZADA: a API-Redis já gravou a viagem no Postgres e só falta apagar a sessão. */
    String INICIADA = "INICIADA", FINALIZADA = "FINALIZADA", SINCRONIZADA = "SINCRONIZADA";
    String PENDENTE = "PENDENTE", CONCLUIDA = "CONCLUIDA";

    class Inicio {
        @SerializedName("rota_id")
        public final String rotaId;

        /** funcionario_id do motorista (FK de rota_execucoes no Postgres). */
        @SerializedName("motorista_id")
        public final String motoristaId;

        public Inicio(String rotaId, String motoristaId) {
            this.rotaId = rotaId;
            this.motoristaId = motoristaId;
        }
    }

    class Parada {
        public String id, endereco, status;

        @SerializedName("peso_estimado")
        public String pesoEstimado;

        /** rotas_enderecos.endereco_id: liga a viagem ao ponto do grafo (tempo real do trecho). */
        @SerializedName("endereco_id")
        public String enderecoId;

        /** Posição na rota da Spring API (a API-Redis devolve como texto). */
        public String ordem;

        /** Hora em que a coleta foi concluída (só nas paradas CONCLUIDA). */
        @SerializedName("timestamp_conclusao")
        public String conclusao;

        public Parada(String endereco) {
            this.endereco = endereco;
        }

        public Parada(String endereco, String enderecoId, Integer ordem) {
            this.endereco = endereco;
            this.enderecoId = enderecoId;
            this.ordem = ordem == null ? null : String.valueOf(ordem);
        }
    }

    class Paradas {
        public List<Parada> paradas;

        public Paradas(List<Parada> paradas) {
            this.paradas = paradas;
        }
    }

    class NovoStatus {
        public final String status;

        /** A API recusa (403) a alteração de quem não iniciou a viagem. */
        @SerializedName("motorista_id")
        public final String motoristaId;

        public NovoStatus(String status) {
            this(status, null);
        }

        public NovoStatus(String status, String motoristaId) {
            this.status = status;
            this.motoristaId = motoristaId;
        }
    }

    class Motorista {
        @SerializedName("motorista_id")
        public final String motoristaId;

        public Motorista(String motoristaId) {
            this.motoristaId = motoristaId;
        }
    }

    /** A API-Redis devolve a sessão como hash do Redis: tudo texto. */
    class Sessao {
        public String status;

        @SerializedName("motorista_id")
        public String motoristaId;

        @SerializedName("timestamp_inicio")
        public String inicio;
    }

    @POST("rotas")
    Call<Map<String, Object>> iniciar(@Body Inicio body);

    /** 404 = a viagem não foi iniciada (ou já foi finalizada e sincronizada). */
    @GET("rotas/{id}")
    Call<Sessao> sessao(@Path("id") String rotaId);

    @POST("rotas/{id}/paradas")
    Call<Map<String, Object>> adicionarParadas(@Path("id") String rotaId, @Body Paradas body);

    @GET("rotas/{id}/paradas")
    Call<Paradas> paradas(@Path("id") String rotaId);

    @PUT("rotas/{id}/parada/{indice}")
    Call<Parada> atualizarParada(
            @Path("id") String rotaId, @Path("indice") int indice, @Body NovoStatus body);

    @PUT("rotas/{id}/finalizar")
    Call<Sessao> finalizar(@Path("id") String rotaId);

    @PUT("rotas/{id}/finalizar")
    Call<Sessao> finalizar(@Path("id") String rotaId, @Body Motorista body);

    static RotaViagemApiService criar() {
        return criar(BASE_URL);
    }

    /** Outro servidor (testes contra a API-Redis local). */
    static RotaViagemApiService criar(String baseUrl) {
        return criar(baseUrl, null);
    }

    /** {@code callbacks} != null: executa as respostas nesse executor (testes fora do Android). */
    static RotaViagemApiService criar(String baseUrl, Executor callbacks) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        Retrofit.Builder b = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create());
        if (callbacks != null) b.callbackExecutor(callbacks);
        return b.build().create(RotaViagemApiService.class);
    }
}
