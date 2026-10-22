package com.example.renovai;

import android.widget.ScrollView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Com EdgeToEdge ligado, o adjustResize não encolhe a tela quando o teclado abre, e o botão fica
 * escondido atrás dele. Aqui a tela (um ScrollView) ganha padding embaixo do tamanho do teclado e
 * rola até o fim, para o campo e o botão continuarem à vista.
 */
public final class TelaComTeclado {

    private TelaComTeclado() {}

    public static void ajustar(ScrollView tela) {
        ViewCompat.setOnApplyWindowInsetsListener(
                tela,
                (v, insets) -> {
                    Insets teclado = insets.getInsets(WindowInsetsCompat.Type.ime());
                    Insets barras = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
                    v.setPadding(0, 0, 0, Math.max(teclado.bottom, barras.bottom));
                    if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                        tela.post(() -> tela.smoothScrollTo(0, tela.getChildAt(0).getHeight()));
                    }
                    return insets;
                });
    }
}
