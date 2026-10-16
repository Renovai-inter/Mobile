package com.example.renovai;

import android.app.Application;

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
        // Cloudinary: upload das fotos das coletas.
        CloudinaryUploader.init(this);
    }
}
