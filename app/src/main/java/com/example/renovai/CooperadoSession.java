package com.example.renovai;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Cache do Funcionario (cooperado) logado — funcionarioId, cooperativaId,
 * cooperativaNome, cargo e nome do usuário.
 *
 * Por que isso existe como arquivo separado: o prompt de implementação pede
 * explicitamente para NÃO modificar SessionManager. Mas SessionManager só sabe
 * guardar token/email/role/empresaId — não tem nenhum campo para o
 * funcionarioId de um cooperado. Sem persistir isso em algum lugar, nenhuma tela
 * de Cooperado saberia "quem" está logado depois que o app é reaberto (o token
 * continua válido, mas o funcionarioId se perderia).
 *
 * Esta classe resolve isso do mesmo jeito que SessionManager resolve empresaId:
 * SharedPreferences próprio (arquivo "renovai_cooperado_session", separado do
 * "renovai_session" usado por SessionManager — não há conflito nem sobreposição).
 * É preenchida uma vez, logo após o login bem-sucedido, por AuthController
 * (usando FuncionarioResolver) e lida por todas as Activities da área Cooperado.
 */
public final class CooperadoSession {

    private static final String PREFS_NAME = "renovai_cooperado_session";
    private static final String KEY_USUARIO_ID = "usuario_id";
    private static final String KEY_FUNCIONARIO_ID = "funcionario_id";
    private static final String KEY_COOPERATIVA_ID = "cooperativa_id";
    private static final String KEY_COOPERATIVA_NOME = "cooperativa_nome";
    private static final String KEY_CARGO = "cargo";
    private static final String KEY_USUARIO_NOME = "usuario_nome";

    private static SharedPreferences prefs;

    private CooperadoSession() {
    }

    public static void init(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * usuarioId é guardado à parte (ver salvarUsuarioId) assim que o login
     * termina, ANTES de sabermos se a resolução do Funcionario vai dar certo.
     * Isso permite tentar resolver de novo mais tarde (ex.: PerfilCooperadoController)
     * sem precisar de um novo login, caso a primeira tentativa falhe por causa de
     * uma falha de rede passageira.
     */
    public static void salvarUsuarioId(String usuarioId) {
        prefs.edit().putString(KEY_USUARIO_ID, usuarioId).apply();
    }

    public static String getUsuarioId() {
        return prefs.getString(KEY_USUARIO_ID, null);
    }

    public static void salvar(String funcionarioId, String cooperativaId, String cooperativaNome,
                               String cargo, String usuarioNome) {
        prefs.edit()
                .putString(KEY_FUNCIONARIO_ID, funcionarioId)
                .putString(KEY_COOPERATIVA_ID, cooperativaId)
                .putString(KEY_COOPERATIVA_NOME, cooperativaNome)
                .putString(KEY_CARGO, cargo)
                .putString(KEY_USUARIO_NOME, usuarioNome)
                .apply();
    }

    public static String getFuncionarioId() {
        return prefs.getString(KEY_FUNCIONARIO_ID, null);
    }

    public static String getCooperativaId() {
        return prefs.getString(KEY_COOPERATIVA_ID, null);
    }

    public static String getCooperativaNome() {
        return prefs.getString(KEY_COOPERATIVA_NOME, null);
    }

    public static String getCargo() {
        return prefs.getString(KEY_CARGO, null);
    }

    public static String getUsuarioNome() {
        return prefs.getString(KEY_USUARIO_NOME, null);
    }

    public static boolean temCooperadoResolvido() {
        return getFuncionarioId() != null;
    }

    public static void limpar() {
        prefs.edit().clear().apply();
    }
}
