package com.example.renovai;

import com.example.renovai.dto.response.RotaResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * NÃO existia nenhuma interface para /rotas/** no projeto ainda — criada
 * seguindo com.renovai.api.controller.RotaController (backend). Só expõe o
 * endpoint usado pelo seletor "Rota de origem" da tela 2.2 (coleta Externa).
 */
public interface RotaApiService {

    @GET("rotas/ativas/por-cooperativa/{cooperativaId}")
    Call<List<RotaResponse>> listarAtivasPorCooperativa(@Path("cooperativaId") String cooperativaId);
}
