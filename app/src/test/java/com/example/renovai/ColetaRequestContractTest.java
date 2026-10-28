package com.example.renovai;

import static org.junit.Assert.*;

import com.example.renovai.controller.ColetaController;
import com.example.renovai.dto.request.ColetaRequest;
import com.example.renovai.dto.request.MaterialRequest;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

public class ColetaRequestContractTest {
    private final Gson gson = new Gson();

    @Test
    public void coletaComMateriaisLevaEquipeETriagem() {
        ColetaRequest pedido =
                ColetaController.montarRequest("coop", new BigDecimal("10"), "INTERNO", null, null)
                        .comTriagem(true)
                        .comMateriais("equipe", Arrays.asList(
                                new ColetaRequest.Material("pet", new BigDecimal("6")),
                                new ColetaRequest.Material("papel", new BigDecimal("4"))));

        JsonObject json = gson.toJsonTree(pedido).getAsJsonObject();

        assertEquals("equipe", json.get("equipeId").getAsString());
        assertTrue(json.get("precisaTriagem").getAsBoolean());
        // descrição e data prevista não vão para a API (o banco não tem os campos): ficam no Firestore
        assertFalse(json.has("descricao"));
        assertFalse(json.has("dataPrevistaTriagem"));
        assertEquals(2, json.getAsJsonArray("materiais").size());
        assertEquals("pet", json.getAsJsonArray("materiais").get(0).getAsJsonObject().get("materialId").getAsString());
    }

    @Test
    public void coletaSemMateriaisNaoMandaCamposVazios() {
        ColetaRequest pedido =
                ColetaController.montarRequest("coop", BigDecimal.ONE, "INTERNO", null, null)
                        .comTriagem(true)
                        .comMateriais(null, Arrays.asList());

        JsonObject json = gson.toJsonTree(pedido).getAsJsonObject();

        assertFalse(json.has("materiais"));
        assertFalse(json.has("equipeId"));
    }

    @Test
    public void dataPrevistaFicaNoFormatoBrasileiro() {
        assertEquals("10/11/2026", ColetaDetalhes.dataFormatada("2026-11-10"));
        assertNull(ColetaDetalhes.dataFormatada(null));
    }

    @Test
    public void materialLevaAFotoDoCloudinary() {
        JsonObject json = gson.toJsonTree(new MaterialRequest(
                "cat", "coop", new BigDecimal("2.5"), true, "https://res.cloudinary.com/x.jpg")).getAsJsonObject();

        assertEquals("cat", json.get("categoriaId").getAsString());
        assertEquals("https://res.cloudinary.com/x.jpg", json.get("imagemUrl").getAsString());
    }
}
