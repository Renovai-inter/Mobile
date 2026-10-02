package com.example.renovai;

import com.example.renovai.dto.response.CooperativaPerfilPublicoResponse;
import com.example.renovai.dto.response.FavoritoResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface FavoritoApiService {

    @GET("favoritos/por-empresa/{empresaId}")
    Call<List<FavoritoResponse>> listarPorEmpresa(@Path("empresaId") String empresaId);

    @GET("cooperativas/{id}/perfil-publico")
    Call<CooperativaPerfilPublicoResponse> perfilPublico(@Path("id") String id);
}
