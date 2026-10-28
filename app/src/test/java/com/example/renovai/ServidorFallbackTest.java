package com.example.renovai;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.ServerSocket;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ServidorFallbackTest {
    private final MockWebServer principal = new MockWebServer();
    private final MockWebServer reserva = new MockWebServer();

    @Before
    public void iniciar() throws IOException {
        principal.start();
        reserva.start();
    }

    @After
    public void parar() throws IOException {
        principal.shutdown();
        reserva.shutdown();
    }

    private static String url(MockWebServer s) {
        return s.url("/api/").toString();
    }

    private static String fora() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return "http://127.0.0.1:" + s.getLocalPort() + "/api/";
        }
    }

    private static OkHttpClient cliente(String principal, String reserva) {
        return new OkHttpClient.Builder()
                .addInterceptor(new ServidorFallbackInterceptor(principal, reserva))
                .build();
    }

    private static Response get(OkHttpClient c, String url) throws IOException {
        return c.newCall(new Request.Builder().url(url).build()).execute();
    }

    @Test
    public void usaOPrincipalQuandoEleResponde() throws IOException {
        principal.enqueue(new MockResponse().setBody("aws"));

        try (Response r = get(cliente(url(principal), url(reserva)), url(principal) + "cooperativas")) {
            assertEquals("aws", r.body().string());
        }
        assertEquals(0, reserva.getRequestCount());
    }

    @Test
    public void principalForaCaiNoReservaComOMesmoCaminho() throws IOException, InterruptedException {
        reserva.enqueue(new MockResponse().setBody("render"));
        String fora = fora();

        try (Response r = get(cliente(fora, url(reserva)), fora + "coletas/cooperativa/c1?status=x")) {
            assertEquals("render", r.body().string());
        }
        assertEquals("/api/coletas/cooperativa/c1?status=x", reserva.takeRequest().getPath());
    }

    @Test
    public void principalForaTambemSalvaPostNoReserva() throws IOException, InterruptedException {
        reserva.enqueue(new MockResponse().setResponseCode(201));
        String fora = fora();
        RequestBody corpo = RequestBody.create("{\"a\":1}", MediaType.get("application/json"));

        try (Response r = cliente(fora, url(reserva))
                .newCall(new Request.Builder().url(fora + "coletas").post(corpo).build()).execute()) {
            assertEquals(201, r.code());
        }
        okhttp3.mockwebserver.RecordedRequest recebido = reserva.takeRequest();
        assertEquals("/api/coletas", recebido.getPath());
        assertEquals("{\"a\":1}", recebido.getBody().readUtf8());
    }

    @Test
    public void gatewaySemAplicacaoCaiNoReservaEFicaNeleUmTempo() throws IOException {
        principal.enqueue(new MockResponse().setResponseCode(503));
        reserva.enqueue(new MockResponse().setBody("render"));
        reserva.enqueue(new MockResponse().setBody("render"));
        OkHttpClient c = cliente(url(principal), url(reserva));

        try (Response r = get(c, url(principal) + "perfis")) {
            assertEquals("render", r.body().string());
        }
        try (Response r = get(c, url(principal) + "perfis")) {
            assertEquals("render", r.body().string());
        }
        assertEquals(1, principal.getRequestCount());
    }

    @Test
    public void erroDaAplicacaoNaoTrocaDeServidor() throws IOException {
        principal.enqueue(new MockResponse().setResponseCode(500));

        try (Response r = get(cliente(url(principal), url(reserva)), url(principal) + "coletas")) {
            assertEquals(500, r.code());
        }
        assertEquals(0, reserva.getRequestCount());
    }
}
