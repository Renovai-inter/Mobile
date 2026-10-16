package com.example.renovai;

import static org.junit.Assert.*;

import com.example.renovai.controller.ColetaController;
import com.example.renovai.controller.TriagemController;
import com.example.renovai.dto.request.GestorRequests;
import com.example.renovai.dto.request.TriagemRequest;
import com.example.renovai.dto.response.GestorResponses;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.Request;

import okio.Buffer;

import org.junit.Test;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;

public class AreasApiContractTest {
    private final Retrofit retrofit =
            new Retrofit.Builder()
                    .baseUrl("http://localhost/api/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

    private JsonObject body(Request r) throws Exception {
        Buffer b = new Buffer();
        r.body().writeTo(b);
        return new Gson().fromJson(b.readUtf8(), JsonObject.class);
    }

    @Test
    public void coletaExternaPreservaRotaEEntregaNaoEnviaRota() throws Exception {
        ColetaApiService api = retrofit.create(ColetaApiService.class);
        for (String tipo : Arrays.asList("EXTERNA", "EXTERNO")) {
            Request request =
                    api.criar(
                                    ColetaController.montarRequest(
                                            "func", new BigDecimal("1.125"), tipo, "rota", null))
                            .request();
            assertEquals("/api/coletas", request.url().encodedPath());
            JsonObject json = body(request);
            assertEquals("EXTERNA", json.get("tipoColeta").getAsString());
            assertEquals("rota", json.get("rotaId").getAsString());
            assertEquals("1.125", json.get("quantidadeKg").getAsString());
        }
        assertFalse(
                body(api.criar(
                                        ColetaController.montarRequest(
                                                "f", BigDecimal.ONE, "ENTREGA", "rota", null))
                                .request())
                        .has("rotaId"));
    }

    @Test
    public void marcadorDaTriagemNaoCreditaPesoFicticio() throws Exception {
        Request request =
                retrofit.create(GestorApiService.class)
                        .criarTriagem(
                                new TriagemRequest(
                                        "eq",
                                        "col",
                                        "mat",
                                        "status",
                                        TriagemController.PESO_INICIAL,
                                        TriagemController.PESO_INICIAL,
                                        null))
                        .request();
        JsonObject json = body(request);
        assertEquals(
                0,
                json.get("quantidadeKg")
                        .getAsBigDecimal()
                        .compareTo(json.get("quantidadeRejeitoKg").getAsBigDecimal()));
    }

    @Test
    public void pesosInvalidosNaoPermitemConcluirTriagem() {
        assertNotNull(TriagemController.validarPesos(null, BigDecimal.ZERO));
        assertNotNull(TriagemController.validarPesos(new BigDecimal("0.0001"), BigDecimal.ZERO));
        assertNotNull(TriagemController.validarPesos(BigDecimal.ONE, new BigDecimal("-1")));
        assertNotNull(TriagemController.validarPesos(BigDecimal.ONE, new BigDecimal("2")));
        assertNull(
                TriagemController.validarPesos(new BigDecimal("1.125"), new BigDecimal("0.125")));
    }

    @Test
    public void periodoMensalIncluiMeiaNoiteEUltimoDiaSemExcecao() throws Exception {
        Request request =
                retrofit.create(GestorApiService.class)
                        .rateioGeral(
                                GestorRequests.RateioPeriodo.paraMes(
                                        "g", "c", YearMonth.of(2024, 2)))
                        .request();
        JsonObject json = body(request);
        assertEquals("2024-02-01T00:00:00", json.get("dataInicio").getAsString());
        assertEquals("2024-02-29T23:59:59", json.get("dataFim").getAsString());
        assertEquals("/api/rateios/executar-geral", request.url().encodedPath());
    }

    @Test
    public void motoristaConsultaCadastroSemOperacoesDeProgressoSql() {
        MotoristaApiService api = retrofit.create(MotoristaApiService.class);
        assertEquals(
                "/api/rotas/por-cooperativa/coop", api.rotas("coop").request().url().encodedPath());
        assertEquals("/api/rotas/rota", api.rota("rota").request().url().encodedPath());
        for (java.lang.reflect.Method m : MotoristaApiService.class.getDeclaredMethods()) {
            assertNotNull(m.getAnnotation(retrofit2.http.GET.class));
        }
    }

    @Test
    public void rotaInativaNaoSignificaViagemConcluida() {
        GestorResponses.Rota ativa = new GestorResponses.Rota();
        ativa.estaAtiva = true;
        GestorResponses.Rota inativa = new GestorResponses.Rota();
        inativa.estaAtiva = false;
        GestorResponses.Rota desconhecida = new GestorResponses.Rota();
        MotoristaRotas.Grupo g =
                MotoristaRotas.classificar(Arrays.asList(ativa, inativa, desconhecida));
        assertEquals(Arrays.asList(ativa), g.disponiveis);
        assertEquals(Arrays.asList(inativa), g.inativas);
    }
}
