package com.example.renovai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.renovai.Notificacoes.Perfil;

import org.junit.Test;

/** Quem recebe push em cada perfil e para onde o toque leva (regras da API em NotificacaoService). */
public class NotificacoesTest {

    @Test
    public void gestorEAdminDaCooperativaRecebemComoGestor() {
        assertEquals(Perfil.GESTOR, Notificacoes.perfilDoRole("GESTOR_COOPERATIVA", true, false));
        assertEquals(Perfil.GESTOR, Notificacoes.perfilDoRole("ADMIN_COOPERATIVA", true, false));
    }

    @Test
    public void cooperadoRecebeComoCooperado() {
        assertEquals(Perfil.COOPERADO, Notificacoes.perfilDoRole("COOPERADO", true, false));
    }

    @Test
    public void perfilDaEmpresaRecebeComoEmpresa() {
        assertEquals(Perfil.EMPRESA, Notificacoes.perfilDoRole("GESTOR_EMPRESA", false, true));
        // o "GESTOR" do role da empresa não pode virar gestor de cooperativa
        assertEquals(Perfil.EMPRESA, Notificacoes.perfilDoRole("GESTOR_EMPRESA", true, true));
    }

    @Test
    public void quemNaoTemAvisosNaoRegistraOAparelho() {
        assertNull(Notificacoes.perfilDoRole("MOTORISTA", true, false));
        assertNull(Notificacoes.perfilDoRole("ADMIN_SITE", false, false));
        assertNull(Notificacoes.perfilDoRole(null, true, true));
        // sem funcionário ou empresa resolvidos a API não saberia a cooperativa/empresa da conta
        assertNull(Notificacoes.perfilDoRole("GESTOR_COOPERATIVA", false, false));
        assertNull(Notificacoes.perfilDoRole("COOPERADO", false, false));
        assertNull(Notificacoes.perfilDoRole("GESTOR_EMPRESA", false, false));
    }

    @Test
    public void toqueAbreAAbaDoAvisoEmCadaPerfil() {
        assertEquals(R.id.nav_g_coletas, Notificacoes.abaDoAviso(Perfil.GESTOR, "coletas"));
        assertEquals(R.id.nav_g_estoque, Notificacoes.abaDoAviso(Perfil.GESTOR, "estoque"));
        assertEquals(R.id.nav_g_home, Notificacoes.abaDoAviso(Perfil.GESTOR, "pedidos"));
        assertEquals(R.id.nav_rateios, Notificacoes.abaDoAviso(Perfil.COOPERADO, "rateios"));
        assertEquals(R.id.nav_home_cooperado, Notificacoes.abaDoAviso(Perfil.COOPERADO, "estoque"));
        assertEquals(R.id.nav_pedidos, Notificacoes.abaDoAviso(Perfil.EMPRESA, "pedidos"));
    }

    @Test
    public void propostasDoGestorAbremOsPedidos() {
        assertTrue(Notificacoes.abrePedidosDoGestor(Perfil.GESTOR, "pedidos"));
        assertFalse(Notificacoes.abrePedidosDoGestor(Perfil.GESTOR, "coletas"));
        assertFalse(Notificacoes.abrePedidosDoGestor(Perfil.EMPRESA, "pedidos"));
        assertFalse(Notificacoes.abrePedidosDoGestor(Perfil.COOPERADO, "estoque"));
    }
}
