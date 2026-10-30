package com.example.renovai;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Cálculo de rota da API de IA (POST /ia/rotas/otimizar): o modelo de tempo de trajeto é treinado com
 * as rotas que o RPA-neo4j sincroniza do Postgres para o Neo4j. Devolve o tempo na ordem da rota,
 * a ordem sugerida e quanto ela economiza. Quando a IA não responde, a tela cai no RotaEstimador.
 */
public interface RotaOtimizadorIaService {

    /**
     * Mesmo host da AWS. O gateway manda todo /rotas/** para a API-Redis, por isso a IA expõe o
     * cálculo também em /ia/rotas/** (repo IA, PR #36).
     */
    String BASE_URL = "http://ad0d8c99545a84713891def8a28f1d46-c7cd4a13dc3dd2e8.elb.us-east-1.amazonaws.com/";

    class Ponto {
        public final String id, endereco, bairro;

        public Ponto(String id, String endereco, String bairro) {
            this.id = id;
            this.endereco = endereco;
            this.bairro = bairro;
        }
    }

    class Pedido {
        @SerializedName("cooperativa_id")
        public final String cooperativaId;

        @SerializedName("rota_id")
        public final String rotaId;

        public final Ponto origem;
        public final List<Ponto> paradas;

        @SerializedName("hora_saida")
        public final int horaSaida;

        /** O grafo é do RPA-neo4j (ele apaga o que não veio do Postgres): a IA só calcula. */
        @SerializedName("salvar_no_neo4j")
        public final boolean salvarNoNeo4j = false;

        public Pedido(String cooperativaId, String rotaId, Ponto origem, List<Ponto> paradas,
                      int horaSaida) {
            this.cooperativaId = cooperativaId;
            this.rotaId = rotaId;
            this.origem = origem;
            this.paradas = paradas;
            this.horaSaida = horaSaida;
        }
    }

    class ParadaOtimizada {
        public Integer ordem;
        public String id, endereco;
    }

    class Resultado {
        @SerializedName("paradas_ordenadas")
        public List<ParadaOtimizada> paradasOrdenadas;

        @SerializedName("distancia_total_km")
        public Double distanciaKm;

        @SerializedName("tempo_total_estimado_min")
        public Double tempoOtimizadoMin;

        @SerializedName("tempo_ordem_original_min")
        public Double tempoOrdemAtualMin;

        @SerializedName("economia_estimada_min")
        public Double economiaMin;
    }

    @POST("ia/rotas/otimizar")
    Call<Resultado> otimizar(@Body Pedido body);

    static RotaOtimizadorIaService criar() {
        return criar(BASE_URL);
    }

    /** Outro servidor (testes contra o modelo de rotas da IA rodando local). */
    static RotaOtimizadorIaService criar(String baseUrl) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .build();
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(RotaOtimizadorIaService.class);
    }
}
