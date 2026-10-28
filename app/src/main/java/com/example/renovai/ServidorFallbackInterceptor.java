package com.example.renovai;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Manda as chamadas para o servidor principal (AWS) e, se ele não responder, repete no reserva
 * (Render). Depois de cair no reserva, fica nele por alguns minutos antes de tentar o principal de
 * novo, para não pagar o tempo de espera em toda chamada.
 *
 * <p>Só repete quando o pedido com certeza não foi processado (sem conexão, servidor fora ou
 * gateway sem a aplicação de pé), para um POST não ser gravado duas vezes. GET repete em qualquer
 * falha de rede.
 */
public class ServidorFallbackInterceptor implements Interceptor {
    private static final String TAG = "ServidorFallback";
    private static final long TEMPO_NO_RESERVA_MS = TimeUnit.MINUTES.toMillis(5);
    private static final int CONEXAO_PRINCIPAL_S = 10;

    private final HttpUrl principal;
    private final HttpUrl reserva;
    private volatile long reservaAte;

    public ServidorFallbackInterceptor(String principal, String reserva) {
        this.principal = HttpUrl.get(principal);
        this.reserva = HttpUrl.get(reserva);
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request pedido = chain.request();
        if (!doPrincipal(pedido.url())) return chain.proceed(pedido);

        if (System.currentTimeMillis() < reservaAte) {
            return chain.proceed(noReserva(pedido));
        }

        boolean leitura = "GET".equals(pedido.method()) || "HEAD".equals(pedido.method());
        Response resposta;
        try {
            resposta = chain.withConnectTimeout(CONEXAO_PRINCIPAL_S, TimeUnit.SECONDS).proceed(pedido);
        } catch (IOException e) {
            if (!leitura && !naoChegou(e)) throw e;
            avisar("Servidor principal fora, usando o reserva: " + e);
            reservaAte = System.currentTimeMillis() + TEMPO_NO_RESERVA_MS;
            return chain.proceed(noReserva(pedido));
        }

        int codigo = resposta.code();
        if (codigo == 502 || codigo == 503 || (leitura && codigo == 504)) {
            avisar("Servidor principal respondeu " + codigo + ", usando o reserva");
            resposta.close();
            reservaAte = System.currentTimeMillis() + TEMPO_NO_RESERVA_MS;
            return chain.proceed(noReserva(pedido));
        }
        return resposta;
    }

    private boolean doPrincipal(HttpUrl url) {
        return url.host().equals(principal.host()) && url.port() == principal.port();
    }

    /** Troca o servidor e mantém o caminho a partir do /api/. */
    private Request noReserva(Request pedido) {
        String caminho = pedido.url().encodedPath().substring(principal.encodedPath().length());
        HttpUrl.Builder url = reserva.newBuilder().addEncodedPathSegments(caminho);
        String query = pedido.url().encodedQuery();
        if (query != null) url.encodedQuery(query);
        return pedido.newBuilder().url(url.build()).build();
    }

    private static void avisar(String msg) {
        try {
            Log.w(TAG, msg);
        } catch (RuntimeException semAndroid) {
            // testes de unidade rodam sem o Log do Android
        }
    }

    /** Falhas em que o pedido não chegou ao servidor (repetir não duplica nada). */
    private static boolean naoChegou(IOException e) {
        if (e instanceof ConnectException || e instanceof UnknownHostException
                || e instanceof NoRouteToHostException || e instanceof SSLException) {
            return true;
        }
        String msg = e.getMessage();
        return e instanceof InterruptedIOException && msg != null && msg.toLowerCase().contains("connect");
    }
}
