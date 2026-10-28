package com.example.renovai.view;

import com.example.renovai.AdminApiService;
import com.example.renovai.ApiClient;
import com.example.renovai.EmpresaData;
import com.example.renovai.R;

import java.util.function.Consumer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Base das abas do Admin do site: mesmo cabeçalho do Gestor, com "Admin Renovaí" no lugar do nome
 * da cooperativa, e um atalho para chamar a API só enquanto a aba está na tela.
 */
public abstract class AdminAbaFragment extends GestorAbaFragment {

    private AdminApiService api;

    protected AdminAbaFragment(int layout) {
        super(layout);
    }

    @Override
    protected String nomeNoCabecalho() {
        return "Admin Renovaí";
    }

    @Override
    protected int abaPerfil() {
        return R.id.nav_a_perfil;
    }

    protected AdminApiService api() {
        if (api == null) api = ApiClient.createService(AdminApiService.class);
        return api;
    }

    /**
     * Faz a chamada e entrega a resposta (o corpo pode ser null, ex.: DELETE) se a aba ainda estiver
     * na tela; em erro mostra a mensagem da API. {@code aoFim} roda nos dois casos.
     */
    protected <T> void chamar(Call<T> call, Consumer<T> aoReceber, Runnable aoFim) {
        call.enqueue(
                new Callback<T>() {
                    @Override
                    public void onResponse(Call<T> c, Response<T> r) {
                        if (!vivo()) return;
                        aoFim.run();
                        if (r.isSuccessful()) aoReceber.accept(r.body());
                        else toast(EmpresaData.erro(r));
                    }

                    @Override
                    public void onFailure(Call<T> c, Throwable t) {
                        if (!vivo()) return;
                        aoFim.run();
                        toast("Sem conexão com o servidor.");
                    }
                });
    }
}
