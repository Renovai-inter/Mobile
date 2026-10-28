package com.example.renovai;

import com.example.renovai.dto.response.CooperativaResponse;
import com.example.renovai.dto.response.EmpresaResponse;
import com.example.renovai.dto.response.PedidoResponse;
import com.example.renovai.dto.response.PerfilResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Rotas que o Admin do site (role ADMIN_SITE) usa: a API dá a ele acesso a cooperativas,
 * empresas, perfis (contas) e pedidos de todo o sistema (ver SecurityConfig no backend).
 */
public interface AdminApiService {

    @GET("cooperativas")
    Call<List<CooperativaResponse>> cooperativas();

    @DELETE("cooperativas/{id}")
    Call<Void> excluirCooperativa(@Path("id") String id);

    @GET("empresas")
    Call<List<EmpresaResponse>> empresas();

    @DELETE("empresas/{id}")
    Call<Void> excluirEmpresa(@Path("id") String id);

    @GET("perfis")
    Call<List<PerfilResponse>> perfis();

    @PUT("perfis/{id}/desativar")
    Call<PerfilResponse> desativarPerfil(@Path("id") String id);

    @DELETE("perfis/{id}")
    Call<Void> excluirPerfil(@Path("id") String id);

    @GET("pedidos")
    Call<List<PedidoResponse>> pedidos();
}
