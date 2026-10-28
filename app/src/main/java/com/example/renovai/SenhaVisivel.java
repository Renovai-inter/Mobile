package com.example.renovai;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.widget.EditText;

import androidx.core.content.ContextCompat;

/**
 * Olho no fim de um campo de senha: tocar nele mostra ou esconde o que foi digitado. Usa o
 * drawable da direita do próprio EditText, então não precisa mudar o layout.
 */
public final class SenhaVisivel {

    private SenhaVisivel() {}

    @SuppressLint("ClickableViewAccessibility")
    public static void ativar(EditText campo) {
        atualizarIcone(campo, false);
        campo.setOnTouchListener(
                (v, event) -> {
                    Drawable olho = campo.getCompoundDrawablesRelative()[2];
                    if (olho == null || event.getAction() != MotionEvent.ACTION_UP) return false;
                    // Área de toque: o ícone mais o padding à direita dele.
                    int inicioIcone = campo.getWidth() - campo.getPaddingEnd() - olho.getBounds().width();
                    if (event.getX() < inicioIcone - campo.getCompoundDrawablePadding()) return false;
                    alternar(campo);
                    return true;
                });
    }

    private static void alternar(EditText campo) {
        boolean visivel = !(campo.getTransformationMethod() instanceof PasswordTransformationMethod);
        int cursor = campo.getSelectionEnd();
        // Trocar o TransformationMethod (e não o inputType) mantém a fonte e o teclado iguais.
        campo.setTransformationMethod(visivel ? PasswordTransformationMethod.getInstance() : null);
        campo.setSelection(Math.max(0, Math.min(cursor, campo.length())));
        atualizarIcone(campo, !visivel);
    }

    private static void atualizarIcone(EditText campo, boolean visivel) {
        Drawable olho =
                ContextCompat.getDrawable(
                        campo.getContext(), visivel ? R.drawable.ic_olho_fechado : R.drawable.ic_olho_aberto);
        Drawable[] atuais = campo.getCompoundDrawablesRelative();
        campo.setCompoundDrawablesRelativeWithIntrinsicBounds(atuais[0], atuais[1], olho, atuais[3]);
    }
}
