package com.example.renovai.view;

import android.app.Activity;
import android.graphics.Color;
import android.widget.TextView;

import com.example.renovai.R;

/**
 * Marca a etapa atual no indicador include_etapas_redefinicao (verificação em dois fatores):
 * 1 E-mail → 2 Código enviado por e-mail → 3 Nova senha. Etapas anteriores ficam verdes com ✓,
 * a atual em vinho e as seguintes em cinza.
 */
final class EtapasRedefinicao {

    static final int EMAIL = 1;
    static final int CODIGO = 2;
    static final int NOVA_SENHA = 3;

    private static final int VERDE = Color.parseColor("#519059");
    private static final int VINHO = Color.parseColor("#882B4E");
    private static final int CINZA = Color.parseColor("#9CA3AF");
    private static final int LINHA_CINZA = Color.parseColor("#D1D5DB");

    private static final int[] CIRCULOS = {R.id.etapa1, R.id.etapa2, R.id.etapa3};
    private static final int[] TEXTOS = {R.id.etapa1Texto, R.id.etapa2Texto, R.id.etapa3Texto};
    private static final int[] LINHAS = {R.id.etapaLinha1, R.id.etapaLinha2};

    private EtapasRedefinicao() {}

    static void marcar(Activity activity, int etapaAtual) {
        for (int i = 0; i < CIRCULOS.length; i++) {
            int etapa = i + 1;
            TextView circulo = activity.findViewById(CIRCULOS[i]);
            TextView texto = activity.findViewById(TEXTOS[i]);
            if (circulo == null || texto == null) continue;

            if (etapa < etapaAtual) {
                circulo.setBackgroundResource(R.drawable.bg_etapa_concluida);
                circulo.setTextColor(Color.WHITE);
                circulo.setText("✓");
                texto.setTextColor(VERDE);
            } else if (etapa == etapaAtual) {
                circulo.setBackgroundResource(R.drawable.bg_etapa_ativa);
                circulo.setTextColor(Color.WHITE);
                circulo.setText(String.valueOf(etapa));
                texto.setTextColor(VINHO);
            } else {
                circulo.setBackgroundResource(R.drawable.bg_etapa_pendente);
                circulo.setTextColor(CINZA);
                circulo.setText(String.valueOf(etapa));
                texto.setTextColor(CINZA);
            }
        }
        for (int i = 0; i < LINHAS.length; i++) {
            android.view.View linha = activity.findViewById(LINHAS[i]);
            if (linha != null) linha.setBackgroundColor(i + 1 < etapaAtual ? VERDE : LINHA_CINZA);
        }
    }
}
