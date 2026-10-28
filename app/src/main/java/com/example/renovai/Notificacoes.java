package com.example.renovai;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.renovai.view.AdminHomeActivity;
import com.example.renovai.view.CooperadoHomeActivity;
import com.example.renovai.view.EmpresaHomeActivity;
import com.example.renovai.view.GestorHomeActivity;
import com.example.renovai.view.MenuActivity;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Notificações do app. A API não envia push, então o app verifica sozinho: um WorkManager roda o
 * NotificacaoVerificador a cada 15 min (o mínimo do Android) e também ao abrir/voltar para a Home
 * do perfil. Cada verificação compara a API com o retrato salvo da última vez e notifica o que
 * mudou.
 *
 * <p>Gestor: proposta nova, proposta aceita/finalizada e mudança de estado de coletas e triagens.
 * Cooperado: material do estoque abaixo de 1.000 kg e rateio realizado em que ele entrou. Empresa:
 * contraproposta e proposta finalizada.
 */
public final class Notificacoes {

    public enum Perfil {
        GESTOR,
        COOPERADO,
        EMPRESA
    }

    static final String CANAL_PEDIDOS = "pedidos",
            CANAL_OPERACAO = "operacao",
            CANAL_ESTOQUE = "estoque",
            CANAL_RATEIOS = "rateios";

    private static final String TRABALHO_PERIODICO = "notificacoes_periodicas",
            TRABALHO_AGORA = "notificacoes_agora";
    private static final int PEDIDO_PERMISSAO = 7301;

    /** O que mostrar e para onde o toque leva. */
    public static final class Aviso {
        final String canal, chave, titulo, texto;
        final Class<? extends MenuActivity> casca;
        final int aba;

        /**
         * @param chave identifica o evento (a mesma chave substitui a notificação anterior)
         * @param casca Home do perfil; @param aba aba dela que o toque abre
         */
        public Aviso(
                String canal,
                String chave,
                String titulo,
                String texto,
                Class<? extends MenuActivity> casca,
                int aba) {
            this.canal = canal;
            this.chave = chave;
            this.titulo = titulo;
            this.texto = texto;
            this.casca = casca;
            this.aba = aba;
        }
    }

    private Notificacoes() {}

    /** Perfil logado com notificações, pelo mesmo critério do LoginActivity.destinoParaRole. */
    public static Perfil perfil() {
        if (!SessionManager.estaLogado()) return null;
        String r = SessionManager.getRole();
        if (r == null) return null;
        r = r.toUpperCase(Locale.ROOT);
        if (AdminHomeActivity.ehAdminDoSite(r)) return null;
        if (r.contains("EMPRESA")) return SessionManager.getEmpresaId() != null ? Perfil.EMPRESA : null;
        if (!CooperadoSession.temCooperadoResolvido()) return null;
        if (r.contains("GESTOR") || r.equals("ADMIN_COOPERATIVA")) return Perfil.GESTOR;
        if (r.contains("COOPERADO")) return Perfil.COOPERADO;
        return null;
    }

    /**
     * Chamado pela Home de cada perfil: cria os canais, pede a permissão (Android 13+) e liga a
     * verificação periódica.
     */
    public static void iniciar(Activity a) {
        criarCanais(a);
        if (ContextCompat.checkSelfPermission(a, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    a, new String[] {Manifest.permission.POST_NOTIFICATIONS}, PEDIDO_PERMISSAO);
        }
        PeriodicWorkRequest periodico =
                new PeriodicWorkRequest.Builder(NotificacaoWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(comInternet())
                        .build();
        WorkManager.getInstance(a)
                .enqueueUniquePeriodicWork(
                        TRABALHO_PERIODICO, ExistingPeriodicWorkPolicy.KEEP, periodico);
    }

    /** Verifica já (ao voltar para a Home), sem esperar a próxima rodada periódica. */
    public static void verificarAgora(Context c) {
        OneTimeWorkRequest agora =
                new OneTimeWorkRequest.Builder(NotificacaoWorker.class)
                        .setConstraints(comInternet())
                        .build();
        WorkManager.getInstance(c)
                .enqueueUniqueWork(TRABALHO_AGORA, ExistingWorkPolicy.KEEP, agora);
    }

    /** No logout: para as verificações e tira da bandeja os avisos da conta que saiu. */
    public static void parar(Context c) {
        WorkManager wm = WorkManager.getInstance(c);
        wm.cancelUniqueWork(TRABALHO_PERIODICO);
        wm.cancelUniqueWork(TRABALHO_AGORA);
        NotificationManagerCompat.from(c).cancelAll();
    }

    private static Constraints comInternet() {
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    }

    static void criarCanais(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        nm.createNotificationChannel(
                canal(CANAL_PEDIDOS, "Propostas e pedidos",
                        "Propostas recebidas, contrapropostas e propostas finalizadas",
                        NotificationManager.IMPORTANCE_HIGH));
        nm.createNotificationChannel(
                canal(CANAL_OPERACAO, "Coletas e triagens",
                        "Mudanças de estado das coletas e triagens da cooperativa",
                        NotificationManager.IMPORTANCE_DEFAULT));
        nm.createNotificationChannel(
                canal(CANAL_ESTOQUE, "Estoque baixo",
                        "Materiais do estoque abaixo de 1.000 kg",
                        NotificationManager.IMPORTANCE_DEFAULT));
        nm.createNotificationChannel(
                canal(CANAL_RATEIOS, "Rateios",
                        "Rateios realizados em que você participou",
                        NotificationManager.IMPORTANCE_DEFAULT));
    }

    private static NotificationChannel canal(String id, String nome, String descricao, int importancia) {
        NotificationChannel ch = new NotificationChannel(id, nome, importancia);
        ch.setDescription(descricao);
        return ch;
    }

    static void mostrar(Context c, Aviso aviso) {
        if (ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;
        int id = aviso.chave.hashCode();
        NotificationCompat.Builder b =
                new NotificationCompat.Builder(c, aviso.canal)
                        .setSmallIcon(R.drawable.ic_notificacao)
                        .setContentTitle(aviso.titulo)
                        .setContentText(aviso.texto)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(aviso.texto))
                        .setAutoCancel(true)
                        .setContentIntent(destino(c, aviso, id));
        try {
            NotificationManagerCompat.from(c).notify(id, b.build());
        } catch (SecurityException ignorado) {
            // permissão revogada entre a checagem e o notify
        }
    }

    /** Abre a Home do perfil na aba do aviso (reaproveita a casca se ela já estiver aberta). */
    private static PendingIntent destino(Context c, Aviso aviso, int id) {
        Intent i = new Intent(c, aviso.casca);
        i.putExtra(MenuActivity.EXTRA_ABA, aviso.aba);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                c, id, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static Class<? extends MenuActivity> casca(Perfil p) {
        switch (p) {
            case GESTOR:
                return GestorHomeActivity.class;
            case COOPERADO:
                return CooperadoHomeActivity.class;
            default:
                return EmpresaHomeActivity.class;
        }
    }
}
