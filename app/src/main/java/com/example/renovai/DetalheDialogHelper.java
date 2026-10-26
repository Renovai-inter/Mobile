package com.example.renovai;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

/**
 * Monta o diálogo genérico de "Detalhes" (Coleta ou Triagem) usado em
 * CooperadoHomeFragment, CooperadoColetasFragment e CooperadoTriagensFragment.
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
        mostrar(activity, titulo, linhas, null, null);
    }

    /** Detalhe de coleta: mostra a foto (busca em GET /coletas/{id} se a listagem não trouxe). */
    public static AlertDialog mostrar(Activity activity, String titulo, String[][] linhas,
            String coletaId, String imagemUrl) {
        return mostrar(activity, titulo, linhas, TipoImagem.COLETA, coletaId, imagemUrl);
    }

    public static AlertDialog mostrar(Activity activity, String titulo, String[][] linhas,
            TipoImagem tipo, String registroId, String imagemUrl) {
        LinearLayout container = (LinearLayout) LayoutInflater.from(activity)
                .inflate(R.layout.dialog_detalhe_generico, null)
                .findViewById(R.id.containerDetalheGenerico);

        // Foto no topo do detalhe (Cloudinary, com cache do Glide — ver Imagens).
        ImageView foto = null;
        if (registroId != null || ImagemUrl.normalizar(imagemUrl) != null) {
            foto = new ImageView(activity);
            foto.setId(R.id.imgFotoDetalhe);
            foto.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 200));
            params.bottomMargin = dp(activity, 12);
            container.addView(foto, params);
            if (tipo == TipoImagem.COLETA) Imagens.coleta(foto, registroId, imagemUrl, false);
            else Imagens.carregar(foto, imagemUrl, false, tipo);
        }

        for (String[] linha : linhas) {
            container.addView(criarLinha(activity, linha[0], linha[1]));
        }

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(titulo)
                .setView((View) container.getParent())
                .setPositiveButton("Fechar", null)
                .show();
        ImageView alvo = foto;
        dialog.setOnDismissListener(ignorado -> { if (alvo != null) Imagens.limpar(alvo); });
        return dialog;
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
