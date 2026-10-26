package com.example.renovai;

import org.junit.Test;
import static org.junit.Assert.*;

public class NomeCabecalhoTest {
    @Test public void tiraCooperativaDoComeco() {
        assertEquals("Recicla Vida", GestorUi.nomeCabecalho("Cooperativa Recicla Vida"));
        assertEquals("Reciclagem Vida", GestorUi.nomeCabecalho("cooperativa de Reciclagem Vida"));
        assertEquals("Catadores", GestorUi.nomeCabecalho("Coop. dos catadores"));
    }

    @Test public void mantemNomesQueNaoComecamComCooperativa() {
        assertEquals("Recicla Mais Cooperativa", GestorUi.nomeCabecalho(" Recicla Mais Cooperativa "));
        assertEquals("Cooperativa", GestorUi.nomeCabecalho("Cooperativa"));
        assertEquals("Renovaí", GestorUi.nomeCabecalho(null));
        assertEquals("Renovaí", GestorUi.nomeCabecalho("  "));
    }
}
