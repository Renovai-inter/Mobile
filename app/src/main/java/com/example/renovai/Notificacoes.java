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

import com.example.renovai.view.AdminHomeActivity;
import com.example.renovai.view.CooperadoHomeActivity;
import com.example.renovai.view.EmpresaHomeActivity;
import com.example.renovai.view.GestorHomeActivity;
import com.example.renovai.view.GestorPedidosActivity;
import com.example.renovai.view.GestorPedidosPendentesActivity;
import com.example.renovai.view.MenuActivity;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Notificações push. Quem decide o que avisar e para quem é a API (NotificacaoService), na hora
 * do evento e nunca para quem fez a ação. O app só registra o aparelho para a conta logada (uma
 * conta por aparelho), mostra o que chega (NotificacaoPushService) e descadastra no logout.
 *
 * <p>Gestor: proposta recebida, proposta aceita/finalizada, coletas e triagens. Cooperado: estoque
 * abaixo de 1.000 kg e rateio em que entrou. Empresa: contraproposta e proposta finalizada.
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

    private static final int PEDIDO_PERMISSAO = 7301;

    private Notificacoes() {}

    /** Perfil logado com notificações, pelo mesmo critério do LoginActivity.destinoParaRole. */
    public static Perfil perfil() {
        if (!SessionManager.estaLogado()) return null;
        return perfilDoRole(
                SessionManager.getRole(),
                CooperadoSession.temCooperadoResolvido(),
                SessionManager.getEmpresaId() != null);
    }

    /**
     * Perfil que recebe avisos para o role da conta, ou null para quem não recebe (admin do site,
     * motorista, conta sem funcionário ou empresa resolvidos). O admin da cooperativa recebe como
     * gestor, igual à área que ele usa no app.
     */
    static Perfil perfilDoRole(String role, boolean temFuncionario, boolean temEmpresa) {
        if (role == null) return null;
        String r = role.toUpperCase(Locale.ROOT);
        if (AdminHomeActivity.ehAdminDoSite(r)) return null;
        if (r.contains("EMPRESA")) return temEmpresa ? Perfil.EMPRESA : null;
        if (!temFuncionario) return null;
        if (r.contains("GESTOR") || r.equals("ADMIN_COOPERATIVA")) return Perfil.GESTOR;
        if (r.contains("COOPERADO")) return Perfil.COOPERADO;
        return null;
    }

    /**
     * Chamado pela Home de cada perfil: cria os canais, pede a permissão (Android 13+) e liga o
     * aparelho à conta logada na API.
     */
    public static void iniciar(Activity a) {
        criarCanais(a);
        if (ContextCompat.checkSelfPermission(a, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    a, new String[] {Manifest.permission.POST_NOTIFICATIONS}, PEDIDO_PERMISSAO);
        }
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(Notificacoes::registrar);
    }

    /** Envia o token do aparelho para a API (ela troca a conta dona do aparelho, se for outra). */
    static void registrar(String token) {
        if (token == null || perfil() == null) return;
        ApiClient.createService(DispositivoApiService.class)
                .registrar(new DispositivoApiService.Dispositivo(token))
                .enqueue(ignorar());
    }

    /**
     * No logout (antes de limpar a sessão): descadastra o aparelho na API, invalida o token no
     * Firebase e tira da bandeja os avisos da conta que saiu.
     */
    public static void parar(Context c) {
        NotificationManagerCompat.from(c).cancelAll();
        if (perfil() == null) return;
        String bearer = "Bearer " + SessionManager.getToken();
        FirebaseMessaging fcm = FirebaseMessaging.getInstance();
        fcm.getToken()
                .addOnSuccessListener(
                        token ->
                                ApiClient.createService(DispositivoApiService.class)
                                        .remover(bearer, token)
                                        .enqueue(
                                                new Callback<Void>() {
                                                    @Override
                                                    public void onResponse(Call<Void> call, Response<Void> r) {
                                                        fcm.deleteToken();
                                                    }

                                                    @Override
                                                    public void onFailure(Call<Void> call, Throwable t) {
                                                        // sem rede: o token invalidado faz a API
                                                        // descartar o aparelho no próximo envio
                                                        fcm.deleteToken();
                                                    }
                                                }));
    }

    private static Callback<Void> ignorar() {
        return new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> r) {}

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // tenta de novo na próxima abertura da Home
            }
        };
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

    /**
     * Mostra um push da API; aba = "pedidos", "pedidos_pendentes" (proposta recebida), "coletas",
     * "estoque" ou "rateios".
     */
    static void mostrar(Context c, String titulo, String texto, String aba) {
        Perfil p = perfil();
        if (p == null) return; // aviso chegou depois do logout
        // o aviso é de uma mudança no servidor: as listas em cache (pedidos, coletas, estoque,
        // rateios) rebuscam na próxima abertura, senão o toque leva a uma tela sem a novidade
        GestorCache.invalidarTudo();
        if (ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;
        criarCanais(c);
        int id = (titulo + texto).hashCode();
        NotificationCompat.Builder b =
                new NotificationCompat.Builder(c, canal(aba))
                        .setSmallIcon(R.drawable.ic_notificacao)
                        .setContentTitle(titulo)
                        .setContentText(texto)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                        .setAutoCancel(true)
                        .setContentIntent(destino(c, p, aba, id));
        try {
            NotificationManagerCompat.from(c).notify(id, b.build());
        } catch (SecurityException ignorado) {
            // permissão revogada entre a checagem e o notify
        }
    }

    private static String canal(String aba) {
        if ("coletas".equals(aba)) return CANAL_OPERACAO;
        if ("estoque".equals(aba)) return CANAL_ESTOQUE;
        if ("rateios".equals(aba)) return CANAL_RATEIOS;
        return CANAL_PEDIDOS;
    }

    /**
     * Abre a Home do perfil na aba do aviso (reaproveita a casca se ela já estiver aberta). As
     * propostas do gestor abrem os pedidos por cima da Home (proposta recebida vai até os
     * pendentes), e o voltar retorna por essas telas.
     */
    private static PendingIntent destino(Context c, Perfil p, String aba, int id) {
        Intent i = new Intent(c, casca(p));
        i.putExtra(MenuActivity.EXTRA_ABA, abaDoAviso(p, aba));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        if (abrePedidosDoGestor(p, aba)) {
            Intent pedidos = new Intent(c, GestorPedidosActivity.class);
            Intent[] pilha =
                    abrePendentesDoGestor(p, aba)
                            ? new Intent[] {i, pedidos, new Intent(c, GestorPedidosPendentesActivity.class)}
                            : new Intent[] {i, pedidos};
            return PendingIntent.getActivities(c, id, pilha, flags);
        }
        return PendingIntent.getActivity(c, id, i, flags);
    }

    /** Proposta recebida, aceita ou finalizada: o gestor cai direto nos pedidos. */
    static boolean abrePedidosDoGestor(Perfil p, String aba) {
        return p == Perfil.GESTOR
                && (aba == null || "pedidos".equals(aba) || "pedidos_pendentes".equals(aba));
    }

    /** Proposta recebida: está nos pendentes, esperando Recusar/Negociar. */
    static boolean abrePendentesDoGestor(Perfil p, String aba) {
        return p == Perfil.GESTOR && "pedidos_pendentes".equals(aba);
    }

    private static Class<? extends MenuActivity> casca(Perfil p) {
        switch (p) {
            case GESTOR:
                return GestorHomeActivity.class;
            case COOPERADO:
                return CooperadoHomeActivity.class;
            default:
                return EmpresaHomeActivity.class;
        }
    }

    /** Item do menu da casca para a aba do aviso. */
    static int abaDoAviso(Perfil p, String aba) {
        switch (p) {
            case GESTOR:
                if ("coletas".equals(aba)) return R.id.nav_g_coletas;
                if ("estoque".equals(aba)) return R.id.nav_g_estoque;
                return R.id.nav_g_home;
            case COOPERADO:
                if ("rateios".equals(aba)) return R.id.nav_rateios;
                return R.id.nav_home_cooperado;
            default:
                return R.id.nav_pedidos;
        }
    }
}
