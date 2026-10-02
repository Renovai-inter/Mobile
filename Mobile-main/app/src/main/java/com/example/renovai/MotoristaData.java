package com.example.renovai;

import com.example.renovai.dto.response.GestorResponses;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

/** Camada de dados do Motorista: mesmo padrão cache-primeiro-depois-atualiza do Gestor/Empresa. */
public final class MotoristaData {

    private MotoristaData() {}

    public static MotoristaApiService api() {
        return ApiClient.createService(MotoristaApiService.class);
    }

    public static String coop() {
        return CooperadoSession.getCooperativaId();
    }

    public static void rotas(boolean forcar, GestorData.Ouvinte<List<GestorResponses.Rota>> o) {
        if (coop() == null) {
            o.aoErro("Não foi possível identificar a cooperativa. Faça login novamente.");
            return;
        }
        final List<GestorResponses.Rota> cache = GestorCache.obter("motorista:rotas");
        final long sessao = GestorCache.sessao();
        if (cache != null) o.aoReceber(cache, true);
        if (cache != null && !forcar && GestorCache.valido("motorista:rotas")) return;

        api().rotas(coop())
                .enqueue(
                        new Callback<List<GestorResponses.Rota>>() {
                            @Override
                            public void onResponse(
                                    Call<List<GestorResponses.Rota>> call,
                                    Response<List<GestorResponses.Rota>> r) {
                                if (sessao != GestorCache.sessao()) return;
                                if (r.isSuccessful() && r.body() != null) {
                                    GestorCache.guardar("motorista:rotas", r.body());
                                    o.aoReceber(r.body(), false);
                                } else if (cache == null) {
                                    o.aoErro("Erro " + r.code() + " ao carregar as rotas.");
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<List<GestorResponses.Rota>> call, Throwable t) {
                                if (sessao != GestorCache.sessao()) return;
                                if (cache == null) o.aoErro("Sem conexão com o servidor.");
                            }
                        });
    }

    public static void invalidar() {
        GestorCache.invalidar("motorista:rotas");
    }
}
