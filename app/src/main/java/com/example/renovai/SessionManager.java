package com.example.renovai;

import android.content.Context;
import android.content.SharedPreferences;

public final class SessionManager {

    private static final String PREFS_NAME = "renovai_session";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";
    private static final String KEY_EMPRESA_ID = "empresa_id";

    private static SharedPreferences prefs;

    private SessionManager() {
    }

    public static void init(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void salvarSessao(String token, String email, String role) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_EMAIL, email)
                .putString(KEY_ROLE, role)
                .remove(KEY_EMPRESA_ID) // sessão nova: descarta empresaId de sessão anterior
                .apply();
    }

    public static String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public static String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public static String getRole() {
        return prefs.getString(KEY_ROLE, null);
    }

    /**
     * empresaId não vem no /auth/login (a API não devolve isso hoje) — é resolvido
     * à parte via PerfilResolver e guardado aqui depois. Ver AuthController.login()
     * e HomeFragment (que também tenta resolver como fallback, se ainda estiver null).
     */
    public static void salvarEmpresaId(String empresaId) {
        prefs.edit().putString(KEY_EMPRESA_ID, empresaId).apply();
    }

    public static String getEmpresaId() {
        return prefs.getString(KEY_EMPRESA_ID, null);
    }

    /**
     * Atalho SÓ PARA TESTES enquanto o login de empresa não fecha de ponta a ponta
     * (ver observação sobre AuthService.login no chat). Chame isso manualmente uma
     * vez (ex: num botão escondido, ou direto no onCreate da EmpresaActivity) com um
     * empresaId real copiado do Swagger (GET /empresas) pra testar a Home sozinha.
     * Remover quando o login de empresa estiver funcionando de verdade.
     */
    public static void forcarEmpresaIdDeTeste(String empresaId) {
        salvarEmpresaId(empresaId);
    }

    public static boolean estaLogado() {
        return getToken() != null;
    }

    public static void logout() {
        prefs.edit().clear().apply();
    }
}
