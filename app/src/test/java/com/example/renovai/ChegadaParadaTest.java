package com.example.renovai;

import static org.junit.Assert.*;

import com.example.renovai.controller.ChegadaParada;

import org.junit.Test;

/** Conclusão automática da parada quando o motorista chega perto dela. */
public class ChegadaParadaTest {

    private final ChegadaParada chegada = new ChegadaParada();

    @Test
    public void duasLeiturasDentroDoRaioConcluemUmaVez() {
        assertFalse(chegada.posicao(0, 300, 10));
        assertFalse(chegada.posicao(0, 60, 10)); // primeira leitura perto
        assertTrue(chegada.posicao(0, 50, 10)); // segunda: chegou
        assertFalse(chegada.posicao(0, 40, 10)); // não repete na leitura seguinte
    }

    @Test
    public void passarPertoUmaVezSoNaoConclui() {
        assertFalse(chegada.posicao(0, 70, 10));
        assertFalse(chegada.posicao(0, 200, 10)); // saiu: zera
        assertFalse(chegada.posicao(0, 70, 10));
        assertTrue(chegada.posicao(0, 70, 10)); // controle: duas seguidas concluem
    }

    @Test
    public void gpsImprecisoNaoContaNemZera() {
        assertFalse(chegada.posicao(0, 30, 10));
        assertFalse(chegada.posicao(0, 30, 200)); // precisão ruim: ignora
        assertFalse(chegada.posicao(0, 30, 0)); // precisão desconhecida: ignora
        assertTrue(chegada.posicao(0, 30, 12));
    }

    @Test
    public void trocarDeParadaRecomecaAContagem() {
        assertFalse(chegada.posicao(0, 30, 10));
        assertFalse(chegada.posicao(1, 30, 10)); // a parada 0 foi concluída por outro meio
        assertTrue(chegada.posicao(1, 30, 10));
        assertFalse(chegada.posicao(-1, 0, 0)); // nenhuma parada: nada a fazer
    }

    @Test
    public void depoisDeDesfeitaSoVoltaAConcluirAoSairEVoltar() {
        chegada.posicao(0, 30, 10);
        assertTrue(chegada.posicao(0, 30, 10));
        chegada.desfeita(0);
        for (int i = 0; i < 5; i++) assertFalse(chegada.posicao(0, 30, 10)); // continua parado ali
        assertFalse(chegada.posicao(0, 120, 10)); // ainda dentro da saída
        assertFalse(chegada.posicao(0, 30, 10));
        assertFalse(chegada.posicao(0, 200, 10)); // afastou-se
        assertFalse(chegada.posicao(0, 30, 10));
        assertTrue(chegada.posicao(0, 30, 10)); // voltou: conclui de novo
    }
}
