package com.example.renovai;

import android.util.Log;

import com.example.renovai.view.AdminHomeActivity;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Usuários ativos por dia (DAU): a cada login a API-Redis marca o dia da conta
 * (POST /atividade/login) e depois sincroniza no PostgreSQL. Funcionário da cooperativa (gestor,
 * cooperado, motorista, admin da cooperativa) vai como "cooperado" com o funcionario_id; o perfil da
 * empresa vai como "empresa" com o empresa_id. Falha aqui nunca atrapalha o login.
 */
public final class Atividade {

    private static final String TAG = "Atividade";

    interface Api {
        @POST("atividade/login")
        Call<Void> login(@Body Corpo corpo);
    }

    static final class Corpo {
        final String tipo;
        final String id;

        Corpo(String tipo, String id) {
            this.tipo = tipo;
            this.id = id;
        }
    }

    private static Api api;

    private Atividade() {}

    /** Registra o login da conta da sessão atual (chamado depois de resolver empresa/funcionário). */
    public static void registrarLogin() {
        Corpo corpo =
                daConta(
                        SessionManager.getRole(),
                        CooperadoSession.getFuncionarioId(),
                        SessionManager.getEmpresaId());
        if (corpo == null) return;
        api().login(corpo)
                .enqueue(
                        new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> r) {
                                if (!r.isSuccessful()) Log.w(TAG, "atividade/login: HTTP " + r.code());
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Log.w(TAG, "atividade/login sem resposta: " + t.getClass().getSimpleName());
                            }
                        });
    }

    /** tipo e id da conta, ou null quando ela não entra no DAU (admin do site, sem vínculo). */
    static Corpo daConta(String role, String funcionarioId, String empresaId) {
        if (role == null) return null;
        String r = role.toUpperCase(Locale.ROOT);
        if (AdminHomeActivity.ehAdminDoSite(r)) return null;
        if (r.contains("EMPRESA")) return vazio(empresaId) ? null : new Corpo("empresa", empresaId);
        return vazio(funcionarioId) ? null : new Corpo("cooperado", funcionarioId);
    }

    private static boolean vazio(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static synchronized Api api() {
        if (api == null) {
            OkHttpClient client =
                    new OkHttpClient.Builder()
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .readTimeout(15, TimeUnit.SECONDS)
                            .build();
            // mesmo gateway da viagem do motorista: /atividade/** vai para a API-Redis
            api =
                    new Retrofit.Builder()
                            .baseUrl(RotaViagemApiService.BASE_URL)
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                            .create(Api.class);
        }
        return api;
    }
}
