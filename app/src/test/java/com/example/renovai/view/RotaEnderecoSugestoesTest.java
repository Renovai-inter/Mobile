package com.example.renovai.view;

import org.junit.Test;
import static org.junit.Assert.*;

public class RotaEnderecoSugestoesTest {
    @Test public void preservaNumeroCidadeEstadoECepDoProvedor() {
        assertEquals("Avenida Paulista, 1000 - Bela Vista, São Paulo - SP, 01310-100, Brasil",
                RotaEnderecoSugestoes.normalizar(
                        "  Avenida Paulista, 1000 - Bela Vista, São Paulo - SP, 01310-100, Brasil  "));
    }

    @Test public void removeQuebrasEEspacosDuplicadosSemMudarONumero() {
        assertEquals("Rua Augusta, 500A - Consolação, São Paulo - SP",
                RotaEnderecoSugestoes.normalizar(
                        "Rua  Augusta,\n500A -\tConsolação,\u00a0 São Paulo - SP"));
    }

    @Test public void enderecoAusenteNaoViraSugestao() {
        assertEquals("", RotaEnderecoSugestoes.normalizar(null));
        assertEquals("", RotaEnderecoSugestoes.normalizar(" \n\t "));
    }
}
