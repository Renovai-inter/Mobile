package com.example.renovai;

import static org.junit.Assert.*;

import com.example.renovai.dto.request.CadastroEmpresaRequest;
import com.example.renovai.dto.request.EmpresaRequests;
import com.example.renovai.dto.response.EmpresaResponses;
import com.example.renovai.dto.response.MaterialResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.Request;

import okio.Buffer;

import org.junit.Test;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Exercita os contratos reais de EmpresaContaController sem enviar dados ao servidor. */
public class EmpresaApiContractTest {
    @Test
    public void conclusaoDoGestorUsaValorAceitoERotaConcluir() throws Exception {
        GestorApiService gestor =
                new Retrofit.Builder()
                        .baseUrl("http://localhost/api/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(GestorApiService.class);
        Request request =
                gestor.fechar(
                                "neg",
                                new com.example.renovai.dto.request.GestorRequests.Fechar(
                                        12.50, null))
                        .request();
        assertEquals("PATCH", request.method());
        assertEquals("/api/negociacoes/neg/concluir", request.url().encodedPath());
        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        assertEquals(
                12.50,
                new Gson()
                        .fromJson(buffer.readUtf8(), JsonObject.class)
                        .get("valorFinal")
                        .getAsDouble(),
                0.001);
    }

    private final Gson gson = new Gson();
    private final EmpresaApiService api =
            new Retrofit.Builder()
                    .baseUrl("http://localhost/api/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(EmpresaApiService.class);

    @Test
    public void consultasUsamContaAutenticadaEParametrosDeBusca() {
        assertEquals(
                "/api/empresas-conta/dashboard", api.dashboard().request().url().encodedPath());
        assertEquals(
                "/api/empresas-conta/pedidos",
                api.pedidosPorEmpresa().request().url().encodedPath());
        assertEquals(
                "/api/empresas-conta/pedidos/pedido/itens",
                api.itensPedido("pedido").request().url().encodedPath());
        assertEquals(
                "/api/empresas-conta/meus-pedidos/pedido/cooperativas",
                api.cooperativasPedido("pedido").request().url().encodedPath());
        assertEquals(
                "/api/empresas-conta/interesse",
                api.materiaisInteresse().request().url().encodedPath());
        Request busca = api.buscarCooperativas("categoria", "São Paulo", "10.5").request();
        assertEquals("/api/empresas-conta/cooperativas", busca.url().encodedPath());
        assertEquals("categoria", busca.url().queryParameter("categoriaId"));
        assertEquals("São Paulo", busca.url().queryParameter("cidade"));
        assertEquals("10.5", busca.url().queryParameter("quantidadeMin"));
        assertNull(api.buscarCooperativas(null, null, null).request().url().query());
    }

    @Test
    public void pedidoEhAtomicoComChaveEstavelEDecimais() throws Exception {
        EmpresaRequests.EnviarPedido body = new EmpresaRequests.EnviarPedido();
        body.cooperativaId = "coop";
        body.chaveSolicitacao = "mesma-chave";
        body.itens.add(new EmpresaRequests.PedidoItem("material", 0.125, 2.35));
        Request primeira = api.enviarPedido(body).request();
        Request repeticao = api.enviarPedido(body).request();
        assertEquals("POST", primeira.method());
        assertEquals("/api/empresas-conta/pedidos", primeira.url().encodedPath());
        Buffer a = new Buffer(), b = new Buffer();
        primeira.body().writeTo(a);
        repeticao.body().writeTo(b);
        String json = a.readUtf8();
        assertEquals(json, b.readUtf8());
        JsonObject pedido = gson.fromJson(json, JsonObject.class);
        assertEquals("mesma-chave", pedido.get("chaveSolicitacao").getAsString());
        assertFalse(pedido.has("empresaId"));
        assertFalse(pedido.has("observacao"));
        JsonObject item = pedido.getAsJsonArray("itens").get(0).getAsJsonObject();
        assertEquals("0.125", item.get("quantidadeKg").getAsString());
        assertEquals("2.35", item.get("precoUnitario").getAsString());
        assertFalse(item.has("pedidoId"));
    }

    @Test
    public void materialGlobalNaoPrecisaDeCooperativaId() {
        MaterialResponse material =
                gson.fromJson(
                        "{\"materialId\":\"m\",\"categoriaNome\":\"Papel\",\"precoSugerido\":2.50,\"estaDisponivel\":true}",
                        MaterialResponse.class);
        assertNull(material.getCooperativaId());
        assertEquals(Boolean.TRUE, material.getEstaDisponivel());
        EmpresaResponses.CooperativaPerfilPublico perfil =
                gson.fromJson(
                        "{\"cooperativaId\":\"c\",\"telefone\":\"11999999999\",\"email\":\"contato@coop.com\",\"materiaisDisponiveis\":[{\"materialId\":\"m\",\"quantidadeKg\":12.125}]}",
                        EmpresaResponses.CooperativaPerfilPublico.class);
        assertEquals("11999999999", perfil.contato());
        assertEquals(material.getMaterialId(), perfil.materiaisDisponiveis.get(0).materialId);
        assertEquals(12.125, perfil.materiaisDisponiveis.get(0).quantidadeKg, 0.000001);
        perfil.telefone = null;
        assertEquals("contato@coop.com", perfil.contato());
    }

    @Test
    public void cadastroIncluiCpfEavaliacaoIncluiPedidoECooperativa() throws Exception {
        JsonObject cadastro =
                gson.toJsonTree(
                                new CadastroEmpresaRequest(
                                        "Nome",
                                        "12345678901",
                                        "email@empresa.com",
                                        "11999999999",
                                        "senha123",
                                        "Empresa",
                                        "12345678000199",
                                        "Endereço"))
                        .getAsJsonObject();
        assertEquals("12345678901", cadastro.get("cpf").getAsString());
        Request request =
                api.criarAvaliacao(
                                new EmpresaRequests.Avaliacao(null, "coop", "pedido", 5, "Ótimo"))
                        .request();
        assertEquals("/api/empresas-conta/avaliacoes", request.url().encodedPath());
        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        JsonObject avaliacao = gson.fromJson(buffer.readUtf8(), JsonObject.class);
        assertEquals("coop", avaliacao.get("cooperativaId").getAsString());
        assertEquals("pedido", avaliacao.get("pedidoId").getAsString());
        assertFalse(avaliacao.has("avaliadorId"));
        assertFalse(avaliacao.has("avaliadoId"));
    }

    @Test
    public void aceiteUsaRotaDaContaSemEnviarValorFinal() throws Exception {
        Request request = api.aceitarNegociacao("neg").request();
        assertEquals("PATCH", request.method());
        assertEquals("/api/empresas-conta/negociacoes/neg/aceitar", request.url().encodedPath());
        assertTrue(request.body() == null || request.body().contentLength() == 0);
        assertEquals(
                "/api/empresas-conta/negociacoes",
                api.negociacoesPorEmpresa().request().url().encodedPath());
        assertEquals(
                "/api/empresas-conta/negociacoes/neg/mensagens",
                api.mensagens("neg").request().url().encodedPath());
        assertEquals(
                "/api/empresas-conta/negociacoes/neg/recusar",
                api.recusarNegociacao(
                                "neg",
                                new com.example.renovai.dto.request.GestorRequests.Recusar(
                                        "Recusado"))
                        .request()
                        .url()
                        .encodedPath());
    }

    @Test
    public void interessesSaoSubstituidosEmUmaChamadaInclusiveSelecaoVazia() throws Exception {
        Request request =
                api.substituirInteresses(
                                new EmpresaRequests.SubstituirInteresses(
                                        java.util.Arrays.asList("papel", "metal")))
                        .request();
        assertEquals("PUT", request.method());
        assertEquals("/api/empresas-conta/interesse", request.url().encodedPath());
        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        JsonObject body = gson.fromJson(buffer.readUtf8(), JsonObject.class);
        assertEquals(2, body.getAsJsonArray("categoriaIds").size());
        assertFalse(body.has("empresaId"));
        request =
                api.substituirInteresses(
                                new EmpresaRequests.SubstituirInteresses(
                                        java.util.Collections.emptyList()))
                        .request();
        buffer = new Buffer();
        request.body().writeTo(buffer);
        assertEquals(
                0,
                gson.fromJson(buffer.readUtf8(), JsonObject.class)
                        .getAsJsonArray("categoriaIds")
                        .size());
        assertEquals(
                "/api/empresas-conta/interesse/papel",
                api.removerInteresse("papel").request().url().encodedPath());
    }

    @Test
    public void favoritosNaoEnviamEmpresaIdEUsamCooperativaNoCaminho() throws Exception {
        assertEquals(
                "/api/empresas-conta/favoritos", api.favoritos().request().url().encodedPath());
        Request request = api.favoritar(new EmpresaRequests.Favorito("coop")).request();
        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        JsonObject body = gson.fromJson(buffer.readUtf8(), JsonObject.class);
        assertEquals("coop", body.get("cooperativaId").getAsString());
        assertEquals(1, body.size());
        assertEquals(
                "/api/empresas-conta/favoritos/coop",
                api.desfavoritar("coop").request().url().encodedPath());
        assertNull(api.desfavoritar("coop").request().url().query());
    }

    @Test
    public void observacaoDaContrapropostaEhDesserializada() {
        com.example.renovai.dto.response.GestorResponses.Negociacao n =
                gson.fromJson(
                        "{\"negociacaoId\":\"n\",\"statusAtual\":\"Em"
                            + " andamento\",\"valorTotal\":120.00,\"observacao\":\"Retirada pela"
                            + " empresa\"}",
                        com.example.renovai.dto.response.GestorResponses.Negociacao.class);
        assertEquals("Retirada pela empresa", n.observacao);
        assertEquals(120, n.valorTotal, 0);
        n =
                gson.fromJson(
                        "{\"observacao\":null}",
                        com.example.renovai.dto.response.GestorResponses.Negociacao.class);
        assertNull(n.observacao);
    }

    @Test
    public void carrinhoMantemChaveNaMesmaCooperativaEGeraOutraParaNovoPedido() {
        EmpresaCarrinho.limpar();
        EmpresaCarrinho.iniciar("coop", "Cooperativa");
        String chave = EmpresaCarrinho.chaveSolicitacao;
        EmpresaCarrinho.itens.add(new EmpresaCarrinho.Item("m", "Metal", 10, 2));
        EmpresaCarrinho.iniciar("coop", "Cooperativa");
        assertEquals(chave, EmpresaCarrinho.chaveSolicitacao);
        assertEquals(20, EmpresaCarrinho.total(), 0);
        EmpresaCarrinho.iniciar("outra", "Outra");
        assertTrue(EmpresaCarrinho.itens.isEmpty());
        assertNotEquals(chave, EmpresaCarrinho.chaveSolicitacao);
        EmpresaCarrinho.limpar();
    }
}
