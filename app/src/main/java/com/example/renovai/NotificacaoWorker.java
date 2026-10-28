package com.example.renovai;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/** Roda uma verificação de notificações em segundo plano (ver Notificacoes). */
public class NotificacaoWorker extends Worker {

    public NotificacaoWorker(@NonNull Context c, @NonNull WorkerParameters p) {
        super(c, p);
    }

    @NonNull
    @Override
    public Result doWork() {
        NotificacaoVerificador.verificar(getApplicationContext());
        return Result.success();
    }
}
