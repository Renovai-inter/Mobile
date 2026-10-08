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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lista de cartões de Rateio (tela 2.5 — Rateios Recebidos).
 *
 * "status" (Fechado/Pendente/Agendada) NÃO existe como campo na API — o backend
 * não expõe um status para Rateio (RateioListaResponse não tem esse campo). É
 * inferido aqui a partir da dataRateio comparada com a data atual (ver
 * RateiController, que monta o Item já com esse status calculado). O adapter só
 * exibe o que já vem pronto.
 */
public class RateiListAdapter extends RecyclerView.Adapter<RateiListAdapter.ViewHolder> {

    public enum StatusRateio { FECHADO, PENDENTE, AGENDADA }

    public static class Item {
        public final String rateioId;
        public final BigDecimal valorTotal;
        public final BigDecimal valorDoCooperado;
        public final StatusRateio status;
        public final String dataRateioIso;

        public Item(String rateioId, BigDecimal valorTotal, BigDecimal valorDoCooperado,
                     StatusRateio status, String dataRateioIso) {
            this.rateioId = rateioId;
            this.valorTotal = valorTotal;
            this.valorDoCooperado = valorDoCooperado;
            this.status = status;
            this.dataRateioIso = dataRateioIso;
        }
    }

    private final List<Item> itens = new ArrayList<>();

    public void submitList(List<Item> novaLista) {
        itens.clear();
        itens.addAll(novaLista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rateio, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = itens.get(position);

        holder.txtValorTotalRateio.setText(formatarReais(item.valorTotal));

        String rotuloValor = item.status == StatusRateio.FECHADO ? "Você recebeu " : "Previsão de ";
        holder.txtValorRecebidoRateio.setText(rotuloValor + formatarReais(item.valorDoCooperado));

        holder.txtReferenteRateio.setText("Referente: " + formatarMesAno(item.dataRateioIso));

        aplicarStatus(holder, item.status);
    }

    private void aplicarStatus(ViewHolder holder, StatusRateio status) {
        int corBorda, corBadgeFundo, corBadgeTexto;
        String texto;

        switch (status) {
            case FECHADO:
                corBorda = Color.parseColor("#8BC98F");
                corBadgeFundo = Color.parseColor("#E3F5E4");
                corBadgeTexto = Color.parseColor("#2E7D32");
                texto = "Fechado";
                break;
            case AGENDADA:
                corBorda = Color.parseColor("#D4E699");
                corBadgeFundo = Color.parseColor("#EFF4D6");
                corBadgeTexto = Color.parseColor("#7C8B2E");
                texto = "Agendada";
                break;
            case PENDENTE:
            default:
                corBorda = Color.parseColor("#E8A9BB");
                corBadgeFundo = Color.parseColor("#FBE4EA");
                corBadgeTexto = Color.parseColor("#C2185B");
                texto = "Pendente";
                break;
        }

        GradientDrawable fundoCard = (GradientDrawable) holder.itemView.getBackground().mutate();
        float pxBorda = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.5f,
                holder.itemView.getResources().getDisplayMetrics());
        fundoCard.setStroke((int) pxBorda, corBorda);

        GradientDrawable fundoBadge = (GradientDrawable) holder.txtStatusRateio.getBackground().mutate();
        fundoBadge.setColor(corBadgeFundo);
        holder.txtStatusRateio.setText(texto);
        holder.txtStatusRateio.setTextColor(corBadgeTexto);
    }

    private String formatarReais(BigDecimal valor) {
        double v = valor != null ? valor.doubleValue() : 0;
        return String.format(new Locale("pt", "BR"), "R$ %,.2f", v);
    }

    private String formatarMesAno(String dataIso) {
        if (dataIso == null) return "--/----";
        try {
            LocalDateTime data = LocalDateTime.parse(dataIso.length() > 19 ? dataIso.substring(0, 19) : dataIso);
            return data.format(DateTimeFormatter.ofPattern("MM/yyyy"));
        } catch (DateTimeParseException e) {
            return "--/----";
        }
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtValorTotalRateio, txtStatusRateio, txtValorRecebidoRateio, txtReferenteRateio;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtValorTotalRateio = itemView.findViewById(R.id.txtValorTotalRateio);
            txtStatusRateio = itemView.findViewById(R.id.txtStatusRateio);
            txtValorRecebidoRateio = itemView.findViewById(R.id.txtValorRecebidoRateio);
            txtReferenteRateio = itemView.findViewById(R.id.txtReferenteRateio);
        }
    }
}
