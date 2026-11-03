package com.example.renovai;

import com.example.renovai.dto.response.CategoriaMaterialResponse;
import com.example.renovai.dto.response.GestorResponses;
import com.example.renovai.dto.response.MaterialResponse;
import com.google.gson.Gson;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class NegociacaoApresentacaoTest {
    private final Gson gson = new Gson();

    @Test
    public void contrapropostaSemObservacaoTemMensagemEspecial() {
        for (String texto : new String[] {null, "", "   "}) {
            GestorResponses.Mensagem m = new GestorResponses.Mensagem();
            m.tipoMensagem = "CONTRAPROPOSTA";
            m.mensagem = texto;
            assertEquals("Contraproposta enviada", m.textoExibicao());
        }
    }

    @Test
    public void contrapropostaPreservaObservacaoETextoNormalPermaneceIgual() {
        GestorResponses.Mensagem m = new GestorResponses.Mensagem();
        m.tipoMensagem = "CONTRAPROPOSTA";
        m.mensagem = "Retirada pela empresa";
        assertEquals("Contraproposta enviada\nObservação: Retirada pela empresa", m.textoExibicao());
        m.tipoMensagem = "TEXTO";
        assertEquals(m.mensagem, m.textoExibicao());
        m.tipoMensagem = "SISTEMA";
        assertEquals(m.mensagem, m.textoExibicao());
    }

    @Test
    public void materialMostraCategoriaEspecificaEHierarquiaSemTrocarIdentidade() {
        MaterialResponse m = material();
        assertEquals("Latinhas › Alumínio › Metal", m.nomeExibicao(Arrays.asList(
                categoria("metal", null, "Metal"),
                categoria("aluminio", "metal", "Alumínio"),
                categoria("lata", "aluminio", "Latinhas"))));
        assertEquals("material-1", m.getMaterialId());
    }

    @Test
    public void ausenciaDeCategoriasMantemNomeRecebidoDaApi() {
        assertEquals("Latinhas", material().nomeExibicao(Collections.emptyList()));
        MaterialResponse semNome = gson.fromJson("{}", MaterialResponse.class);
        assertEquals("Material", semNome.nomeExibicao(Collections.emptyList()));
    }

    @Test
    public void categoriaPaiParcialUsaNomeFornecidoPelaApi() {
        CategoriaMaterialResponse c = gson.fromJson(
                "{\"categoriaId\":\"lata\",\"nomeCategoria\":\"Latinhas\","
                        + "\"categoriaPaiId\":\"aluminio\",\"categoriaPaiNome\":\"Alumínio\"}",
                CategoriaMaterialResponse.class);
        assertEquals("Latinhas › Alumínio", material().nomeExibicao(Collections.singletonList(c)));
    }

    @Test
    public void hierarquiaCiclicaNaoTravaListagem() {
        assertEquals("Latinhas › Alumínio", material().nomeExibicao(Arrays.asList(
                categoria("lata", "aluminio", "Latinhas"),
                categoria("aluminio", "lata", "Alumínio"))));
    }

    private MaterialResponse material() {
        return gson.fromJson("{\"materialId\":\"material-1\",\"categoriaId\":\"lata\","
                + "\"categoriaNome\":\"Latinhas\"}", MaterialResponse.class);
    }

    private CategoriaMaterialResponse categoria(String id, String pai, String nome) {
        return gson.fromJson("{\"categoriaId\":\"" + id + "\",\"categoriaPaiId\":"
                + (pai == null ? "null" : "\"" + pai + "\"")
                + ",\"nomeCategoria\":\"" + nome + "\"}", CategoriaMaterialResponse.class);
    }
}
