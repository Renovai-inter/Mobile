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
import com.example.renovai.dto.response.PedidoResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lista de "Pedidos Recentes" da Home. Cada item já chega com o texto de detalhe
 * (material + cooperativa + peso) pronto, montado pelo HomeFragment — o adapter só
 * exibe, não busca nada da rede.
 */
public class PedidoAdapter extends RecyclerView.Adapter<PedidoAdapter.ViewHolder> {

    /** Três baldes normalizados a partir do texto livre que vem em statusAtual. */
    public enum StatusPedido { ANDAMENTO, CONCLUIDO, CANCELADO }

    public static class Item {
        public final PedidoResponse pedido;
        public final String materialTitulo;   // ex: "PET" — vazio se não houver itens
        public final String detalheSubtitulo; // ex: "Cooperativa tal - 50kg"
        public final StatusPedido status;

        public Item(PedidoResponse pedido, String materialTitulo, String detalheSubtitulo, StatusPedido status) {
            this.pedido = pedido;
            this.materialTitulo = materialTitulo;
            this.detalheSubtitulo = detalheSubtitulo;
            this.status = status;
        }
    }

    private final List<Item> itens = new ArrayList<>();

    public void submitList(List<Item> novaLista) {
        itens.clear();
        itens.addAll(novaLista);
        notifyDataSetChanged();
    }

    /** Classifica o statusAtual (texto livre vindo do banco) num dos 3 estados da UI. */
    public static StatusPedido classificarStatus(String statusAtual, boolean temDataConclusao) {
        if (statusAtual != null) {
            String s = statusAtual.trim().toUpperCase(Locale.ROOT);
            if (s.contains("CANCEL")) return StatusPedido.CANCELADO;
            if (s.contains("CONCLU") || s.contains("FINALIZ") || s.contains("ACEIT")) return StatusPedido.CONCLUIDO;
        }
        return temDataConclusao ? StatusPedido.CONCLUIDO : StatusPedido.ANDAMENTO;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pedido_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = itens.get(position);

        holder.txtMaterial.setText(item.materialTitulo != null && !item.materialTitulo.isEmpty()
                ? item.materialTitulo : "Material");
        holder.txtDetalhe.setText(item.detalheSubtitulo != null ? item.detalheSubtitulo : "-");
        holder.txtData.setText(formatarData(item.pedido.getDataPedido()));

        aplicarStatus(holder, item.status);
    }

    private void aplicarStatus(ViewHolder holder, StatusPedido status) {
        int corBorda, corBadgeFundo, corBadgeTexto;
        String texto;

        switch (status) {
            case CONCLUIDO:
                corBorda = Color.parseColor("#8BC98F");
                corBadgeFundo = Color.parseColor("#E3F5E4");
                corBadgeTexto = Color.parseColor("#2E7D32");
                texto = "CONCLUÍDO";
                break;
            case CANCELADO:
                corBorda = Color.parseColor("#882B4E");
                corBadgeFundo = Color.parseColor("#F6D9E3");
                corBadgeTexto = Color.parseColor("#882B4E");
                texto = "CANCELADO";
                break;
            case ANDAMENTO:
            default:
                corBorda = Color.parseColor("#E8A9BB");
                corBadgeFundo = Color.parseColor("#FBE4EA");
                corBadgeTexto = Color.parseColor("#C2185B");
                texto = "ANDAMENTO";
                break;
        }

        GradientDrawable fundoCard = (GradientDrawable) holder.itemView.getBackground().mutate();
        float pxBorda = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.5f,
                holder.itemView.getResources().getDisplayMetrics());
        fundoCard.setStroke((int) pxBorda, corBorda);

        GradientDrawable fundoBadge = (GradientDrawable) holder.txtStatus.getBackground().mutate();
        fundoBadge.setColor(corBadgeFundo);

        holder.txtStatus.setText(texto);
        holder.txtStatus.setTextColor(corBadgeTexto);
    }

    private String formatarData(String dataIso) {
        if (dataIso == null) return "";
        try {
            LocalDateTime data = LocalDateTime.parse(dataIso.length() > 19 ? dataIso.substring(0, 19) : dataIso);
            String hora = data.format(DateTimeFormatter.ofPattern("HH:mm"));
            if (data.toLocalDate().isEqual(LocalDate.now())) {
                return "Hoje, " + hora;
            }
            return data.format(DateTimeFormatter.ofPattern("dd/MM")) + ", " + hora;
        } catch (DateTimeParseException e) {
            return "";
        }
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtMaterial, txtDetalhe, txtStatus, txtData;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtMaterial = itemView.findViewById(R.id.txtMaterial);
            txtDetalhe = itemView.findViewById(R.id.txtDetalhe);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtData = itemView.findViewById(R.id.txtData);
        }
    }
}
