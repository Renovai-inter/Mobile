package com.example.renovai;

import static org.junit.Assert.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.Request;

import okio.Buffer;

import org.junit.Test;

import java.util.Arrays;

/** Formato dos pedidos do app para a API-Redis (andamento da viagem) e para a IA (/ia/rotas/otimizar). */
public class RotasMotoristaContractTest {
    private final RotaViagemApiService redis = RotaViagemApiService.criar("http://localhost/");
    private final RotaOtimizadorIaService ia = RotaOtimizadorIaService.criar("http://localhost/");

    private JsonObject body(Request r) throws Exception {
        Buffer b = new Buffer();
        r.body().writeTo(b);
        return new Gson().fromJson(b.readUtf8(), JsonObject.class);
    }

    @Test
    public void iniciarMandaRotaEMotoristaNaRaizDoGateway() throws Exception {
        Request r = redis.iniciar(new RotaViagemApiService.Inicio("rota-1", "func-9")).request();
        assertEquals("POST", r.method());
        assertEquals("/rotas", r.url().encodedPath());
        JsonObject json = body(r);
        assertEquals("rota-1", json.get("rota_id").getAsString());
        assertEquals("func-9", json.get("motorista_id").getAsString());
    }

    @Test
    public void paradasVaoSoComEnderecoSemIdNemStatus() throws Exception {
        Request r =
                redis.adicionarParadas(
                                "rota-1",
                                new RotaViagemApiService.Paradas(
                                        Arrays.asList(
                                                new RotaViagemApiService.Parada("Rua A, 1"),
                                                new RotaViagemApiService.Parada("Rua B, 2"))))
                        .request();
        assertEquals("/rotas/rota-1/paradas", r.url().encodedPath());
        JsonObject p = body(r).getAsJsonArray("paradas").get(1).getAsJsonObject();
        assertEquals("Rua B, 2", p.get("endereco").getAsString());
        // a API numera as paradas pela posição; id/status mandados pelo app seriam ignorados
        assertFalse(p.has("id"));
        assertFalse(p.has("status"));
    }

    @Test
    public void marcarColetaUsaOIndiceDaParadaEPut() throws Exception {
        Request r =
                redis.atualizarParada(
                                "rota-1", 3,
                                new RotaViagemApiService.NovoStatus(RotaViagemApiService.CONCLUIDA))
                        .request();
        assertEquals("PUT", r.method());
        assertEquals("/rotas/rota-1/parada/3", r.url().encodedPath());
        assertEquals("CONCLUIDA", body(r).get("status").getAsString());

        Request f = redis.finalizar("rota-1").request();
        assertEquals("PUT", f.method());
        assertEquals("/rotas/rota-1/finalizar", f.url().encodedPath());
    }

    @Test
    public void leSessaoEParadasComoOsHashesDoRedis() {
        Gson gson = new Gson();
        RotaViagemApiService.Sessao s =
                gson.fromJson(
                        "{\"rota_id\":\"r\",\"motorista_id\":\"func-9\",\"status\":\"INICIADA\","
                                + "\"parada_atual\":\"0\",\"timestamp_inicio\":\"2026-10-29T08:15:03.1\"}",
                        RotaViagemApiService.Sessao.class);
        assertEquals("INICIADA", s.status);
        assertEquals("func-9", s.motoristaId);
        assertEquals("2026-10-29T08:15:03.1", s.inicio);

        RotaViagemApiService.Paradas p =
                gson.fromJson(
                        "{\"paradas\":[{\"id\":\"10\",\"status\":\"CONCLUIDA\","
                                + "\"peso_estimado\":\"0\",\"endereco\":\"Rua A\"}]}",
                        RotaViagemApiService.Paradas.class);
        assertEquals("10", p.paradas.get(0).id);
        assertEquals("CONCLUIDA", p.paradas.get(0).status);
    }

    @Test
    public void otimizarPedeSoOCalculoSemGravarNoNeo4j() throws Exception {
        Request r =
                ia.otimizar(
                                new RotaOtimizadorIaService.Pedido(
                                        "coop-1", "rota-1",
                                        new RotaOtimizadorIaService.Ponto("e0", "Rua A, 1, São Paulo", "Sé"),
                                        Arrays.asList(
                                                new RotaOtimizadorIaService.Ponto(
                                                        "e1", "Rua B, 2, São Paulo", null)),
                                        9))
                        .request();
        // /rotas/** o gateway entrega para a API-Redis; /ia/rotas/** chega na IA
        assertEquals("/ia/rotas/otimizar", r.url().encodedPath());
        JsonObject json = body(r);
        assertFalse(json.get("salvar_no_neo4j").getAsBoolean());
        assertEquals("coop-1", json.get("cooperativa_id").getAsString());
        assertEquals(9, json.get("hora_saida").getAsInt());
        assertEquals("Rua A, 1, São Paulo", json.getAsJsonObject("origem").get("endereco").getAsString());
        assertEquals("e1", json.getAsJsonArray("paradas").get(0).getAsJsonObject().get("id").getAsString());
    }
}
