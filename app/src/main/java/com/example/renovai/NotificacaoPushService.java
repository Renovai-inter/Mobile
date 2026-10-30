package com.example.renovai;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Recebe os pushes da API (Firebase Cloud Messaging). A API manda só dados (titulo, texto, aba)
 * e quem monta a notificação é o app, com o canal e a aba do perfil logado (ver Notificacoes).
 */
public class NotificacaoPushService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage mensagem) {
        Map<String, String> dados = mensagem.getData();
        String titulo = dados.get("titulo"), texto = dados.get("texto");
        if (titulo == null || texto == null) return;
        Notificacoes.mostrar(this, titulo, texto, dados.get("aba"));
    }

    /** O Firebase trocou o token do aparelho: a API precisa do novo para continuar enviando. */
    @Override
    public void onNewToken(@NonNull String token) {
        Notificacoes.registrar(token);
    }
}
