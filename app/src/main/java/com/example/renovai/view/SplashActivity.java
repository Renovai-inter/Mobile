package com.example.renovai.view;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.renovai.R;

/**
 * Splash animada (3,5 s, ver SplashAnimacao) e depois a tela de login com fade.
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

        animacao = new SplashAnimacao(findViewById(R.id.main), this::abrirLogin);
        animacao.iniciar();
    }

    private void abrirLogin() {
        if (isFinishing() || isDestroyed()) return;
        Intent rota = new Intent(this, LoginActivity.class);
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
