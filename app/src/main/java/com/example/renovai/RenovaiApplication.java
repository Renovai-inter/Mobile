package com.example.renovai;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.activity.ComponentActivity;

public class RenovaiApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SessionManager.init(this);
        // Necessário para a área de Cooperado (ver CooperadoSession.java) — não
        // mexe em SessionManager, só inicializa o SharedPreferences próprio.
        CooperadoSession.init(this);
        // Firebase: Firestore com cache persistente (offline) + login anônimo para as regras.
        FirebaseCache.init(this);
        // Render gratuito: começa a acordar a API enquanto a pessoa ainda está na tela.
        ApiClient.acordarServidor();
        // Barra de status da cor do cabeçalho de cada tela, em todas as versões do Android.
        registerActivityLifecycleCallbacks(
                new ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityCreated(Activity a, Bundle b) {
                        if (a instanceof ComponentActivity) TelaInsets.bordaABorda((ComponentActivity) a);
                    }

                    @Override public void onActivityStarted(Activity a) {}
                    @Override public void onActivityResumed(Activity a) {}
                    @Override public void onActivityPaused(Activity a) {}
                    @Override public void onActivityStopped(Activity a) {}
                    @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
                    @Override public void onActivityDestroyed(Activity a) {}
                });
    }
}
