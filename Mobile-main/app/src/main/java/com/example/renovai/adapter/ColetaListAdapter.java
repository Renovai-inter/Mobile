package com.example.renovai.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renovai.R;
import com.example.renovai.dto.response.ColetaResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lista de cartões de Coleta (Home "recentes" e Listagem Coletas — telas 2.1/2.6). Segue o mesmo
 * padrão do PedidoAdapter (Empresa): cor da borda/badge mutada em tempo de execução a partir de um
 * GradientDrawable, texto do status classificado a partir do texto livre de statusAtual (a API não
 * expõe um enum fixo).
 */
public class ColetaListAdapter extends RecyclerView.Adapter<ColetaListAdapter.ViewHolder> {

    public interface OnColetaClickListener {
        void onDetalhesClick(ColetaResponse coleta);
    }

    private final List<ColetaResponse> itens = new ArrayList<>();
    private final OnColetaClickListener listener;

    public ColetaListAdapter(OnColetaClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ColetaResponse> novaLista) {
        itens.clear();
        itens.addAll(novaLista);
        notifyDataSetChanged();
    }

    /**
     * Classifica o statusAtual (texto livre vindo do banco, referencia=COLETA) num rótulo de
     * exibição em português. Os valores reais de status não são conhecidos sem consultar o banco
     * (ver IMPLEMENTACAO.md) — por isso o casamento é por palavra-chave, no mesmo espírito do
     * PedidoAdapter.
     */
    private static String rotuloStatus(String statusAtual) {
        if (statusAtual == null || statusAtual.trim().isEmpty()) return "Pendente";
        String s = statusAtual.trim().toUpperCase(Locale.ROOT);
        if (s.contains("CANCEL")) return "Cancelada";
        if (s.contains("CONCLU") || s.contains("ESTOQUE")) return "Concluída";
        if (s.contains("AGEND")) return "Agendada";
        if (s.contains("ABERT")) return "Aberta";
        return capitalizar(statusAtual);
    }

    private static String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) return texto;
        String minusculo = texto.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(minusculo.charAt(0)) + minusculo.substring(1);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_coleta, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ColetaResponse coleta = itens.get(position);

        String idCurto =
                coleta.getColetaId() != null && coleta.getColetaId().length() >= 6
                        ? "#" + coleta.getColetaId().substring(0, 6).toUpperCase(Locale.ROOT)
                        : "#------";
        holder.txtIdColeta.setText(idCurto);

        String tipo =
                com.example.renovai.GestorUi.tem(coleta.getTipoColeta(), "EXTERN")
                        ? "Externa"
                        : "Interna";
        String rotuloStatus = rotuloStatus(coleta.getStatusAtual());
        holder.txtStatusColeta.setText(tipo + "/" + rotuloStatus);

        holder.txtFeitoPorColeta.setText(
                coleta.getCooperadoNome() != null ? coleta.getCooperadoNome() : "—");

        double kg = coleta.getQuantidadeKg() != null ? coleta.getQuantidadeKg().doubleValue() : 0;
        holder.txtPesoColeta.setText(formatarKg(kg) + " kg");

        aplicarCorStatus(holder, rotuloStatus);

        holder.txtDetalhesColeta.setOnClickListener(
                v -> {
                    if (listener != null) listener.onDetalhesClick(coleta);
                });
        holder.itemView.setOnClickListener(
                v -> {
                    if (listener != null) listener.onDetalhesClick(coleta);
                });
    }

    private void aplicarCorStatus(ViewHolder holder, String rotuloStatus) {
        int corBorda, corBadgeFundo, corBadgeTexto;

        switch (rotuloStatus) {
            case "Cancelada":
                corBorda = Color.parseColor("#882B4E");
                corBadgeFundo = Color.parseColor("#F6D9E3");
                corBadgeTexto = Color.parseColor("#882B4E");
                break;
            case "Aberta":
            case "Pendente":
                corBorda = Color.parseColor("#E8A9BB");
                corBadgeFundo = Color.parseColor("#FBE4EA");
                corBadgeTexto = Color.parseColor("#C2185B");
                break;
            case "Agendada":
            case "Concluída":
            default:
                corBorda = Color.parseColor("#8BC98F");
                corBadgeFundo = Color.parseColor("#E3F5E4");
                corBadgeTexto = Color.parseColor("#2E7D32");
                break;
        }

        GradientDrawable fundoCard = (GradientDrawable) holder.itemView.getBackground().mutate();
        float pxBorda =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        1.5f,
                        holder.itemView.getResources().getDisplayMetrics());
        fundoCard.setStroke((int) pxBorda, corBorda);

        GradientDrawable fundoBadge =
                (GradientDrawable) holder.txtStatusColeta.getBackground().mutate();
        fundoBadge.setColor(corBadgeFundo);
        holder.txtStatusColeta.setTextColor(corBadgeTexto);
    }

    private String formatarKg(double kg) {
        if (kg == Math.floor(kg)) return String.valueOf((long) kg);
        return String.format(Locale.getDefault(), "%.1f", kg);
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtIdColeta, txtStatusColeta, txtFeitoPorColeta, txtPesoColeta, txtDetalhesColeta;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtIdColeta = itemView.findViewById(R.id.txtIdColeta);
            txtStatusColeta = itemView.findViewById(R.id.txtStatusColeta);
            txtFeitoPorColeta = itemView.findViewById(R.id.txtFeitoPorColeta);
            txtPesoColeta = itemView.findViewById(R.id.txtPesoColeta);
            txtDetalhesColeta = itemView.findViewById(R.id.txtDetalhesColeta);
        }
    }
}
