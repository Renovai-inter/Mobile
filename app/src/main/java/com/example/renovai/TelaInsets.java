package com.example.renovai;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.renovai.view.MenuActivity;

/**
 * Aplica os espaços do sistema a partir do layout original, sem acumular padding.
 *
 * <p>A barra de status é transparente em todas as telas (ver {@link #bordaABorda}): o cabeçalho
 * marcado com {@code android:tag="topo"} se estende por trás dela, então a barra fica da cor do
 * cabeçalho de cada tela.
 */
public final class TelaInsets {
    /** Tag (android:tag) do cabeçalho que fica por trás da barra de status. */
    public static final String TAG_TOPO = "topo";

    private TelaInsets() {}

    /** Barra de status transparente com ícones brancos (os cabeçalhos são verde ou vinho). */
    public static void bordaABorda(ComponentActivity activity) {
        EdgeToEdge.enable(activity, SystemBarStyle.dark(Color.TRANSPARENT));
    }

    public static void conteudo(Activity activity, boolean barraInferior) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) return;
        View raiz = content.getChildAt(0);
        // Nas cascas dos menus o cabeçalho é de cada aba (TelaFragment cuida dele).
        boolean cabecalhoProprio = activity instanceof MenuActivity;
        View cabecalho = cabecalhoProprio ? null : raiz.findViewWithTag(TAG_TOPO);
        if (cabecalho != null) topo(cabecalho);
        final boolean descontarTopo = cabecalhoProprio || cabecalho != null;
        final int esquerda = raiz.getPaddingLeft(), topo = raiz.getPaddingTop();
        final int direita = raiz.getPaddingRight(), fundo = raiz.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(
                raiz,
                (v, ins) -> {
                    Insets barras =
                            ins.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                                            | WindowInsetsCompat.Type.displayCutout());
                    int teclado = ins.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                    v.setPadding(
                            esquerda + barras.left,
                            topo + (descontarTopo ? 0 : barras.top),
                            direita + barras.right,
                            fundo
                                    + (barraInferior
                                            ? Math.max(0, teclado - barras.bottom)
                                            : Math.max(barras.bottom, teclado)));
                    return ins;
                });
        ViewCompat.requestApplyInsets(raiz);
    }

    /**
     * Estende o cabeçalho por trás da barra de status: soma a altura da barra ao padding de topo
     * que ele já tinha no layout.
     */
    public static void topo(View cabecalho) {
        Object guardado = cabecalho.getTag(R.id.tag_padding_topo);
        final int base =
                guardado instanceof Integer ? (Integer) guardado : cabecalho.getPaddingTop();
        cabecalho.setTag(R.id.tag_padding_topo, base);
        ViewCompat.setOnApplyWindowInsetsListener(
                cabecalho,
                (v, ins) -> {
                    Insets barras =
                            ins.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                                            | WindowInsetsCompat.Type.displayCutout());
                    v.setPadding(
                            v.getPaddingLeft(), base + barras.top, v.getPaddingRight(), v.getPaddingBottom());
                    return ins;
                });
        if (cabecalho.isAttachedToWindow()) {
            ViewCompat.requestApplyInsets(cabecalho);
        } else {
            // aba recém-criada: os insets chegam quando ela entra na janela
            cabecalho.addOnAttachStateChangeListener(
                    new View.OnAttachStateChangeListener() {
                        @Override
                        public void onViewAttachedToWindow(View v) {
                            v.removeOnAttachStateChangeListener(this);
                            ViewCompat.requestApplyInsets(v);
                        }

                        @Override
                        public void onViewDetachedFromWindow(View v) {}
                    });
        }
    }

    public static void navegacao(View nav) {
        final int esquerda = nav.getPaddingLeft(), topo = nav.getPaddingTop();
        final int direita = nav.getPaddingRight(), fundo = nav.getPaddingBottom();
        final int altura = nav.getLayoutParams().height;
        ViewCompat.setOnApplyWindowInsetsListener(
                nav,
                (v, ins) -> {
                    Insets barras =
                            ins.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                                            | WindowInsetsCompat.Type.displayCutout());
                    v.setPadding(esquerda, topo, direita, fundo + barras.bottom);
                    ViewGroup.LayoutParams params = v.getLayoutParams();
                    if (altura > 0 && params.height != altura + barras.bottom) {
                        params.height = altura + barras.bottom;
                        v.setLayoutParams(params);
                    }
                    return ins;
                });
        ViewCompat.requestApplyInsets(nav);
    }
}
