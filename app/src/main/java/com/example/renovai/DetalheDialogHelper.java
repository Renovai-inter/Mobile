package com.example.renovai;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

/**
 * Monta o diálogo genérico de "Detalhes" (Coleta ou Triagem) usado em
 * CooperadoHomeActivity, ListagemColetasActivity e ListagemTriagensActivity.
 *
 * Por que é um diálogo e não uma 8ª Activity: o prompt de implementação lista
 * exatamente 7 Activities para a área de Cooperado (nenhuma "Detalhe*Activity").
 * O wireframe (Cooperado.png) tem uma tela "Detalhes" reaproveitada a partir dos
 * botões "Detalhes" das listagens — aqui ela vira um AlertDialog dentro da
 * própria Activity que a chamou, mostrando os mesmos campos, sem estourar a
 * lista de entregáveis pedida.
 */
public final class DetalheDialogHelper {

    private DetalheDialogHelper() {
    }

    public static void mostrar(Activity activity, String titulo, String[][] linhas) {
        LinearLayout container = (LinearLayout) LayoutInflater.from(activity)
                .inflate(R.layout.dialog_detalhe_generico, null)
                .findViewById(R.id.containerDetalheGenerico);

        for (String[] linha : linhas) {
            container.addView(criarLinha(activity, linha[0], linha[1]));
        }

        new AlertDialog.Builder(activity)
                .setTitle(titulo)
                .setView((View) container.getParent())
                .setPositiveButton("Fechar", null)
                .show();
    }

    private static View criarLinha(Activity activity, String label, String valor) {
        LinearLayout linha = new LinearLayout(activity);
        linha.setOrientation(LinearLayout.VERTICAL);
        int paddingV = dp(activity, 8);
        linha.setPadding(0, paddingV, 0, paddingV);

        TextView txtLabel = new TextView(activity);
        txtLabel.setText(label);
        txtLabel.setTextSize(11f);
        txtLabel.setTextColor(0xFF8A8A8A);

        TextView txtValor = new TextView(activity);
        txtValor.setText(valor != null && !valor.trim().isEmpty() ? valor : "—");
        txtValor.setTextSize(15f);
        txtValor.setTextColor(0xFF222222);

        linha.addView(txtLabel);
        linha.addView(txtValor);
        return linha;
    }

    private static int dp(Activity activity, int valor) {
        float densidade = activity.getResources().getDisplayMetrics().density;
        return (int) (valor * densidade);
    }
}
