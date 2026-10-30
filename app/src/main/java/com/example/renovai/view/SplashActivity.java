package com.example.renovai.view;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.renovai.CooperadoSession;
import com.example.renovai.EmpresaData;
import com.example.renovai.GestorData;
import com.example.renovai.R;
import com.example.renovai.SessaoSalva;
import com.example.renovai.SessionManager;

/**
 * Splash animada (3,5 s, ver SplashAnimacao) e depois a tela de login com fade — ou, se a sessão
 * salva ainda vale (token não vencido e conta já resolvida), direto a Home do perfil, que usa o
 * cache offline quando não há internet. O logout limpa a sessão e volta a abrir o login.
 * O fundo verde ocupa a tela toda, por trás das barras do sistema.
 */
public class SplashActivity extends AppCompatActivity {

    private SplashAnimacao animacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
        setContentView(R.layout.activity_splash_screen);

        animacao = new SplashAnimacao(findViewById(R.id.main), this::entrar);
        animacao.iniciar();
    }

    private void entrar() {
        if (isFinishing() || isDestroyed()) return;
        Class<?> home = homeDaSessaoSalva();
        if (home == null) {
            abrir(LoginActivity.class);
            return;
        }
        // mesmo pré-carregamento do login (com o cache, funciona sem internet; no máx. 15 s)
        if (home == GestorHomeActivity.class) GestorData.precarregar(() -> runOnUiThread(() -> abrir(home)));
        else if (home == EmpresaHomeActivity.class) EmpresaData.precarregar(() -> runOnUiThread(() -> abrir(home)));
        else abrir(home);
    }

    /** Home do perfil se a sessão salva vale; senão null (vai para o login). */
    private static Class<?> homeDaSessaoSalva() {
        String token = SessionManager.getToken();
        if (!SessaoSalva.tokenValido(token, System.currentTimeMillis() / 1000)) {
            if (token != null) SessionManager.logout(); // vencido: não fica guardado
            return null;
        }
        Class<?> home = LoginActivity.destinoParaRole(SessionManager.getRole());
        if (home == null) return null;
        if (home == EmpresaHomeActivity.class && SessionManager.getEmpresaId() == null) return null;
        boolean funcionario = home == GestorHomeActivity.class
                || home == MotoristaHomeActivity.class
                || home == CooperadoHomeActivity.class;
        if (funcionario && !CooperadoSession.temCooperadoResolvido()) return null;
        return home;
    }

    private void abrir(Class<?> destino) {
        if (isFinishing() || isDestroyed()) return;
        Intent rota = new Intent(this, destino);
        ActivityOptions fade =
                ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out);
        startActivity(rota, fade.toBundle());
        finish();
    }

    @Override
    protected void onDestroy() {
        if (animacao != null) animacao.cancelar();
        super.onDestroy();
    }
}
