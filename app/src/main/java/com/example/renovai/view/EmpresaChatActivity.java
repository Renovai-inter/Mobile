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

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.renovai.EmpresaBottomNav;
import com.example.renovai.EmpresaData;
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
 * "Conversa da empresa" — chat da negociação, visto do lado da empresa. Mesmo padrão do
 * GestorChatActivity.
 */
public class EmpresaChatActivity extends EmpresaBaseActivity {

    public static final String EXTRA_NEG_ID = "negociacaoId",
            EXTRA_COOPERATIVA_ID = "cooperativaId";
    private String negId, meuPerfil;
    private boolean enviando;
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
        if (!preparar(R.layout.activity_empresa_chat, EmpresaBottomNav.Aba.NENHUMA)) return;
        titulo("Conversa da cooperativa");
        negId = getIntent().getStringExtra(EXTRA_NEG_ID);

        if (negId == null) {
            toast("Negociação não informada.");
            finish();
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content),
                (v, ins) -> {
                    Insets bars =
                            ins.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                                            | WindowInsetsCompat.Type.ime());
                    v.setPadding(0, 0, 0, bars.bottom);
                    return ins;
                });

        findViewById(R.id.btnEnviarE).setOnClickListener(v -> enviar());
        EmpresaData.meuPerfilId(
                new com.example.renovai.GestorData.Ouvinte<String>() {
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
        EmpresaData.api()
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
        LinearLayout lista = findViewById(R.id.listaMensagensE);
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
        ScrollView sv = findViewById(R.id.scrollChatE);
        sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
    }

    private void enviar() {
        EditText e = findViewById(R.id.edtMensagemE);
        String txt = e.getText().toString().trim();
        if (txt.isEmpty() || enviando) return;
        if (txt.length() > 4000) {
            toast("A mensagem deve ter até 4000 caracteres.");
            return;
        }
        if (meuPerfil == null) {
            toast("Ainda identificando sua conta — tente novamente em instantes.");
            return;
        }
        enviando = true;
        findViewById(R.id.btnEnviarE).setEnabled(false);
        EmpresaData.api()
                .enviarMensagem(negId, new GestorRequests.Mensagem(negId, meuPerfil, txt, "TEXTO"))
                .enqueue(
                        new Callback<GestorResponses.Mensagem>() {
                            @Override
                            public void onResponse(
                                    Call<GestorResponses.Mensagem> c,
                                    Response<GestorResponses.Mensagem> r) {
                                enviando = false;
                                if (!vivo()) return;
                                findViewById(R.id.btnEnviarE).setEnabled(true);
                                if (r.isSuccessful()) {
                                    if (e.getText().toString().trim().equals(txt)) e.setText("");
                                    carregar();
                                } else toast("Não foi possível enviar (erro " + r.code() + ").");
                            }

                            @Override
                            public void onFailure(Call<GestorResponses.Mensagem> c, Throwable t) {
                                enviando = false;
                                if (!vivo()) return;
                                findViewById(R.id.btnEnviarE).setEnabled(true);
                                toast("Sem conexão com o servidor.");
                            }
                        });
    }
}
