package com.example.renovai;

import com.example.renovai.dto.response.ColetaResponse;
import com.google.gson.Gson;
import org.junit.Test;
import static org.junit.Assert.*;

public class ImagemUrlTest {
    @Test public void aceitaUrlVersionadaSemAlterarAChaveDoCache() {
        String url = "https://res.cloudinary.com/ki7bmzxt/image/upload/v123/Renovai/foto.jpg";
        assertEquals(url, ImagemUrl.normalizar("  " + url + "  "));
        assertNotEquals(ImagemUrl.normalizar(url), ImagemUrl.normalizar(url.replace("v123", "v124")));
    }

    @Test public void rejeitaValoresQueNaoSaoImagensRemotasValidas() {
        for (String value : new String[] {null, "", "  ", "null", "http://example.com/a.jpg",
                "file:///sdcard/foto.jpg", "content://media/1", "https:///foto.jpg",
                "https://user:secret@example.com/foto.jpg", "https://example.com/a b.jpg"})
            assertNull(value, ImagemUrl.normalizar(value));
    }

    @Test public void leUrlDaApiEmCamelCaseEDoBancoEmSnakeCase() {
        Gson gson = new Gson();
        String url = "https://res.cloudinary.com/demo/image/upload/foto.jpg";
        assertEquals(url, gson.fromJson("{\"imagemUrl\":\"" + url + "\"}", ColetaResponse.class).getImagemUrl());
        assertEquals(url, gson.fromJson("{\"imagem_url\":\"" + url + "\"}", ColetaResponse.class).getImagemUrl());
        assertNull(gson.fromJson("{\"imagemUrl\":null}", ColetaResponse.class).getImagemUrl());
    }
}
