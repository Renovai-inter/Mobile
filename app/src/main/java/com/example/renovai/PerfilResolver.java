package com.example.renovai;

import com.example.renovai.dto.response.PerfilResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Descobre o empresaId de quem logou, cruzando o email da sessão com GET /perfis
 * (endpoint que já existe na API — nada foi alterado no backend pra isso).
 *
 * Isso só funciona DEPOIS de um login bem-sucedido (precisa do token já salvo,
 * porque o AuthInterceptor anexa ele em toda chamada). Se a conta logada não tiver
 * nenhum Perfil de empresa vinculado, o resultado vem null — não é um erro.
 */
public final class PerfilResolver {

    public interface Callback {
        void onResultado(String empresaId); // pode vir null
    }

    private PerfilResolver() {
    }

    public static void resolverEmpresaId(String email, Callback callback) {
        if (email == null || email.trim().isEmpty()) {
            callback.onResultado(null);
            return;
        }

        PerfilApiService service = ApiClient.createService(PerfilApiService.class);
        service.listar().enqueue(new retrofit2.Callback<List<PerfilResponse>>() {
            @Override
            public void onResponse(Call<List<PerfilResponse>> call, Response<List<PerfilResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onResultado(null);
                    return;
                }
                for (PerfilResponse perfil : response.body()) {
                    if (email.equalsIgnoreCase(perfil.getEmail()) && perfil.getEmpresaId() != null) {
                        callback.onResultado(perfil.getEmpresaId());
                        return;
                    }
                }
                callback.onResultado(null);
            }

            @Override
            public void onFailure(Call<List<PerfilResponse>> call, Throwable t) {
                callback.onResultado(null);
            }
        });
    }
}
