package com.example.renovai;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * "Manter conectado": decide se a sessão guardada no aparelho ainda vale para abrir a Home sem
 * passar pelo login (e aproveitar o cache offline). O app não confere a assinatura do token — quem
 * faz isso é a API a cada chamada —, mas nunca reaproveita um token vencido, malformado ou sem
 * data de expiração.
 */
public final class SessaoSalva {

    /** Folga para o token não vencer logo depois de abrir a Home. */
    static final long MARGEM_SEGUNDOS = 5 * 60;

    private SessaoSalva() {}

    /** Campo "exp" do JWT (segundos desde 1970), ou -1 se o token não for um JWT legível. */
    public static long expiracao(String token) {
        if (token == null) return -1;
        String[] partes = token.trim().split("\\.");
        if (partes.length != 3) return -1;
        try {
            String json = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
            JsonObject payload = JsonParser.parseString(json).getAsJsonObject();
            JsonElement exp = payload.get("exp");
            return exp == null || !exp.isJsonPrimitive() ? -1 : exp.getAsLong();
        } catch (RuntimeException invalido) {
            return -1;
        }
    }

    public static boolean tokenValido(String token, long agoraSegundos) {
        long exp = expiracao(token);
        return exp > 0 && exp - MARGEM_SEGUNDOS > agoraSegundos;
    }
}
