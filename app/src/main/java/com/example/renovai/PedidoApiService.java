package com.example.renovai;

import com.example.renovai.dto.response.ItemResponse;
import com.example.renovai.dto.response.PedidoCooperativaResponse;
import com.example.renovai.dto.response.PedidoResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/** Consome rotas /pedidos/** já existentes na API. */
public interface PedidoApiService {

    @GET("pedidos/por-empresa/{empresaId}")
    Call<List<PedidoResponse>> listarPorEmpresa(@Path("empresaId") String empresaId);

    @GET("pedidos/{pedidoId}/itens")
    Call<List<ItemResponse>> listarItens(@Path("pedidoId") String pedidoId);

    @GET("pedidos/por-cooperativa/{cooperativaId}")
    Call<List<PedidoCooperativaResponse>> listarPorCooperativa(@Path("cooperativaId") String cooperativaId);
}
