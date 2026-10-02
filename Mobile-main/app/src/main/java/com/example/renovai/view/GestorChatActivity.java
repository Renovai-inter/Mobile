package com.example.renovai.view;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.renovai.EmpresaData;
import com.example.renovai.EmpresaStatus;
import com.example.renovai.GestorBottomNav;
import com.example.renovai.GestorData;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.dto.request.GestorRequests;
import com.example.renovai.dto.response.GestorResponses;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Conversa da empresa: chat da negociação (atualiza sozinho a cada 8 s) e "Nova contraproposta".
 */
public class GestorChatActivity extends GestorBaseActivity {

    public static final String EXTRA_NEG_ID = "negociacaoId";

    private String negId, meuPerfil;
    private boolean podeContrapropor, enviandoProposta;
    private List<GestorResponses.Mensagem> mensagens = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable atualizador =
            new Runnable() {
                @Override
                public void run() {
                    carregar();
                    handler.postDelayed(this, 8000);
                }
            };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_gestor_chat, GestorBottomNav.Aba.NENHUMA)) return;
        titulo("Conversa da empresa");
        negId = getIntent().getStringExtra(EXTRA_NEG_ID);
        if (negId == null || negId.trim().isEmpty()) {
            finish();
            return;
        }
        findViewById(R.id.btnContraproposta).setVisibility(View.GONE);

        findViewById(R.id.btnEnviar).setOnClickListener(v -> enviar());
        findViewById(R.id.btnContraproposta)
                .setOnClickListener(
                        v ->
                                GestorUi.pedirTexto(
                                        this,
                                        "Nova contraproposta (valor total, R$)",
                                        "0,00",
                                        "",
                                        GestorUi.TIPO_DECIMAL,
                                        t -> {
                                            Double valor = GestorUi.parseValor(t);
                                            if (valor == null
                                                    || !Double.isFinite(valor)
                                                    || valor <= 0) {
                                                toast("Informe um valor válido.");
                                                return;
                                            }
                                            GestorUi.pedirTexto(
                                                    this,
                                                    "Observação da contraproposta (opcional)",
                                                    "",
                                                    "",
                                                    GestorUi.TIPO_TEXTO,
                                                    observacao ->
                                                            contraproposta(valor, observacao));
                                        }));

        GestorData.perfilDaCooperativa(
                new GestorData.Ouvinte<String>() {
                    @Override
                    public void aoReceber(String id, boolean c) {
                        meuPerfil = id;
                        if (vivo()) render();
                    }

                    @Override
                    public void aoErro(String m) {
                        if (vivo()) toast(m);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (vivo() && negId != null) atualizador.run();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(atualizador);
    }

    private void carregar() {
        GestorData.api()
                .negociacao(negId)
                .enqueue(
                        new Callback<GestorResponses.Negociacao>() {
                            @Override
                            public void onResponse(
                                    Call<GestorResponses.Negociacao> c,
                                    Response<GestorResponses.Negociacao> r) {
                                if (!vivo()) return;
                                int estado =
                                        r.isSuccessful() && r.body() != null
                                                ? EmpresaStatus.estado(r.body(), null, null)
                                                : EmpresaStatus.RECUSADO;
                                podeContrapropor =
                                        estado == EmpresaStatus.EM_NEGOCIACAO
                                                || estado == EmpresaStatus.ABERTO;
                                atualizarProposta();
                            }

                            @Override
                            public void onFailure(Call<GestorResponses.Negociacao> c, Throwable t) {
                                if (!vivo()) return;
                                podeContrapropor = false;
                                atualizarProposta();
                            }
                        });
        GestorData.api()
                .mensagens(negId)
                .enqueue(
                        new Callback<List<GestorResponses.Mensagem>>() {
                            @Override
                            public void onResponse(
                                    Call<List<GestorResponses.Mensagem>> c,
                                    Response<List<GestorResponses.Mensagem>> r) {
                                if (r.isSuccessful()
                                        && r.body() != null
                                        && vivo()
                                        && r.body().size() != mensagens.size()) {
                                    mensagens = r.body();
                                    render();
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<List<GestorResponses.Mensagem>> c, Throwable t) {}
                        });
    }

    private void render() {
        LinearLayout lista = findViewById(R.id.listaMensagens);
        lista.removeAllViews();
        int larg = (int) (getResources().getDisplayMetrics().widthPixels * 0.72f);
        for (GestorResponses.Mensagem m : mensagens) {
            boolean minha = meuPerfil != null && meuPerfil.equalsIgnoreCase(m.remetenteId);
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.gravity = minha ? Gravity.END : Gravity.START;
            lp.bottomMargin = GestorUi.dp(this, 14);
            col.setLayoutParams(lp);

            TextView bolha = new TextView(this);
            bolha.setText(m.mensagem);
            bolha.setTextColor(0xFFFFFFFF);
            bolha.setTextSize(12f);
            bolha.setMaxWidth(larg);
            int p = GestorUi.dp(this, 14);
            bolha.setPadding(p, p - 2, p, p - 2);
            bolha.setBackgroundResource(
                    minha ? R.drawable.bubble_out_background : R.drawable.bubble_in_background);
            col.addView(bolha);

            TextView hora = new TextView(this);
            hora.setText(GestorUi.hora(m.dataEnvio));
            hora.setTextSize(10f);
            hora.setTextColor(0xFF6B7280);
            hora.setGravity(minha ? Gravity.END : Gravity.START);
            col.addView(hora);
            lista.addView(col);
        }
        ScrollView sv = findViewById(R.id.scrollChat);
        sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
    }

    private void enviar() {
        EditText e = findViewById(R.id.edtMensagem);
        String txt = e.getText().toString().trim();
        if (txt.isEmpty()) return;
        postar(txt, "TEXTO", () -> e.setText(""));
    }

    private void postar(String texto, String tipo, Runnable aoOk) {
        if (meuPerfil == null) {
            toast("Ainda identificando a conta da cooperativa — tente novamente em instantes.");
            return;
        }
        GestorData.api()
                .enviarMensagem(negId, new GestorRequests.Mensagem(negId, meuPerfil, texto, tipo))
                .enqueue(
                        new Callback<GestorResponses.Mensagem>() {
                            @Override
                            public void onResponse(
                                    Call<GestorResponses.Mensagem> c,
                                    Response<GestorResponses.Mensagem> r) {
                                if (r.isSuccessful()) {
                                    if (aoOk != null) aoOk.run();
                                    carregar();
                                } else toast("Não foi possível enviar (erro " + r.code() + ").");
                            }

                            @Override
                            public void onFailure(Call<GestorResponses.Mensagem> c, Throwable t) {
                                toast("Sem conexão com o servidor.");
                            }
                        });
    }

    private void contraproposta(Double valor, String observacao) {
        if (!podeContrapropor || enviandoProposta) {
            toast("Aguarde a consulta da negociação em andamento.");
            return;
        }
        if (valor == null || valor <= 0) {
            toast("Informe um valor válido.");
            return;
        }
        enviandoProposta = true;
        atualizarProposta();
        GestorData.api()
                .contraproposta(negId, new GestorRequests.Contraproposta(negId, valor, observacao))
                .enqueue(
                        new Callback<GestorResponses.Negociacao>() {
                            @Override
                            public void onResponse(
                                    Call<GestorResponses.Negociacao> c,
                                    Response<GestorResponses.Negociacao> r) {
                                enviandoProposta = false;
                                if (!vivo()) return;
                                atualizarProposta();
                                if (!r.isSuccessful()) {
                                    toast(EmpresaData.erro(r));
                                    carregar();
                                    return;
                                }
                                GestorData.invalidar(com.example.renovai.GestorCache.NEGOCIACOES);
                                toast("Contraproposta enviada.");
                                carregar();
                            }

                            @Override
                            public void onFailure(Call<GestorResponses.Negociacao> c, Throwable t) {
                                enviandoProposta = false;
                                if (!vivo()) return;
                                atualizarProposta();
                                toast("Sem conexão com o servidor.");
                                carregar();
                            }
                        });
    }

    private void atualizarProposta() {
        View botao = findViewById(R.id.btnContraproposta);
        botao.setVisibility(podeContrapropor ? View.VISIBLE : View.GONE);
        botao.setEnabled(!enviandoProposta);
    }
}
