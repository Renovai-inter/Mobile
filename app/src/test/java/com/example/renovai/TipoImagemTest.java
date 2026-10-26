package com.example.renovai;

import com.example.renovai.controller.TriagemController;
import com.example.renovai.dto.response.TriagemResponse;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.Test;
import static org.junit.Assert.*;

public class TipoImagemTest {
    @Test public void cadaCategoriaTemPastaEChaveDeCacheDistintas() {
        HashSet<String> folders = new HashSet<>(), keys = new HashSet<>();
        for (TipoImagem tipo : TipoImagem.values()) {
            assertTrue(folders.add(tipo.pastaCloudinary()));
            assertTrue(keys.add(tipo.chaveCache()));
        }
        assertEquals("Renovai/coletas", TipoImagem.COLETA.pastaCloudinary());
        assertEquals("Renovai/triagens", TipoImagem.TRIAGEM.pastaCloudinary());
        assertEquals("Renovai/perfis", TipoImagem.PERFIL.pastaCloudinary());
    }

    @Test public void grupoTriagemUsaImagemDaTriagemComFoto() {
        Gson gson = new Gson();
        TriagemResponse semFoto = gson.fromJson("{\"triagemId\":\"t1\",\"coletaId\":\"c1\"}", TriagemResponse.class);
        String url = "https://res.cloudinary.com/demo/image/upload/Renovai/triagens/foto.jpg";
        TriagemResponse comFoto = gson.fromJson("{\"triagemId\":\"t2\",\"coletaId\":\"c1\",\"imagem_url\":\"" + url + "\"}", TriagemResponse.class);
        assertEquals(url, TriagemController.agruparPorColeta(Arrays.asList(semFoto, comFoto)).get(0).imagemUrl);
    }

    @Test public void triagemSemImagemNaoRecebeFotoDeOutraColetaOuTriagem() {
        Gson gson = new Gson();
        TriagemResponse a = gson.fromJson("{\"triagemId\":\"t1\",\"coletaId\":\"c1\"}", TriagemResponse.class);
        TriagemResponse b = gson.fromJson("{\"triagemId\":\"t2\",\"coletaId\":\"c2\",\"imagemUrl\":\"https://example.com/triagem.jpg\"}", TriagemResponse.class);
        assertNull(TriagemController.agruparPorColeta(Arrays.asList(a, b)).get(0).imagemUrl);
        assertEquals("https://example.com/triagem.jpg", TriagemController.agruparPorColeta(Arrays.asList(a, b)).get(1).imagemUrl);
    }
}
