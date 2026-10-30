package com.example.renovai;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import retrofit2.Response;

/**
 * Fluxo do motorista contra servidores de verdade, rodando local (não toca na AWS):
 *
 * <ul>
 *   <li>RENOVAI_API_REDIS_URL: main.py da API-Redis (ex.: com fakeredis no lugar do Redis);
 *   <li>RENOVAI_IA_ROTAS_URL: /rotas/otimizar com os schemas e o modelo de rotas do repo IA.
 * </ul>
 *
 * Sem as variáveis, os testes são pulados.
 */
public class RotasMotoristaIntegracaoTest {

    private static String url(String variavel) {
        String v = System.getenv(variavel);
        return v == null || v.isBlank() ? null : (v.endsWith("/") ? v : v + "/");
    }

    /** Mesmo casamento da tela: índice da coleta → status. */
    private static Map<Integer, String> porIndice(List<RotaViagemApiService.Parada> paradas) {
        Map<Integer, String> m = new HashMap<>();
        for (RotaViagemApiService.Parada p : paradas) m.put(Integer.parseInt(p.id), p.status);
        return m;
    }

    @Test
    public void viagemCompletaNaApiRedis() throws Exception {
        String base = url("RENOVAI_API_REDIS_URL");
        assumeTrue("RENOVAI_API_REDIS_URL não definida", base != null);
        RotaViagemApiService api = RotaViagemApiService.criar(base);
        String rota = UUID.randomUUID().toString();
        String motorista = UUID.randomUUID().toString();

        // antes de iniciar: 404 (a tela mostra "Viagem não iniciada")
        assertEquals(404, api.sessao(rota).execute().code());

        assertEquals(201, api.iniciar(new RotaViagemApiService.Inicio(rota, motorista)).execute().code());

        // 12 coletas: acima de 10 a API ordena as chaves como texto (…:10 antes de …:2)
        List<RotaViagemApiService.Parada> envio = new ArrayList<>();
        for (int i = 0; i < 12; i++) envio.add(new RotaViagemApiService.Parada("Rua " + i));
        assertEquals(201, api.adicionarParadas(rota, new RotaViagemApiService.Paradas(envio)).execute().code());

        Response<RotaViagemApiService.Sessao> sessao = api.sessao(rota).execute();
        assertEquals(RotaViagemApiService.INICIADA, sessao.body().status);
        assertEquals(motorista, sessao.body().motoristaId);
        assertNotNull(sessao.body().inicio);

        List<RotaViagemApiService.Parada> lidas = api.paradas(rota).execute().body().paradas;
        assertEquals(12, lidas.size());
        for (RotaViagemApiService.Parada p : lidas) {
            // o id casa com a ordem enviada, seja qual for a posição na lista
            assertEquals("Rua " + p.id, p.endereco);
            assertEquals(RotaViagemApiService.PENDENTE, p.status);
        }

        // marca a coleta 10 e a 2, desmarca a 2 (toque duplo na tela)
        RotaViagemApiService.NovoStatus feita = new RotaViagemApiService.NovoStatus(RotaViagemApiService.CONCLUIDA);
        RotaViagemApiService.NovoStatus pendente = new RotaViagemApiService.NovoStatus(RotaViagemApiService.PENDENTE);
        Response<RotaViagemApiService.Parada> r10 = api.atualizarParada(rota, 10, feita).execute();
        assertEquals(200, r10.code());
        assertEquals("Rua 10", r10.body().endereco);
        assertEquals(200, api.atualizarParada(rota, 2, feita).execute().code());
        assertEquals(200, api.atualizarParada(rota, 2, pendente).execute().code());

        Map<Integer, String> status = porIndice(api.paradas(rota).execute().body().paradas);
        assertEquals(RotaViagemApiService.CONCLUIDA, status.get(10));
        assertEquals(RotaViagemApiService.PENDENTE, status.get(2));
        assertEquals(RotaViagemApiService.PENDENTE, status.get(1));

        // parada fora da rota: 404 (a tela recarrega o andamento)
        assertEquals(404, api.atualizarParada(rota, 99, feita).execute().code());

        Response<RotaViagemApiService.Sessao> fim = api.finalizar(rota).execute();
        assertEquals(200, fim.code());
        assertEquals(RotaViagemApiService.FINALIZADA, fim.body().status);
    }

    /** Contrato da API-Redis com posse da viagem (fix/viagem-paradas-ordem-e-posse). */
    @Test
    public void outroMotoristaNaoAssumeEReenvioNaoZeraOProgresso() throws Exception {
        String base = url("RENOVAI_API_REDIS_URL");
        assumeTrue("RENOVAI_API_REDIS_URL não definida", base != null);
        RotaViagemApiService api = RotaViagemApiService.criar(base);
        String rota = UUID.randomUUID().toString();
        String eu = UUID.randomUUID().toString(), outro = UUID.randomUUID().toString();

        assertEquals(201, api.iniciar(new RotaViagemApiService.Inicio(rota, eu)).execute().code());
        List<RotaViagemApiService.Parada> envio = new ArrayList<>();
        for (int i = 0; i < 12; i++)
            envio.add(new RotaViagemApiService.Parada("Rua " + i, "end-" + i, i + 1));
        assertEquals(201, api.adicionarParadas(rota, new RotaViagemApiService.Paradas(envio)).execute().code());

        Response<RotaViagemApiService.Parada> feita = api.atualizarParada(rota, 11,
                new RotaViagemApiService.NovoStatus(RotaViagemApiService.CONCLUIDA, eu)).execute();
        assertEquals(200, feita.code());
        assertNotNull(feita.body().conclusao);
        assertEquals("end-11", feita.body().enderecoId);
        assertEquals("12", feita.body().ordem);

        // a lista volta em ordem numérica
        List<RotaViagemApiService.Parada> lidas = api.paradas(rota).execute().body().paradas;
        for (int i = 0; i < lidas.size(); i++) assertEquals(String.valueOf(i), lidas.get(i).id);

        // outro motorista: não inicia, não marca, não finaliza
        assertEquals(409, api.iniciar(new RotaViagemApiService.Inicio(rota, outro)).execute().code());
        assertEquals(403, api.atualizarParada(rota, 0,
                new RotaViagemApiService.NovoStatus(RotaViagemApiService.CONCLUIDA, outro)).execute().code());
        assertEquals(403, api.finalizar(rota, new RotaViagemApiService.Motorista(outro)).execute().code());
        assertEquals(eu, api.sessao(rota).execute().body().motoristaId);

        // o mesmo motorista retoma: reenviar não apaga a coleta concluída
        assertEquals(200, api.iniciar(new RotaViagemApiService.Inicio(rota, eu)).execute().code());
        assertEquals(200, api.adicionarParadas(rota, new RotaViagemApiService.Paradas(envio)).execute().code());
        assertEquals(RotaViagemApiService.CONCLUIDA, porIndice(api.paradas(rota).execute().body().paradas).get(11));

        assertEquals(400, api.atualizarParada(rota, 0, new RotaViagemApiService.NovoStatus("FEITA", eu)).execute().code());
        assertEquals(200, api.finalizar(rota, new RotaViagemApiService.Motorista(eu)).execute().code());
        assertEquals(409, api.atualizarParada(rota, 0,
                new RotaViagemApiService.NovoStatus(RotaViagemApiService.CONCLUIDA, eu)).execute().code());
    }

    @Test
    public void finalizarRotaNaoIniciadaDa404() throws Exception {
        String base = url("RENOVAI_API_REDIS_URL");
        assumeTrue("RENOVAI_API_REDIS_URL não definida", base != null);
        assertEquals(404, RotaViagemApiService.criar(base).finalizar(UUID.randomUUID().toString()).execute().code());
    }

    @Test
    public void iaCalculaTempoEDistanciaDaRota() throws Exception {
        String base = url("RENOVAI_IA_ROTAS_URL");
        assumeTrue("RENOVAI_IA_ROTAS_URL não definida", base != null);
        List<RotaOtimizadorIaService.Ponto> paradas = new ArrayList<>();
        paradas.add(new RotaOtimizadorIaService.Ponto("e1", "Avenida Paulista, 1000, São Paulo", "Bela Vista"));
        paradas.add(new RotaOtimizadorIaService.Ponto("e2", "Rua Augusta, 500, São Paulo", "Consolação"));
        paradas.add(new RotaOtimizadorIaService.Ponto("e3", "Avenida Itaquera, 3000, São Paulo", "Itaquera"));
        paradas.add(new RotaOtimizadorIaService.Ponto("e4", "Rua Voluntários da Pátria, 1500, São Paulo", "Santana"));

        Response<RotaOtimizadorIaService.Resultado> r =
                RotaOtimizadorIaService.criar(base)
                        .otimizar(new RotaOtimizadorIaService.Pedido(
                                UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                                new RotaOtimizadorIaService.Ponto("e0", "Praça da Sé, São Paulo", "Sé"),
                                paradas, 9))
                        .execute();

        assertTrue("HTTP " + r.code() + ": " + (r.errorBody() == null ? "" : r.errorBody().string()),
                r.isSuccessful());
        RotaOtimizadorIaService.Resultado res = r.body();
        assertNotNull(res.distanciaKm);
        assertNotNull(res.tempoOrdemAtualMin);
        assertNotNull(res.tempoOtimizadoMin);
        assertTrue(res.distanciaKm > 0);
        assertTrue(res.tempoOrdemAtualMin > 0);
        // a ordem sugerida nunca é pior que a da rota
        assertTrue(res.tempoOtimizadoMin <= res.tempoOrdemAtualMin + 0.01);
        assertEquals(4, res.paradasOrdenadas.size());
        System.out.printf("IA: %.1f km, ordem da rota %.0f min, sugerida %.0f min, economia %.0f min%n",
                res.distanciaKm, res.tempoOrdemAtualMin, res.tempoOtimizadoMin, res.economiaMin);
    }
}
