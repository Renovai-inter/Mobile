package com.example.renovai;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Chat de suporte com o NIMO (SuporteController da API). A API decide pelo perfil do login o que
 * o assistente pode responder: análises dos dados (gestor, empresa e admin) ou só dúvidas gerais
 * e FAQ (cooperado e motorista).
 */
public interface SuporteApiService {

    class Pergunta {
        public final String sessaoId, pergunta;

        public Pergunta(String sessaoId, String pergunta) {
            this.sessaoId = sessaoId;
            this.pergunta = pergunta;
        }
    }

    class Resposta {
        public String resposta;
    }

    @POST("suporte/chat")
    Call<Resposta> perguntar(@Body Pergunta body);

    /** Fim da conversa: a IA resume e guarda na memória da conta. */
    @POST("suporte/chat/{sessaoId}/encerrar")
    Call<Void> encerrar(@Path("sessaoId") String sessaoId);
}
