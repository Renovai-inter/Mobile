package com.example.renovai;

import static org.junit.Assert.*;

import com.example.renovai.view.AdminHomeActivity;

import okhttp3.Request;

import org.junit.Test;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AdminApiContractTest {
    private final AdminApiService api =
            new Retrofit.Builder()
                    .baseUrl("http://localhost/api/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(AdminApiService.class);

    private static void rota(Request r, String metodo, String caminho) {
        assertEquals(metodo, r.method());
        assertEquals(caminho, r.url().encodedPath());
    }

    @Test
    public void listasUsamAsRotasLiberadasParaOAdminDoSite() {
        rota(api.cooperativas().request(), "GET", "/api/cooperativas");
        rota(api.empresas().request(), "GET", "/api/empresas");
        rota(api.perfis().request(), "GET", "/api/perfis");
        rota(api.pedidos().request(), "GET", "/api/pedidos");
    }

    @Test
    public void acoesUsamORecursoDoItem() {
        rota(api.excluirCooperativa("c1").request(), "DELETE", "/api/cooperativas/c1");
        rota(api.excluirEmpresa("e1").request(), "DELETE", "/api/empresas/e1");
        rota(api.desativarPerfil("p1").request(), "PUT", "/api/perfis/p1/desativar");
        rota(api.excluirPerfil("p1").request(), "DELETE", "/api/perfis/p1");
    }

    @Test
    public void reconheceSoOAdminDoSite() {
        assertTrue(AdminHomeActivity.ehAdminDoSite("ADMIN_SITE"));
        assertTrue(AdminHomeActivity.ehAdminDoSite("ROLE_ADMIN_SITE"));
        assertFalse(AdminHomeActivity.ehAdminDoSite("ADMIN_COOPERATIVA"));
        assertFalse(AdminHomeActivity.ehAdminDoSite("GESTOR_EMPRESA"));
        assertFalse(AdminHomeActivity.ehAdminDoSite(null));
    }
}
