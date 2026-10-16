package com.example.renovai;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.renovai.dto.response.GestorResponses;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Utilitários de formatação, status e diálogos compartilhados pelas telas do Gestor. */
public final class GestorUi {

    public static final Locale BR = new Locale("pt", "BR");
    public static final int EM_NEGOCIACAO = 0, CONCLUIDO = 1, RECUSADO = 2, OUTRO = 3;

    private GestorUi() {}

    // ── texto ──
    public static String norm(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }

    public static boolean tem(String s, String... chaves) {
        String n = norm(s);
        for (String c : chaves) if (n.contains(c)) return true;
        return false;
    }

    public static String iniciais(String nome) {
        return CooperadoUi.gerarIniciais(nome);
    }

    public static String idCurto(String id) {
        return id != null && id.length() >= 6
                ? "#" + id.substring(0, 6).toUpperCase(Locale.ROOT)
                : "#------";
    }

    public static String dinheiro(Double v) {
        return NumberFormat.getCurrencyInstance(BR)
                .format(v == null ? 0d : v)
                .replace('\u00a0', ' ');
    }

    public static String kg(Double v) {
        double d = v == null ? 0d : v;
        if (d == Math.floor(d)) return String.format(BR, "%,d kg", (long) d);
        return String.format(BR, "%,.1f kg", d);
    }

    public static String numero(double d) {
        if (d == Math.floor(d)) return String.format(BR, "%,d", (long) d);
        return String.format(BR, "%,.1f", d);
    }

    public static String percentual(double p) {
        return String.format(BR, "%.1f%%", p);
    }

    public static Double parseValor(String texto) {
        if (texto == null) return null;
        String t = texto.replace("R$", "").replace("%", "").replace(" ", "").trim();
        if (t.isEmpty()) return null;
        if (t.contains(",")) t = t.replace(".", "").replace(',', '.');
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ── datas ──
    public static LocalDateTime parse(String iso) {
        if (iso == null || iso.length() < 10) return null;
        try {
            if (iso.length() >= 19) return LocalDateTime.parse(iso.substring(0, 19));
            return java.time.LocalDate.parse(iso.substring(0, 10)).atStartOfDay();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static String data(String iso) {
        LocalDateTime d = parse(iso);
        return d == null ? "--/--/----" : d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public static String hora(String iso) {
        LocalDateTime d = parse(iso);
        return d == null ? "--:--" : d.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public static boolean noMes(String iso, YearMonth ym) {
        LocalDateTime d = parse(iso);
        return d != null && d.getYear() == ym.getYear() && d.getMonthValue() == ym.getMonthValue();
    }

    public static String nomeMes(YearMonth ym) {
        return ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL, BR)
                + "/"
                + ym.getYear();
    }

    // ── negociações ──
    public static int estadoNeg(String status) {
        if (tem(status, "RECUS", "CANCEL")) return RECUSADO;
        if (tem(status, "CONCLU", "FECHAD", "ACEIT", "FINALIZ")) return CONCLUIDO;
        if (tem(status, "NEGOCIA", "ANDAMENTO")) return EM_NEGOCIACAO;
        return OUTRO;
    }

    public static int estadoNeg(GestorResponses.Negociacao negociacao) {
        int estado = EmpresaStatus.estado(negociacao, null, null);
        if (estado == EmpresaStatus.FINALIZADO) return CONCLUIDO;
        if (estado == EmpresaStatus.RECUSADO) return RECUSADO;
        return EM_NEGOCIACAO;
    }

    public static String rotuloNeg(int estado) {
        switch (estado) {
            case CONCLUIDO:
                return "Concluído";
            case RECUSADO:
                return "Recusada";
            case EM_NEGOCIACAO:
                return "Em Negociação";
            default:
                return "Aberto";
        }
    }

    public static double pesoNegociacao(GestorResponses.Negociacao n) {
        double s = 0;
        if (n != null && n.itens != null)
            for (GestorResponses.NegItem i : n.itens)
                s += i.quantidadeKg == null ? 0 : i.quantidadeKg;
        return s;
    }

    public static double somaConcluidas(List<GestorResponses.Negociacao> lista, YearMonth mes) {
        double s = 0;
        if (lista == null) return 0;
        for (GestorResponses.Negociacao n : lista) {
            if (estadoNeg(n) == CONCLUIDO
                    && n.valorTotal != null
                    && noMes(n.dataFechamento != null ? n.dataFechamento : n.dataInicio, mes))
                s += n.valorTotal;
        }
        return s;
    }

    /**
     * Pinta o "pill" de status: 0 rosa (em negociação/aberto), 1 verde (concluído), 2 vermelho
     * (recusado).
     */
    public static void badge(TextView tv, int estado) {
        int fundo, texto, icone;
        switch (estado) {
            case CONCLUIDO:
                fundo = Color.parseColor("#C8F7C5");
                texto = Color.parseColor("#2E7D32");
                icone = R.drawable.ic_check_circle_cooperado;
                break;
            case RECUSADO:
                fundo = Color.parseColor("#FBE4EA");
                texto = Color.parseColor("#882B4E");
                icone = R.drawable.ic_cancel_cooperado;
                break;
            default:
                fundo = Color.parseColor("#FBE4EA");
                texto = Color.parseColor("#C2185B");
                icone = R.drawable.ic_refresh_cooperado;
                break;
        }
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.RECTANGLE);
        g.setCornerRadius(dp(tv.getContext(), 40));
        g.setColor(fundo);
        tv.setBackground(g);
        tv.setTextColor(texto);
        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(icone, 0, 0, 0);
        tv.setCompoundDrawablePadding(dp(tv.getContext(), 4));
    }

    public static void borda(View card, int cor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.WHITE);
        g.setCornerRadius(dp(card.getContext(), 12));
        g.setStroke(dp(card.getContext(), 1), cor);
        card.setBackground(g);
    }

    // ── medidas / insets ──
    public static int dp(Context c, int v) {
        return Math.round(
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    /**
     * Empurra o cabeçalho para baixo da barra de status e a navegação para cima da barra de gestos
     * (edge-to-edge).
     */
    public static void insets(View topo, View base) {
        if (topo != null) {
            final int pt = topo.getPaddingTop();
            ViewCompat.setOnApplyWindowInsetsListener(
                    topo,
                    (v, ins) -> {
                        Insets b = ins.getInsets(WindowInsetsCompat.Type.systemBars());
                        v.setPadding(
                                v.getPaddingLeft(),
                                pt + b.top,
                                v.getPaddingRight(),
                                v.getPaddingBottom());
                        return ins;
                    });
            ViewCompat.requestApplyInsets(topo);
        }
        if (base != null) {
            final int pb = base.getPaddingBottom();
            final int altura = base.getLayoutParams() != null ? base.getLayoutParams().height : 0;
            ViewCompat.setOnApplyWindowInsetsListener(
                    base,
                    (v, ins) -> {
                        Insets b = ins.getInsets(WindowInsetsCompat.Type.systemBars());
                        v.setPadding(
                                v.getPaddingLeft(),
                                v.getPaddingTop(),
                                v.getPaddingRight(),
                                pb + b.bottom);
                        ViewGroup.LayoutParams lp = v.getLayoutParams();
                        if (lp != null && altura > 0) {
                            lp.height = altura + b.bottom;
                            v.setLayoutParams(lp);
                        }
                        return ins;
                    });
            ViewCompat.requestApplyInsets(base);
        }
    }

    // ── controles ──
    public interface AoSelecionar {
        void selecionado(int indice);
    }

    /**
     * Preenche um LinearLayout horizontal (fundo cinza) com os segmentos; o selecionado fica
     * colorido.
     */
    public static void segmentado(
            LinearLayout trilha, String[] rotulos, int inicial, boolean maroon, AoSelecionar cb) {
        trilha.removeAllViews();
        final int[] atual = {inicial};
        final TextView[] tvs = new TextView[rotulos.length];
        for (int i = 0; i < rotulos.length; i++) {
            TextView t = new TextView(trilha.getContext());
            t.setText(rotulos[i]);
            t.setGravity(Gravity.CENTER);
            t.setTextSize(13f);
            t.setPadding(0, dp(trilha.getContext(), 10), 0, dp(trilha.getContext(), 10));
            t.setLayoutParams(
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            final int idx = i;
            t.setOnClickListener(
                    v -> {
                        atual[0] = idx;
                        pintar(tvs, idx, maroon);
                        if (cb != null) cb.selecionado(idx);
                    });
            tvs[i] = t;
            trilha.addView(t);
        }
        pintar(tvs, inicial, maroon);
    }

    private static void pintar(TextView[] tvs, int sel, boolean maroon) {
        for (int i = 0; i < tvs.length; i++) {
            boolean s = i == sel;
            tvs[i].setBackgroundResource(
                    s
                            ? (maroon
                                    ? R.drawable.filled_button_cooperado_background
                                    : R.drawable.toggle_selected_cooperado_background)
                            : 0);
            tvs[i].setTextColor(s ? Color.WHITE : Color.parseColor("#222222"));
            tvs[i].setTypeface(
                    null, s ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    public static void pedirTexto(
            Activity a,
            String titulo,
            String dica,
            String inicial,
            int inputType,
            Consumer<String> aoConfirmar) {
        EditText e = new EditText(a);
        e.setHint(dica);
        e.setText(inicial == null ? "" : inicial);
        e.setInputType(inputType);
        e.setSelection(e.getText().length());
        LinearLayout box = new LinearLayout(a);
        int m = dp(a, 20);
        box.setPadding(m, dp(a, 8), m, 0);
        box.addView(
                e,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(a)
                .setTitle(titulo)
                .setView(box)
                .setPositiveButton(
                        "Salvar", (d, w) -> aoConfirmar.accept(e.getText().toString().trim()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    public static void confirmar(
            Activity a, String titulo, String mensagem, String rotuloOk, Runnable aoConfirmar) {
        new AlertDialog.Builder(a)
                .setTitle(titulo)
                .setMessage(mensagem)
                .setPositiveButton(rotuloOk, (d, w) -> aoConfirmar.run())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    public static int TIPO_TEXTO = InputType.TYPE_CLASS_TEXT;
    public static int TIPO_DECIMAL =
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
    public static int TIPO_EMAIL =
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
    public static int TIPO_TELEFONE = InputType.TYPE_CLASS_PHONE;
}
