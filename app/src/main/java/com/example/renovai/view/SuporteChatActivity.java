package com.example.renovai.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renovai.ApiClient;
import com.example.renovai.EmpresaData;
import com.example.renovai.R;
import com.example.renovai.SuporteApiService;
import com.example.renovai.TelaInsets;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Atendimento ao cliente com o NIMO, aberto pelo botão Suporte do perfil de todas as áreas. O que
 * o assistente responde depende do perfil (a API decide pelo login): gestor, empresa e admin
 * recebem análises dos dados; cooperado e motorista, dúvidas gerais e FAQ.
 */
public class SuporteChatActivity extends AppCompatActivity {

    private static final String SAUDACAO =
            "Olá sou o NIMO um assistente virtual para te ajudar com o que precisar, se quiser falar"
                    + " com um de nossos assistentes é só me pedir";
    private static final String DIGITANDO = "NIMO está digitando…";
    private static final String ESTADO_SESSAO = "sessao", ESTADO_TEXTOS = "textos",
            ESTADO_AUTORES = "autores";

    public static void abrir(Context c) {
        c.startActivity(new Intent(c, SuporteChatActivity.class));
    }

    /** Uma mensagem da conversa; {@code doUsuario} = balão verde à direita. */
    private static final class Mensagem {
        final String texto;
        final boolean doUsuario;

        Mensagem(String texto, boolean doUsuario) {
            this.texto = texto;
            this.doUsuario = doUsuario;
        }
    }

    private final List<Mensagem> mensagens = new ArrayList<>();
    private Adaptador adaptador;
    private RecyclerView lista;
    private EditText edtPergunta;
    private ImageView btnEnviar;
    private String sessaoId;
    private boolean aguardando, perguntou;
    private Call<SuporteApiService.Resposta> chamada;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_suporte_chat);
        TelaInsets.conteudo(this, false);

        lista = findViewById(R.id.listaMensagensSuporte);
        edtPergunta = findViewById(R.id.edtPerguntaSuporte);
        btnEnviar = findViewById(R.id.btnEnviarSuporte);

        // como no design, a conversa começa no topo; as novas mensagens rolam a lista até o fim
        lista.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new Adaptador();
        lista.setAdapter(adaptador);

        restaurar(b);
        btnEnviar.setOnClickListener(v -> enviar());
        edtPergunta.setOnEditorActionListener(
                (v, acao, evento) -> {
                    boolean enter = evento != null && evento.getKeyCode() == KeyEvent.KEYCODE_ENTER;
                    if (acao == EditorInfo.IME_ACTION_SEND || enter) {
                        enviar();
                        return true;
                    }
                    return false;
                });
    }

    /** Conversa sobrevive à rotação; a mesma sessão continua na IA. */
    private void restaurar(Bundle b) {
        ArrayList<String> textos = b == null ? null : b.getStringArrayList(ESTADO_TEXTOS);
        boolean[] autores = b == null ? null : b.getBooleanArray(ESTADO_AUTORES);
        sessaoId = b == null ? null : b.getString(ESTADO_SESSAO);
        if (sessaoId == null) sessaoId = UUID.randomUUID().toString();
        if (textos == null || autores == null || textos.size() != autores.length) {
            mensagens.add(new Mensagem(SAUDACAO, false));
        } else {
            for (int i = 0; i < textos.size(); i++) {
                mensagens.add(new Mensagem(textos.get(i), autores[i]));
                if (autores[i]) perguntou = true;
            }
        }
        adaptador.notifyDataSetChanged();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle b) {
        super.onSaveInstanceState(b);
        ArrayList<String> textos = new ArrayList<>();
        List<Boolean> autores = new ArrayList<>();
        for (Mensagem m : mensagens) {
            if (DIGITANDO.equals(m.texto) && !m.doUsuario) continue; // resposta em andamento se perde
            textos.add(m.texto);
            autores.add(m.doUsuario);
        }
        boolean[] arr = new boolean[autores.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = autores.get(i);
        b.putString(ESTADO_SESSAO, sessaoId);
        b.putStringArrayList(ESTADO_TEXTOS, textos);
        b.putBooleanArray(ESTADO_AUTORES, arr);
    }

    private void enviar() {
        String pergunta = edtPergunta.getText().toString().trim();
        if (pergunta.isEmpty() || aguardando) return;
        edtPergunta.setText("");
        adicionar(new Mensagem(pergunta, true));
        adicionar(new Mensagem(DIGITANDO, false));
        aguardar(true);
        perguntou = true;

        chamada =
                ApiClient.createService(SuporteApiService.class)
                        .perguntar(new SuporteApiService.Pergunta(sessaoId, pergunta));
        chamada.enqueue(
                new Callback<SuporteApiService.Resposta>() {
                    @Override
                    public void onResponse(
                            Call<SuporteApiService.Resposta> c,
                            Response<SuporteApiService.Resposta> r) {
                        if (isFinishing() || isDestroyed()) return;
                        String texto =
                                r.isSuccessful() && r.body() != null && r.body().resposta != null
                                        ? r.body().resposta
                                        : r.code() == 401 || r.code() == 403
                                                ? "Sua sessão expirou. Entre de novo no app para"
                                                        + " conversar comigo."
                                                : EmpresaData.erro(r);
                        responder(texto);
                    }

                    @Override
                    public void onFailure(Call<SuporteApiService.Resposta> c, Throwable t) {
                        if (c.isCanceled() || isFinishing() || isDestroyed()) return;
                        responder(
                                "Não consegui me conectar agora. Verifique sua internet e tente"
                                        + " de novo.");
                    }
                });
    }

    /** Troca o "digitando…" pela resposta. */
    private void responder(String texto) {
        int ultimo = mensagens.size() - 1;
        if (ultimo >= 0 && !mensagens.get(ultimo).doUsuario && DIGITANDO.equals(mensagens.get(ultimo).texto)) {
            mensagens.set(ultimo, new Mensagem(texto, false));
            adaptador.notifyItemChanged(ultimo);
        } else {
            adicionar(new Mensagem(texto, false));
        }
        lista.scrollToPosition(mensagens.size() - 1);
        aguardar(false);
    }

    private void adicionar(Mensagem m) {
        mensagens.add(m);
        adaptador.notifyItemInserted(mensagens.size() - 1);
        lista.scrollToPosition(mensagens.size() - 1);
    }

    private void aguardar(boolean sim) {
        aguardando = sim;
        btnEnviar.setEnabled(!sim);
        btnEnviar.setAlpha(sim ? 0.4f : 1f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isFinishing()) return; // rotação: a conversa continua
        if (chamada != null) chamada.cancel();
        if (perguntou) {
            ApiClient.createService(SuporteApiService.class)
                    .encerrar(sessaoId)
                    .enqueue(
                            new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> c, Response<Void> r) {}

                                @Override
                                public void onFailure(Call<Void> c, Throwable t) {
                                    // sem rede: a IA expira a sessão sozinha
                                }
                            });
        }
    }

    // ───────────────────────────── texto ─────────────────────────────

    private static final Pattern NEGRITO = Pattern.compile("\\*\\*(.+?)\\*\\*");

    /** A IA pode mandar **negrito** (markdown): vira negrito de verdade, sem os asteriscos. */
    private CharSequence formatar(String texto) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        Matcher m = NEGRITO.matcher(texto);
        int pos = 0;
        while (m.find()) {
            sb.append(texto, pos, m.start());
            int ini = sb.length();
            sb.append(m.group(1));
            sb.setSpan(new StyleSpan(Typeface.BOLD), ini, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            pos = m.end();
        }
        sb.append(texto, pos, texto.length());
        // "NIMO" na cor vinho, como no design
        String s = sb.toString();
        for (int i = s.indexOf("NIMO"); i >= 0; i = s.indexOf("NIMO", i + 4)) {
            sb.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(this, R.color.vinho)),
                    i, i + 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new StyleSpan(Typeface.BOLD), i, i + 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sb;
    }

    // ───────────────────────────── lista ─────────────────────────────

    private final class Adaptador extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int NIMO = 0, USUARIO = 1;

        @Override
        public int getItemViewType(int pos) {
            return mensagens.get(pos).doUsuario ? USUARIO : NIMO;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int tipo) {
            View v =
                    LayoutInflater.from(parent.getContext())
                            .inflate(
                                    tipo == USUARIO
                                            ? R.layout.item_suporte_usuario
                                            : R.layout.item_suporte_nimo,
                                    parent,
                                    false);
            return new RecyclerView.ViewHolder(v) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
            Mensagem m = mensagens.get(pos);
            TextView tv =
                    h.itemView.findViewById(
                            m.doUsuario ? R.id.txtMensagemUsuario : R.id.txtMensagemNimo);
            tv.setText(m.doUsuario ? m.texto : formatar(m.texto));
            tv.setAlpha(!m.doUsuario && DIGITANDO.equals(m.texto) ? 0.6f : 1f);
        }

        @Override
        public int getItemCount() {
            return mensagens.size();
        }
    }
}
