package com.example.renovai;

import static org.junit.Assert.*;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** "Manter conectado": quando o token guardado ainda pode abrir a Home sem login. */
public class SessaoSalvaTest {

    private static final long AGORA = 1_793_000_000L;

    /** JWT no formato da Spring (HS256, sub, role, iat, exp); a assinatura não é conferida no app. */
    private static String jwt(String payload) {
        Base64.Encoder b = Base64.getUrlEncoder().withoutPadding();
        return b.encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8)) + "."
                + b.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + ".assinatura";
    }

    @Test
    public void tokenDentroDaValidadeAbreAHome() {
        String t = jwt("{\"sub\":\"a@b.com\",\"role\":\"GESTOR_COOPERATIVA\",\"iat\":" + AGORA
                + ",\"exp\":" + (AGORA + 86_400) + "}");
        assertEquals(AGORA + 86_400, SessaoSalva.expiracao(t));
        assertTrue(SessaoSalva.tokenValido(t, AGORA));
    }

    @Test
    public void tokenVencidoOuQuaseVencendoVaiParaOLogin() {
        assertFalse(SessaoSalva.tokenValido(jwt("{\"exp\":" + (AGORA - 1) + "}"), AGORA));
        // vence dentro da margem: não adianta abrir a Home
        assertFalse(SessaoSalva.tokenValido(jwt("{\"exp\":" + (AGORA + 60) + "}"), AGORA));
        // controle: logo depois da margem ainda vale
        assertTrue(SessaoSalva.tokenValido(
                jwt("{\"exp\":" + (AGORA + SessaoSalva.MARGEM_SEGUNDOS + 1) + "}"), AGORA));
    }

    @Test
    public void tokenInvalidoNuncaAbreAHome() {
        assertFalse(SessaoSalva.tokenValido(null, AGORA));
        assertFalse(SessaoSalva.tokenValido("", AGORA));
        assertFalse(SessaoSalva.tokenValido("nao-e-jwt", AGORA));
        assertFalse(SessaoSalva.tokenValido("a.@@@.c", AGORA));
        assertFalse(SessaoSalva.tokenValido(jwt("{\"sub\":\"sem-exp\"}"), AGORA));
        assertFalse(SessaoSalva.tokenValido(jwt("[1,2]"), AGORA));
        assertFalse(SessaoSalva.tokenValido(jwt("{\"exp\":\"amanha\"}"), AGORA));
    }
}
