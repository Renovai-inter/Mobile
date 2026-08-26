package com.example.renovai;

import android.app.Application;

public class RenovaiApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SessionManager.init(this);
    }
}
