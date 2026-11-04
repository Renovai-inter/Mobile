package com.example.renovai;

import org.junit.Test;
import static org.junit.Assert.*;

public class PerfilEmpresaValidacaoTest {
    @Test public void camposObrigatoriosNaoAceitamVazio() {
        for (String campo : new String[] {"nomeEmpresa", "email", "cnpj", "endereco", "telefone"})
            assertNotNull(PerfilEmpresaValidacao.erro(campo, "   "));
    }
    @Test public void opcionaisPodemSerLimpos() {
        for (String campo : new String[] {"cidade", "descricao"})
            assertNull(PerfilEmpresaValidacao.erro(campo, ""));
    }
    @Test public void emailInvalidoNaoDeveSairDoFormulario() {
        assertNotNull(PerfilEmpresaValidacao.erro("email", "nome@"));
        assertNotNull(PerfilEmpresaValidacao.erro("email", "nome @empresa.com"));
        assertNull(PerfilEmpresaValidacao.erro("email", "nome@empresa.com.br"));
    }
    @Test public void telefoneAceitaFormatacaoEDdd() {
        assertNull(PerfilEmpresaValidacao.erro("telefone", "(11) 99999-9999"));
        assertNull(PerfilEmpresaValidacao.erro("telefone", "+55 11 99999-9999"));
        assertNotNull(PerfilEmpresaValidacao.erro("telefone", "123"));
        assertNotNull(PerfilEmpresaValidacao.erro("telefone", "ligue amanhã"));
    }
    @Test public void limitesCorrespondemAosCamposDaApi() {
        assertNotNull(PerfilEmpresaValidacao.erro("cidade", new String(new char[101]).replace('\0', 'a')));
        assertNull(PerfilEmpresaValidacao.erro("cnpj", "12.345.678/0001-90"));
        assertNotNull(PerfilEmpresaValidacao.erro("cnpj", "123"));
        assertNull(PerfilEmpresaValidacao.erro("descricao", new String(new char[4001]).replace('\0', 'a')));
    }
}
