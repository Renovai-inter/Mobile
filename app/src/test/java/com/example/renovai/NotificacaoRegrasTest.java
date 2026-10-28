package com.example.renovai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.renovai.NotificacaoRegras.Mudanca;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificacaoRegrasTest {

    private static Map<String, String> mapa(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    private static String neg(int estado, Double valor) {
        return NotificacaoRegras.retratoNegociacao(estado, valor);
    }

    @Test
    public void primeiraVerificacaoNaoGeraAvisos() {
        assertTrue(NotificacaoRegras.comparar(null, mapa("a", "PENDENTE")).isEmpty());
    }

    @Test
    public void detectaItemNovoEMudancaDeEstado() {
        List<Mudanca> r =
                NotificacaoRegras.comparar(
                        mapa("a", "PENDENTE", "b", "EM_ANDAMENTO"),
                        mapa("a", "PENDENTE", "b", "CONCLUIDA", "c", "PENDENTE"));
        assertEquals(2, r.size());
        Map<String, Mudanca> porId = new HashMap<>();
        for (Mudanca m : r) porId.put(m.id, m);
        assertEquals("EM_ANDAMENTO", porId.get("b").antigo);
        assertEquals("CONCLUIDA", porId.get("b").novo);
        assertTrue(porId.get("c").ehNovo());
        assertNull(porId.get("c").antigo);
    }

    @Test
    public void estoqueAvisaSoNaQuedaAbaixoDoLimite() {
        assertTrue(NotificacaoRegras.caiuAbaixoDoLimite("1200.0", "999.5"));
        assertTrue(NotificacaoRegras.caiuAbaixoDoLimite("1000.0", "800.0"));
        assertFalse(NotificacaoRegras.caiuAbaixoDoLimite("900.0", "800.0")); // já estava baixo
        assertFalse(NotificacaoRegras.caiuAbaixoDoLimite("800.0", "1500.0")); // reposição
        assertFalse(NotificacaoRegras.caiuAbaixoDoLimite(null, "500.0")); // material novo
    }

    @Test
    public void propostaFinalizadaSoNaTransicao() {
        Mudanca fim =
                NotificacaoRegras.comparar(
                                mapa("n", neg(EmpresaStatus.ACEITO, 500.0)),
                                mapa("n", neg(EmpresaStatus.FINALIZADO, 500.0)))
                        .get(0);
        assertTrue(NotificacaoRegras.chegouAoEstado(fim, EmpresaStatus.FINALIZADO));
        assertFalse(NotificacaoRegras.chegouAoEstado(fim, EmpresaStatus.ACEITO));
        assertFalse(NotificacaoRegras.ehContraproposta(fim));
    }

    @Test
    public void contrapropostaQuandoValorMudaEmNegociacao() {
        Mudanca contra =
                NotificacaoRegras.comparar(
                                mapa("n", neg(EmpresaStatus.EM_NEGOCIACAO, 500.0)),
                                mapa("n", neg(EmpresaStatus.EM_NEGOCIACAO, 450.0)))
                        .get(0);
        assertTrue(NotificacaoRegras.ehContraproposta(contra));
        assertFalse(NotificacaoRegras.chegouAoEstado(contra, EmpresaStatus.FINALIZADO));
    }

    @Test
    public void negociacaoNovaOuRecusadaNaoEhContraproposta() {
        Mudanca nova =
                NotificacaoRegras.comparar(mapa(), mapa("n", neg(EmpresaStatus.EM_NEGOCIACAO, 500.0)))
                        .get(0);
        assertFalse(NotificacaoRegras.ehContraproposta(nova));
        assertFalse(NotificacaoRegras.chegouAoEstado(nova, EmpresaStatus.EM_NEGOCIACAO));

        Mudanca recusada =
                NotificacaoRegras.comparar(
                                mapa("n", neg(EmpresaStatus.EM_NEGOCIACAO, 500.0)),
                                mapa("n", neg(EmpresaStatus.RECUSADO, 400.0)))
                        .get(0);
        assertFalse(NotificacaoRegras.ehContraproposta(recusada));
    }
}
