package com.example.renovai;

import com.example.renovai.dto.request.GestorRequests;
import com.example.renovai.dto.request.TriagemRequest;
import com.example.renovai.dto.response.ColetaResponse;
import com.example.renovai.dto.response.FuncionarioResponse;
import com.example.renovai.dto.response.GestorResponses;
import com.example.renovai.dto.response.MaterialResponse;
import com.example.renovai.dto.response.PerfilResponse;
import com.example.renovai.dto.response.RateioListaResponse;
import com.example.renovai.dto.response.TriagemResponse;

import okhttp3.ResponseBody;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

import java.util.List;

/**
 * Todos os endpoints usados pela área do Gestor da Cooperativa. Cada path existe em um controller
 * da API (context-path /api).
 */
public interface GestorApiService {

    // ── Coletas / Triagens / Estoque ──
    @GET("coletas/por-cooperativa/{id}")
    Call<List<ColetaResponse>> coletas(@Path("id") String cooperativaId);

    @GET("coletas/por-rota/{id}")
    Call<List<ColetaResponse>> coletasDaRota(@Path("id") String rotaId);

    @GET("triagens/por-cooperativa/{id}")
    Call<List<TriagemResponse>> triagens(@Path("id") String cooperativaId);

    // Nova triagem (4.8.2): equipe -> membros -> uma linha de triagem por material
    @POST("equipes")
    Call<GestorResponses.Equipe> criarEquipe(@Body GestorRequests.Equipe body);

    @PUT("equipes/{id}")
    Call<GestorResponses.Equipe> atualizarEquipe(
            @Path("id") String equipeId, @Body GestorRequests.Equipe body);

    @POST("equipes-cooperados")
    Call<ResponseBody> adicionarMembroEquipe(@Body GestorRequests.EquipeCooperado body);

    @POST("triagens")
    Call<TriagemResponse> criarTriagem(@Body TriagemRequest body);

    @GET("materiais/disponiveis")
    Call<List<MaterialResponse>> materiaisDisponiveis();

    @GET("estoques/por-cooperativa/{id}")
    Call<List<GestorResponses.Estoque>> estoque(@Path("id") String cooperativaId);

    // ── Pedidos / Negociações ──
    @GET("pedidos/por-cooperativa/{id}")
    Call<List<GestorResponses.PedidoCoop>> pedidosCoop(@Path("id") String cooperativaId);

    @GET("pedidos/{id}")
    Call<GestorResponses.Pedido> pedido(@Path("id") String pedidoId);

    @GET("pedidos/{id}/itens")
    Call<List<GestorResponses.Item>> itensPedido(@Path("id") String pedidoId);

    @PUT("pedidos/cooperativa/{id}/status/{statusId}")
    Call<GestorResponses.PedidoCoop> statusPedidoCoop(
            @Path("id") String id, @Path("statusId") String statusId);

    @GET("negociacoes/por-cooperativa/{id}")
    Call<List<GestorResponses.Negociacao>> negociacoes(@Path("id") String cooperativaId);

    @GET("negociacoes/por-pedido/{id}")
    Call<List<GestorResponses.Negociacao>> negociacoesDoPedido(@Path("id") String pedidoId);

    @GET("negociacoes/{id}")
    Call<GestorResponses.Negociacao> negociacao(@Path("id") String id);

    @POST("negociacoes")
    Call<GestorResponses.Negociacao> abrirNegociacao(@Body GestorRequests.Negociacao body);

    @POST("negociacoes/itens")
    Call<GestorResponses.NegItem> adicionarItemNegociacao(@Body GestorRequests.NegItem body);

    @GET("negociacoes/{id}/mensagens")
    Call<List<GestorResponses.Mensagem>> mensagens(@Path("id") String id);

    @POST("negociacoes/{id}/mensagens")
    Call<GestorResponses.Mensagem> enviarMensagem(
            @Path("id") String id, @Body GestorRequests.Mensagem body);

    @POST("negociacoes/{id}/contraproposta")
    Call<GestorResponses.Negociacao> contraproposta(
            @Path("id") String id, @Body GestorRequests.Contraproposta body);

    @PATCH("negociacoes/{id}/concluir")
    Call<GestorResponses.Negociacao> fechar(
            @Path("id") String id, @Body GestorRequests.Fechar body);

    @PATCH("negociacoes/{id}/recusar")
    Call<GestorResponses.Negociacao> recusar(
            @Path("id") String id, @Body GestorRequests.Recusar body);

    @GET("status")
    Call<List<GestorResponses.Status>> status();

    @POST("functions/pedidos/aceitar")
    Call<GestorResponses.Procedure> aceitarProcedure(@Body GestorRequests.Aceitar body);

    @GET("perfis")
    Call<List<PerfilResponse>> perfis();

    // ── Rateios / Despesas ──
    @GET("rateios/por-cooperativa/{id}")
    Call<List<RateioListaResponse>> rateios(@Path("id") String cooperativaId);

    @GET("rateios/{id}")
    Call<GestorResponses.RateioDetalhe> rateio(@Path("id") String rateioId);

    @POST("rateios/executar-geral")
    Call<GestorResponses.RateioRealizado> rateioGeral(@Body GestorRequests.RateioPeriodo body);

    @POST("rateios/executar-proporcional")
    Call<GestorResponses.RateioRealizado> rateioProporcional(
            @Body GestorRequests.RateioPeriodo body);

    @POST("functions/financeiro/acumulado")
    Call<GestorResponses.TotalAcumulado> totalAcumulado(@Body GestorRequests.Financeiro body);

    @GET("despesas/por-cooperativa/{id}")
    Call<List<GestorResponses.Despesa>> despesas(@Path("id") String cooperativaId);

    @POST("despesas")
    Call<GestorResponses.Despesa> criarDespesa(@Body GestorRequests.Despesa body);

    @POST("despesas/lancamentos")
    Call<GestorResponses.Lancamento> lancarDespesa(@Body GestorRequests.Lancamento body);

    @GET("despesas/lancamentos/total/por-cooperativa/{id}/mes/{mes}")
    Call<GestorResponses.TotalDespesas> totalDespesas(
            @Path("id") String cooperativaId, @Path("mes") String mesIso);

    // ── Funcionários / Perfil ──
    @GET("funcionarios/por-cooperativa/{id}")
    Call<List<FuncionarioResponse>> funcionariosBase(@Path("id") String cooperativaId);

    @GET("funcionarios/pre-cadastro/incompletos/por-cooperativa/{id}")
    Call<List<GestorResponses.PreCadastro>> preCadastros(@Path("id") String cooperativaId);

    @PUT("funcionarios/{id}/cargo/{cargoId}")
    Call<FuncionarioResponse> atualizarCargo(
            @Path("id") String funcionarioId, @Path("cargoId") String cargoId);

    @GET("cargos")
    Call<List<GestorResponses.Cargo>> cargos();

    @GET("usuarios/{id}")
    Call<GestorResponses.Usuario> usuario(@Path("id") String usuarioId);

    // ── Rotas ──
    @GET("rotas/por-cooperativa/{id}")
    Call<List<GestorResponses.Rota>> rotas(@Path("id") String cooperativaId);

    @GET("rotas/{id}")
    Call<GestorResponses.Rota> rota(@Path("id") String rotaId);

    @POST("rotas")
    Call<GestorResponses.Rota> criarRota(@Body GestorRequests.Rota body);

    @PUT("rotas/{id}")
    Call<GestorResponses.Rota> atualizarRota(
            @Path("id") String rotaId, @Body GestorRequests.Rota body);

    @POST("enderecos")
    Call<GestorResponses.Endereco> criarEndereco(@Body GestorRequests.Endereco body);

    @POST("rotas/enderecos")
    Call<GestorResponses.RotaEnd> adicionarEnderecoRota(@Body GestorRequests.RotaEnd body);
}
