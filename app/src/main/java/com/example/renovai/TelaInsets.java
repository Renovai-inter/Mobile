package com.example.renovai;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Aplica os espaços do sistema a partir do layout original, sem acumular padding. */
public final class TelaInsets {
    private TelaInsets() {}

    public static void conteudo(Activity activity, boolean barraInferior) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) return;
        View raiz = content.getChildAt(0);
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
                            topo + barras.top,
                            direita + barras.right,
                            fundo
                                    + (barraInferior
                                            ? Math.max(0, teclado - barras.bottom)
                                            : Math.max(barras.bottom, teclado)));
                    return ins;
                });
        ViewCompat.requestApplyInsets(raiz);
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
