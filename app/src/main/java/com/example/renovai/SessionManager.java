package com.example.renovai;

import android.content.Context;
import android.content.SharedPreferences;

public final class SessionManager {

    private static final String PREFS_NAME = "renovai_session";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";

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

    public static boolean estaLogado() {
        return getToken() != null;
    }

    public static void logout() {
        prefs.edit().clear().apply();
    }
}
