package com.example.renovai;

import com.example.renovai.dto.request.EmpresaRequests;
import com.example.renovai.dto.response.CategoriaMaterialResponse;
import com.example.renovai.dto.response.CooperativaResponse;
import com.example.renovai.dto.response.EmpresaResponse;
import com.example.renovai.dto.response.EmpresaResponses;
import com.example.renovai.dto.response.GestorResponses;
import com.example.renovai.dto.response.ItemResponse;
import com.example.renovai.dto.response.MaterialResponse;
import com.example.renovai.dto.response.PedidoCooperativaResponse;
import com.example.renovai.dto.response.PedidoResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;

/**
 * Todos os endpoints usados pela área da Empresa (telas 5.1 a 5.6), no mesmo espírito do
 * GestorApiService: uma interface só, reunindo tudo que a área precisa. Continua expondo
 * buscarPorId (já existia antes desta área e é usado pela EmpresaActivity original).
 */
public interface EmpresaApiService {

    @GET("empresas/{id}")
    Call<EmpresaResponse> buscarPorId(@Path("id") String id);

    @GET("empresas-conta/dashboard")
    Call<EmpresaResponses.Dashboard> dashboard();

    @PUT("empresas/{id}")
    Call<EmpresaResponse> atualizarEmpresa(
            @Path("id") String id, @Body EmpresaRequests.Empresa body);

    // ── Conta da empresa (patch: /empresas-conta/meu-perfil) ──
    @GET("empresas-conta/meu-perfil")
    Call<EmpresaResponses.MeuPerfil> meuPerfil();

    @PATCH("empresas-conta/meu-perfil")
    Call<EmpresaResponses.MeuPerfil> atualizarMeuPerfil(
            @Body EmpresaRequests.AtualizarMeuPerfil body);

    // ── Cooperativas ──
    @GET("empresas-conta/cooperativas")
    Call<List<CooperativaResponse>> buscarCooperativas(
            @Query("categoriaId") String categoriaId,
            @Query("cidade") String cidade,
            @Query("quantidadeMin") String quantidadeMin);

    @GET("empresas-conta/cooperativas/{id}")
    Call<EmpresaResponses.CooperativaPerfilPublico> perfilPublico(@Path("id") String cooperativaId);

    // ── Categorias e materiais ──
    @GET("categorias-material")
    Call<List<CategoriaMaterialResponse>> categorias();

    @GET("empresas-conta/materiais")
    Call<List<MaterialResponse>> materiaisDisponiveis();

    // ── Favoritos ──
    @GET("empresas-conta/favoritos")
    Call<List<EmpresaResponses.Favorito>> favoritos();

    @POST("empresas-conta/favoritos")
    Call<EmpresaResponses.Favorito> favoritar(@Body EmpresaRequests.Favorito body);

    @DELETE("empresas-conta/favoritos/{cooperativaId}")
    Call<Void> desfavoritar(@Path("cooperativaId") String cooperativaId);

    // ── Avaliações ──
    @GET("empresas-conta/cooperativas/{id}/estrelas")
    Call<EmpresaResponses.DistribuicaoEstrelas> distribuicaoEstrelas(
            @Path("id") String cooperativaId);

    @GET("empresas-conta/cooperativas/{id}/avaliacoes")
    Call<List<EmpresaResponses.Avaliacao>> avaliacoesPorAvaliado(@Path("id") String cooperativaId);

    @POST("empresas-conta/avaliacoes")
    Call<EmpresaResponses.Avaliacao> criarAvaliacao(@Body EmpresaRequests.Avaliacao body);

    // ── Materiais de interesse (perfil da empresa) ──
    @GET("empresas-conta/interesse")
    Call<List<EmpresaResponses.MaterialInteresse>> materiaisInteresse();

    @POST("empresas-conta/interesse")
    Call<EmpresaResponses.MaterialInteresse> adicionarInteresse(
            @Body EmpresaRequests.MaterialInteresse body);

    @PUT("empresas-conta/interesse")
    Call<List<EmpresaResponses.MaterialInteresse>> substituirInteresses(
            @Body EmpresaRequests.SubstituirInteresses body);

    @DELETE("empresas-conta/interesse/{categoriaId}")
    Call<Void> removerInteresse(@Path("categoriaId") String categoriaId);

    @DELETE("empresas-conta/interesse")
    Call<Void> limparInteresses();

    @GET("empresas-conta/meus-pedidos/{id}/cooperativas")
    Call<List<PedidoCooperativaResponse>> cooperativasPedido(@Path("id") String pedidoId);

    @POST("empresas-conta/pedidos")
    Call<PedidoResponse> enviarPedido(@Body EmpresaRequests.EnviarPedido body);

    // ── Pedidos ──
    @GET("empresas-conta/pedidos")
    Call<List<PedidoResponse>> pedidosPorEmpresa();

    @GET("empresas-conta/pedidos/{id}")
    Call<PedidoResponse> pedido(@Path("id") String pedidoId);

    @GET("empresas-conta/pedidos/{id}/itens")
    Call<List<ItemResponse>> itensPedido(@Path("id") String pedidoId);

    // ── Negociações (mesmo padrão do chat do Gestor) ──
    @GET("empresas-conta/negociacoes")
    Call<List<GestorResponses.Negociacao>> negociacoesPorEmpresa();

    @GET("empresas-conta/negociacoes/por-pedido/{pedidoId}")
    Call<List<GestorResponses.Negociacao>> negociacoesPorPedido(@Path("pedidoId") String pedidoId);

    @GET("empresas-conta/negociacoes/{id}")
    Call<GestorResponses.Negociacao> negociacao(@Path("id") String id);

    @PATCH("empresas-conta/negociacoes/{id}/aceitar")
    Call<GestorResponses.Negociacao> aceitarNegociacao(@Path("id") String id);

    @PATCH("empresas-conta/negociacoes/{id}/recusar")
    Call<GestorResponses.Negociacao> recusarNegociacao(
            @Path("id") String id,
            @Body com.example.renovai.dto.request.GestorRequests.Recusar body);

    @GET("empresas-conta/negociacoes/{id}/mensagens")
    Call<List<GestorResponses.Mensagem>> mensagens(@Path("id") String negociacaoId);

    @POST("empresas-conta/negociacoes/{id}/mensagens")
    Call<GestorResponses.Mensagem> enviarMensagem(
            @Path("id") String negociacaoId,
            @Body com.example.renovai.dto.request.GestorRequests.Mensagem body);

    // ── Status ──
    @GET("status")
    Call<List<GestorResponses.Status>> status();
}
