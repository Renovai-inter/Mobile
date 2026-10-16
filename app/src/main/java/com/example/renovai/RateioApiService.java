package com.example.renovai;

import com.example.renovai.dto.response.RateioFuncionarioResponse;
import com.example.renovai.dto.response.RateioListaResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * NÃO existia nenhuma interface para /rateios/** no projeto ainda — criada
 * seguindo com.renovai.api.controller.RateioController (backend).
 *
 * A API não tem um GET /rateios/por-cooperado/{id} (só existe por-cooperativa,
 * pensado para a tela do Gestor — 4.7). Para a tela 2.5 (Rateios Recebidos) do
 * Cooperado, o RateiController (mobile) busca por-cooperativa e depois cruza com
 * a distribuição de cada rateio (mesmo padrão de N chamadas já usado em
 * HomeFragment para cooperativas favoritas da Empresa).
 */
public interface RateioApiService {

    @GET("rateios/por-cooperativa/{cooperativaId}")
    Call<List<RateioListaResponse>> listarPorCooperativa(@Path("cooperativaId") String cooperativaId);

    @GET("rateios/{id}/distribuicao")
    Call<List<RateioFuncionarioResponse>> listarDistribuicao(@Path("id") String rateioId);
}
