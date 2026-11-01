package com.example.renovai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** Corpo do POST /atividade/login (API-Redis) para cada tipo de conta. */
public class AtividadeTest {

    private static final String FUNC = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
    private static final String EMPRESA = "cb24192b-9ff9-4556-b3d5-c2b5b3a58b0e";

    @Test
    public void funcionarioDaCooperativaVaiComoCooperadoComOFuncionarioId() {
        for (String role : new String[] {"COOPERADO", "GESTOR_COOPERATIVA", "ADMIN_COOPERATIVA", "MOTORISTA"}) {
            Atividade.Corpo c = Atividade.daConta(role, FUNC, null);
            assertEquals(role, "cooperado", c.tipo);
            assertEquals(role, FUNC, c.id);
        }
    }

    @Test
    public void perfilDaEmpresaVaiComoEmpresaComOEmpresaId() {
        Atividade.Corpo c = Atividade.daConta("GESTOR_EMPRESA", null, EMPRESA);
        assertEquals("empresa", c.tipo);
        assertEquals(EMPRESA, c.id);
        // mesmo com sobra de funcionário na sessão, a empresa usa o empresa_id
        assertEquals(EMPRESA, Atividade.daConta("GESTOR_EMPRESA", FUNC, EMPRESA).id);
    }

    @Test
    public void semVinculoOuAdminDoSiteNaoRegistra() {
        assertNull(Atividade.daConta("ADMIN_SITE", FUNC, EMPRESA));
        assertNull(Atividade.daConta(null, FUNC, EMPRESA));
        assertNull(Atividade.daConta("COOPERADO", null, null));
        assertNull(Atividade.daConta("GESTOR_EMPRESA", FUNC, " "));
    }

    @Test
    public void corpoSaiNoFormatoDaApi() {
        String json = new com.google.gson.Gson().toJson(Atividade.daConta("COOPERADO", FUNC, null));
        assertEquals("{\"tipo\":\"cooperado\",\"id\":\"" + FUNC + "\"}", json);
    }
}
