package com.example.renovai;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/** Trajeto pelas ruas (resposta do OSRM). */
public class RotaRuasTest {

    @Test
    public void decodificaAPolylineDoExemploDaDocumentacao() {
        List<double[]> p = RotaRuas.decodificar("_p~iF~ps|U_ulLnnqC_mqNvxq`@");
        assertEquals(3, p.size());
        assertArrayEquals(new double[] {38.5, -120.2}, p.get(0), 1e-9);
        assertArrayEquals(new double[] {40.7, -120.95}, p.get(1), 1e-9);
        assertArrayEquals(new double[] {43.252, -126.453}, p.get(2), 1e-9);
    }

    @Test
    public void urlUsaLongitudeLatitudeComPonto() {
        String url = RotaRuas.url(Arrays.asList(new double[] {-23.5, -46.6}, new double[] {-23.51, -46.61}));
        assertTrue(url, url.startsWith(RotaRuas.OSRM + "-46.600000,-23.500000;-46.610000,-23.510000?"));
        assertTrue(url.contains("steps=true"));
    }

    @Test
    public void umTrechoPorParDeParadasJuntandoOsPassos() {
        // 2 legs; a primeira com 2 passos que se encostam no ponto (40.7, -120.95)
        String json = "{\"code\":\"Ok\",\"routes\":[{\"legs\":["
                + "{\"steps\":[{\"geometry\":\"_p~iF~ps|U_ulLnnqC\"},{\"geometry\":\"_flwFn`faV_mqNvxq`@\"}]},"
                + "{\"steps\":[{\"geometry\":\"_p~iF~ps|U_ulLnnqC\"}]}]}]}";
        List<List<double[]>> t = RotaRuas.trechos(json, 2);
        assertNotNull(t);
        assertEquals(2, t.size());
        assertEquals(3, t.get(0).size()); // o ponto repetido entre os passos entra uma vez só
        assertArrayEquals(new double[] {43.252, -126.453}, t.get(0).get(2), 1e-9);
        assertEquals(2, t.get(1).size());
    }

    @Test
    public void respostaSemRotaOuComTrechosFaltandoVoltaALinhaReta() {
        assertNull(RotaRuas.trechos("{\"code\":\"NoRoute\",\"routes\":[]}", 1));
        String umaLeg = "{\"code\":\"Ok\",\"routes\":[{\"legs\":[{\"steps\":[{\"geometry\":\"_p~iF~ps|U_ulLnnqC\"}]}]}]}";
        assertNotNull(RotaRuas.trechos(umaLeg, 1));
        assertNull(RotaRuas.trechos(umaLeg, 2));
    }
}
