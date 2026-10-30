package com.example.renovai;

import static org.junit.Assert.*;

import com.example.renovai.controller.ViagemRota;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Andamento da viagem (ViagemRota) contra uma API-Redis simulada (MockWebServer): o que o app
 * manda, como lê as respostas e o que faz sem conexão.
 */
public class ViagemRotaTest {

    private static final String ROTA = "rota-1", EU = "func-1", OUTRO = "func-2";

    private MockWebServer servidor;
    private final Map<String, Map<Integer, String>> guardadas = new HashMap<>();
    private final List<String> avisos = new ArrayList<>();
    private ViagemRota viagem;
    private volatile CountDownLatch mudancas = new CountDownLatch(1);

    private final ViagemRota.Pendencias pendencias = new ViagemRota.Pendencias() {
        @Override
        public Map<Integer, String> ler(String rotaId) {
            return new HashMap<>(guardadas.getOrDefault(rotaId, new HashMap<>()));
        }

        @Override
        public void guardar(String rotaId, int indice, String status) {
            guardadas.computeIfAbsent(rotaId, k -> new HashMap<>()).put(indice, status);
        }

        @Override
        public void remover(String rotaId, int indice) {
            Map<Integer, String> m = guardadas.get(rotaId);
            if (m != null) m.remove(indice);
        }
    };

    @Before
    public void setUp() throws Exception {
        servidor = new MockWebServer();
        servidor.start();
        // respostas no mesmo thread do OkHttp; o teste espera pelos avisos de "mudou"
        RotaViagemApiService api = RotaViagemApiService.criar(servidor.url("/").toString(), Runnable::run);
        viagem = new ViagemRota(api, ROTA, EU, pendencias, new ViagemRota.Ouvinte() {
            @Override
            public void mudou() {
                mudancas.countDown();
            }

            @Override
            public void aviso(String m) {
                avisos.add(m);
            }
        });
        List<ViagemRota.Coleta> coletas = new ArrayList<>();
        for (int i = 0; i < 12; i++) coletas.add(new ViagemRota.Coleta("Rua " + i, "end-" + i, i + 2));
        viagem.setColetas(coletas);
    }

    @After
    public void tearDown() throws Exception {
        servidor.shutdown();
    }

    private static MockResponse json(int code, String corpo) {
        return new MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(corpo);
    }

    private static String sessao(String motorista, String status) {
        return "{\"status\":\"" + status + "\",\"motorista_id\":\"" + motorista
                + "\",\"timestamp_inicio\":\"2026-10-30T08:15:03\"}";
    }

    /** Paradas como a API-Redis antiga devolvia: em ordem de texto (0, 1, 10, 11, 2, …). */
    private static String paradasForaDeOrdem(int concluida) {
        StringBuilder sb = new StringBuilder("{\"paradas\":[");
        String[] ids = {"0", "1", "10", "11", "2", "3", "4", "5", "6", "7", "8", "9"};
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) sb.append(',');
            boolean feita = Integer.parseInt(ids[i]) == concluida;
            sb.append("{\"id\":\"").append(ids[i]).append("\",\"status\":\"")
                    .append(feita ? "CONCLUIDA" : "PENDENTE").append('"');
            if (feita) sb.append(",\"timestamp_conclusao\":\"2026-10-30T09:02:00\"");
            sb.append('}');
        }
        return sb.append("]}").toString();
    }

    /** Executa a ação e espera {@code n} avisos de mudança. */
    private void esperar(int n, Runnable acao) throws InterruptedException {
        mudancas = new CountDownLatch(n);
        acao.run();
        assertTrue("o controller não respondeu", mudancas.await(5, TimeUnit.SECONDS));
    }

    private JsonObject corpo(RecordedRequest r) {
        return new Gson().fromJson(r.getBody().readUtf8(), JsonObject.class);
    }

    @Test
    public void antesDeIniciarA404DeixaAViagemParada() throws Exception {
        servidor.enqueue(json(404, "{\"erro\":\"Rota nao encontrada\"}"));
        esperar(1, viagem::carregar);
        assertTrue(viagem.carregou());
        assertFalse(viagem.emAndamento());
        assertFalse(viagem.semConexao());
        assertNull(viagem.status());
    }

    @Test
    public void retomaAViagemComOIdDaParadaMesmoForaDeOrdem() throws Exception {
        servidor.enqueue(json(200, sessao(EU, "INICIADA")));
        servidor.enqueue(json(200, paradasForaDeOrdem(10)));
        esperar(1, viagem::carregar);
        assertTrue(viagem.emAndamento());
        assertFalse(viagem.outroMotorista());
        assertTrue(viagem.concluida(10));
        assertFalse(viagem.concluida(2)); // controle: a posição 2 da lista é a parada "11"
        assertEquals("2026-10-30T09:02:00", viagem.conclusao(10));
        assertEquals(1, viagem.concluidas());
        assertEquals(0, viagem.proxima());
        assertTrue(viagem.podeMexer());
    }

    @Test
    public void viagemDeOutroMotoristaSoMostraOAndamento() throws Exception {
        servidor.enqueue(json(200, sessao(OUTRO, "INICIADA")));
        servidor.enqueue(json(200, paradasForaDeOrdem(0)));
        esperar(1, viagem::carregar);
        assertTrue(viagem.outroMotorista());
        assertFalse(viagem.podeMexer());
        int pedidos = servidor.getRequestCount();
        viagem.alternar(1);
        viagem.finalizar();
        assertEquals(pedidos, servidor.getRequestCount()); // nada foi enviado
    }

    @Test
    public void iniciarRegistraAsColetasComEnderecoEOrdem() throws Exception {
        servidor.enqueue(json(201, "{\"mensagem\":\"Rota criada\"}"));
        servidor.enqueue(json(201, "{\"mensagem\":\"12 paradas adicionadas\"}"));
        servidor.enqueue(json(200, sessao(EU, "INICIADA")));
        servidor.enqueue(json(200, paradasForaDeOrdem(-1)));
        mudancas = new CountDownLatch(100);
        viagem.iniciar();
        for (int i = 0; i < 50 && !viagem.emAndamento(); i++) Thread.sleep(50);
        assertTrue(viagem.emAndamento());

        RecordedRequest inicio = servidor.takeRequest();
        assertEquals("POST /rotas HTTP/1.1", inicio.getRequestLine());
        assertEquals(EU, corpo(inicio).get("motorista_id").getAsString());
        RecordedRequest paradas = servidor.takeRequest();
        assertEquals("/rotas/rota-1/paradas", paradas.getPath());
        JsonObject p = corpo(paradas).getAsJsonArray("paradas").get(11).getAsJsonObject();
        assertEquals("end-11", p.get("endereco_id").getAsString());
        assertEquals("13", p.get("ordem").getAsString());
        assertTrue(avisos.contains("Rota iniciada. Boa viagem!"));
    }

    @Test
    public void iniciarRotaDeOutroMotoristaRecebe409ERecarrega() throws Exception {
        servidor.enqueue(json(409, "{\"erro\":\"Rota em andamento com outro motorista\"}"));
        servidor.enqueue(json(200, sessao(OUTRO, "INICIADA")));
        servidor.enqueue(json(200, paradasForaDeOrdem(-1)));
        mudancas = new CountDownLatch(100);
        viagem.iniciar();
        for (int i = 0; i < 50 && !viagem.outroMotorista(); i++) Thread.sleep(50);
        assertTrue(viagem.outroMotorista());
        assertFalse(viagem.podeMexer());
        assertFalse(avisos.isEmpty());
    }

    @Test
    public void semConexaoAColetaFicaGuardadaEEReenviadaDepois() throws Exception {
        servidor.enqueue(json(200, sessao(EU, "INICIADA")));
        servidor.enqueue(json(200, paradasForaDeOrdem(-1)));
        esperar(1, viagem::carregar);
        servidor.takeRequest();
        servidor.takeRequest();

        servidor.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
        esperar(2, () -> viagem.alternar(3));
        assertTrue(viagem.semConexao());
        assertTrue(viagem.concluida(3)); // o motorista vê o que marcou
        assertTrue(viagem.aguardandoEnvio(3));
        assertEquals("CONCLUIDA", guardadas.get(ROTA).get(3));
        RecordedRequest falhou = servidor.takeRequest();
        assertEquals("/rotas/rota-1/parada/3", falhou.getPath());
        assertEquals(EU, corpo(falhou).get("motorista_id").getAsString());

        // finalizar com coleta não enviada: não manda a finalização
        servidor.enqueue(json(200, "{\"id\":\"3\",\"status\":\"CONCLUIDA\"}"));
        esperar(1, viagem::finalizar);
        RecordedRequest reenvio = servidor.takeRequest();
        assertEquals("PUT /rotas/rota-1/parada/3 HTTP/1.1", reenvio.getRequestLine());
        assertFalse(viagem.aguardandoEnvio(3));
        assertTrue(viagem.concluida(3));
        assertEquals(0, viagem.pendentesDeEnvio());
        assertTrue(viagem.podeFinalizar());
    }

    @Test
    public void recusaDaApiDescartaAPendenciaEVoltaAoEstadoOficial() throws Exception {
        servidor.enqueue(json(200, sessao(EU, "INICIADA")));
        servidor.enqueue(json(200, paradasForaDeOrdem(-1)));
        esperar(1, viagem::carregar);
        servidor.enqueue(json(409, "{\"erro\":\"Rota ja finalizada\"}"));
        servidor.enqueue(json(200, sessao(EU, "FINALIZADA")));
        mudancas = new CountDownLatch(100);
        viagem.alternar(0);
        for (int i = 0; i < 50 && !viagem.finalizada(); i++) Thread.sleep(50);
        assertTrue(viagem.finalizada());
        assertEquals(0, viagem.pendentesDeEnvio());
        assertFalse(viagem.podeMexer());
    }

    @Test
    public void finalizarMandaOMotoristaEMarcaFinalizada() throws Exception {
        servidor.enqueue(json(200, sessao(EU, "INICIADA")));
        servidor.enqueue(json(200, paradasForaDeOrdem(-1)));
        esperar(1, viagem::carregar);
        servidor.takeRequest();
        servidor.takeRequest();
        servidor.enqueue(json(200, sessao(EU, "FINALIZADA")));
        mudancas = new CountDownLatch(100);
        viagem.finalizar();
        for (int i = 0; i < 50 && !viagem.finalizada(); i++) Thread.sleep(50);
        RecordedRequest fim = servidor.takeRequest();
        assertEquals("PUT /rotas/rota-1/finalizar HTTP/1.1", fim.getRequestLine());
        assertEquals(EU, corpo(fim).get("motorista_id").getAsString());
        assertTrue(viagem.finalizada());
    }

    @Test
    public void sessaoSincronizadaContaComoFinalizada() throws Exception {
        servidor.enqueue(json(200, sessao(EU, "SINCRONIZADA")));
        esperar(1, viagem::carregar);
        assertTrue(viagem.finalizada());
        assertFalse(viagem.emAndamento());
    }

    @Test
    public void finalizadaMostraAsColetasFeitasSemReenviarNada() throws Exception {
        servidor.enqueue(json(200, sessao(EU, "FINALIZADA")));
        servidor.enqueue(json(200, paradasForaDeOrdem(10)));
        esperar(2, viagem::carregar);
        assertTrue(viagem.finalizada());
        assertTrue(viagem.concluida(10));
        assertEquals(1, viagem.concluidas());
        assertFalse(viagem.podeMexer());
        assertFalse(viagem.semConexao());
        servidor.takeRequest(); // GET /rotas/{id}
        assertEquals("GET", servidor.takeRequest().getMethod()); // GET /paradas
        assertEquals(2, servidor.getRequestCount()); // nada de POST de paradas nem PUT de pendências
    }

    @Test
    public void finalizadaJaSemParadasNaoReenviaAsParadas() throws Exception {
        servidor.enqueue(json(200, sessao(EU, "FINALIZADA")));
        servidor.enqueue(json(200, "{\"paradas\":[]}"));
        esperar(2, viagem::carregar);
        assertEquals(0, viagem.concluidas());
        assertEquals(2, servidor.getRequestCount());
    }

    @Test
    public void coordenadasInvalidasSaoDescartadas() {
        assertTrue(RotaEstimador.valida(new double[] {-23.55, -46.63}));
        assertFalse(RotaEstimador.valida(null));
        assertFalse(RotaEstimador.valida(new double[] {0, 0}));
        assertFalse(RotaEstimador.valida(new double[] {91, 10}));
        assertFalse(RotaEstimador.valida(new double[] {Double.NaN, 10}));
        double m = RotaEstimador.metros(new double[] {-23.5505, -46.6333}, new double[] {-23.5614, -46.5994});
        assertTrue("Sé → Mooca ≈ 3,7 km, veio " + m, m > 3500 && m < 3900);
        assertEquals("350 m", RotaEstimador.textoDistancia(350));
    }
}
