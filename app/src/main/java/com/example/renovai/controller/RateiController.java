package com.example.renovai.controller;

import com.example.renovai.ApiClient;
import com.example.renovai.RateioApiService;
import com.example.renovai.adapter.RateiListAdapter;
import com.example.renovai.dto.response.RateioFuncionarioResponse;
import com.example.renovai.dto.response.RateioListaResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controller de Rateio para a área de Cooperado (tela 2.5 — Rateios Recebidos).
 *
 * <p>A API não tem um GET /rateios/por-cooperado/{id} (só existe por-cooperativa, pensado para a
 * tela do Gestor). Esta classe busca todos os rateios da cooperativa do cooperado e, para cada um,
 * busca a distribuição individual (GET /rateios/{id}/distribuicao), filtrando pelo funcionarioId do
 * cooperado logado — mesmo padrão de N chamadas em paralelo já usado em HomeFragment (Empresa) para
 * cruzar cooperativas favoritas.
 */
public class RateiController {

    public interface ListaCallback {
        void onSuccess(List<RateiListAdapter.Item> itens);

        void onErro(String mensagem);
    }

    private final RateioApiService service;

    public RateiController() {
        this.service = ApiClient.createService(RateioApiService.class);
    }

    public void listarRecebidosPorCooperado(
            String cooperativaId, String funcionarioId, ListaCallback callback) {
        if (cooperativaId == null || funcionarioId == null) {
            callback.onErro("Não foi possível identificar o cooperado logado.");
            return;
        }

        service.listarPorCooperativa(cooperativaId)
                .enqueue(
                        new Callback<List<RateioListaResponse>>() {
                            @Override
                            public void onResponse(
                                    Call<List<RateioListaResponse>> call,
                                    Response<List<RateioListaResponse>> response) {
                                if (!response.isSuccessful() || response.body() == null) {
                                    callback.onErro(
                                            "Erro " + response.code() + " ao carregar rateios.");
                                    return;
                                }

                                List<RateioListaResponse> rateios = response.body();
                                if (rateios.isEmpty()) {
                                    callback.onSuccess(new ArrayList<>());
                                    return;
                                }

                                List<RateiListAdapter.Item> resultado = new ArrayList<>();
                                AtomicInteger pendentes = new AtomicInteger(rateios.size());
                                AtomicInteger falhas = new AtomicInteger();

                                for (RateioListaResponse rateio : rateios) {
                                    service.listarDistribuicao(rateio.getRateioId())
                                            .enqueue(
                                                    new Callback<
                                                            List<RateioFuncionarioResponse>>() {
                                                        @Override
                                                        public void onResponse(
                                                                Call<
                                                                                List<
                                                                                        RateioFuncionarioResponse>>
                                                                        call,
                                                                Response<
                                                                                List<
                                                                                        RateioFuncionarioResponse>>
                                                                        response) {
                                                            if (response.isSuccessful()
                                                                    && response.body() != null) {
                                                                for (RateioFuncionarioResponse
                                                                        distribuicao :
                                                                                response.body()) {
                                                                    if (funcionarioId
                                                                            .equalsIgnoreCase(
                                                                                    distribuicao
                                                                                            .getFuncionarioId())) {
                                                                        resultado.add(
                                                                                new RateiListAdapter
                                                                                        .Item(
                                                                                        rateio
                                                                                                .getRateioId(),
                                                                                        rateio
                                                                                                .getValorTotalDistribuido(),
                                                                                        distribuicao
                                                                                                .getValorRateio(),
                                                                                        RateiListAdapter
                                                                                                .StatusRateio
                                                                                                .FECHADO,
                                                                                        rateio
                                                                                                .getDataRateio()));
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                            if (!response.isSuccessful()
                                                                    || response.body() == null)
                                                                falhas.incrementAndGet();
                                                            finalizarSeCompleto();
                                                        }

                                                        @Override
                                                        public void onFailure(
                                                                Call<
                                                                                List<
                                                                                        RateioFuncionarioResponse>>
                                                                        call,
                                                                Throwable t) {
                                                            falhas.incrementAndGet();
                                                            finalizarSeCompleto();
                                                        }

                                                        private void finalizarSeCompleto() {
                                                            if (pendentes.decrementAndGet() == 0) {
                                                                resultado.sort(
                                                                        (a, b) -> {
                                                                            if (a.dataRateioIso
                                                                                            == null
                                                                                    || b.dataRateioIso
                                                                                            == null)
                                                                                return 0;
                                                                            return b.dataRateioIso
                                                                                    .compareTo(
                                                                                            a.dataRateioIso);
                                                                        });
                                                                if (falhas.get() > 0)
                                                                    callback.onErro(
                                                                            "Não foi possível"
                                                                                + " consultar todas"
                                                                                + " as distribuições."
                                                                                + " Tente atualizar"
                                                                                + " novamente.");
                                                                else callback.onSuccess(resultado);
                                                            }
                                                        }
                                                    });
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<List<RateioListaResponse>> call, Throwable t) {
                                callback.onErro("Não foi possível conectar ao servidor.");
                            }
                        });
    }
}
