package com.example.renovai.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
 * Lista de cartões de Triagem (Home "recentes" e Listagem Triagens — telas 2.1/2.7).
 *
 * IMPORTANTE: cada linha de /triagens é UM material dentro de uma coleta (ver
 * TriagemResponse). O wireframe mostra um cartão por COLETA (ID TRIAGEM, peso
 * atual, progresso geral) — então quem monta essa lista (a Activity) primeiro
 * AGRUPA as TriagemResponse por coletaId em objetos Grupo, e só então entrega
 * aqui. O adapter não busca nem agrupa nada sozinho.
 */
public class TriagemListAdapter extends RecyclerView.Adapter<TriagemListAdapter.ViewHolder> {

    public enum StatusGrupo { PENDENTE, EM_ANDAMENTO, CONCLUIDA }

    public static class Grupo {
        public final String coletaId;
        public final String triagemIdReferencia; // usado para abrir CompletarTriagemActivity
        public final StatusGrupo status;
        public final BigDecimal pesoAtualKg;
        public final int progressoPercentual;
        public final String dataConclusaoIso; // pode ser null

        public Grupo(String coletaId, String triagemIdReferencia, StatusGrupo status,
                      BigDecimal pesoAtualKg, int progressoPercentual, String dataConclusaoIso) {
            this.coletaId = coletaId;
            this.triagemIdReferencia = triagemIdReferencia;
            this.status = status;
            this.pesoAtualKg = pesoAtualKg;
            this.progressoPercentual = progressoPercentual;
            this.dataConclusaoIso = dataConclusaoIso;
        }
    }

    public interface OnTriagemClickListener {
        void onDetalhesClick(Grupo grupo);
        void onContinuarClick(Grupo grupo);
    }

    private final List<Grupo> itens = new ArrayList<>();
    private final OnTriagemClickListener listener;

    public TriagemListAdapter(OnTriagemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Grupo> novaLista) {
        itens.clear();
        itens.addAll(novaLista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_triagem, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Grupo grupo = itens.get(position);

        String idCurto = grupo.triagemIdReferencia != null && grupo.triagemIdReferencia.length() >= 6
                ? "#" + grupo.triagemIdReferencia.substring(0, 6).toUpperCase(Locale.ROOT)
                : "#------";
        holder.txtIdTriagem.setText(idCurto);

        String idColetaCurto = grupo.coletaId != null && grupo.coletaId.length() >= 6
                ? "#" + grupo.coletaId.substring(0, 6).toUpperCase(Locale.ROOT)
                : "#------";
        holder.txtColetaVinculadaTriagem.setText(idColetaCurto);

        double kg = grupo.pesoAtualKg != null ? grupo.pesoAtualKg.doubleValue() : 0;
        holder.txtPesoTriagem.setText(formatarKg(kg) + " kg");
        holder.txtLabelPesoTriagem.setText(
                grupo.status == StatusGrupo.CONCLUIDA ? "Peso Total" : "Peso Atual");

        holder.progressTriagem.setProgress(grupo.progressoPercentual);
        holder.txtProgressoPercentTriagem.setText(grupo.progressoPercentual + "%");

        boolean concluida = grupo.status == StatusGrupo.CONCLUIDA;
        holder.txtTriadoEm.setVisibility(concluida ? View.VISIBLE : View.GONE);
        holder.btnContinuarTriagem.setVisibility(concluida ? View.GONE : View.VISIBLE);
        if (concluida) {
            holder.txtTriadoEm.setText("Triado em: " + formatarDataCurta(grupo.dataConclusaoIso));
        }

        aplicarStatus(holder, grupo.status);

        holder.txtDetalhesTriagem.setOnClickListener(v -> {
            if (listener != null) listener.onDetalhesClick(grupo);
        });
        holder.btnContinuarTriagem.setOnClickListener(v -> {
            if (listener != null) listener.onContinuarClick(grupo);
        });
    }

    private void aplicarStatus(ViewHolder holder, StatusGrupo status) {
        int corBorda, corBadgeFundo, corBadgeTexto, iconeRes;
        String texto;

        switch (status) {
            case CONCLUIDA:
                corBorda = Color.parseColor("#8BC98F");
                corBadgeFundo = Color.parseColor("#E3F5E4");
                corBadgeTexto = Color.parseColor("#2E7D32");
                iconeRes = R.drawable.ic_check_circle_cooperado;
                texto = "Concluída";
                break;
            case EM_ANDAMENTO:
                corBorda = Color.parseColor("#E8A9BB");
                corBadgeFundo = Color.parseColor("#FBE4EA");
                corBadgeTexto = Color.parseColor("#C2185B");
                iconeRes = R.drawable.ic_refresh_cooperado;
                texto = "Em Andamento";
                break;
            case PENDENTE:
            default:
                corBorda = Color.parseColor("#DDDDDD");
                corBadgeFundo = Color.parseColor("#F0F0F0");
                corBadgeTexto = Color.parseColor("#777777");
                iconeRes = R.drawable.ic_refresh_cooperado;
                texto = "Pendente";
                break;
        }

        GradientDrawable fundoCard = (GradientDrawable) holder.itemView.getBackground().mutate();
        float pxBorda = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.5f,
                holder.itemView.getResources().getDisplayMetrics());
        fundoCard.setStroke((int) pxBorda, corBorda);

        GradientDrawable fundoBadge = (GradientDrawable) holder.containerStatusTriagem.getBackground().mutate();
        fundoBadge.setColor(corBadgeFundo);
        holder.txtStatusTriagem.setText(texto);
        holder.txtStatusTriagem.setTextColor(corBadgeTexto);
        holder.imgStatusTriagem.setImageResource(iconeRes);
    }

    private String formatarKg(double kg) {
        if (kg == Math.floor(kg)) return String.valueOf((long) kg);
        return String.format(Locale.getDefault(), "%.1f", kg);
    }

    private String formatarDataCurta(String dataIso) {
        if (dataIso == null) return "--";
        try {
            LocalDateTime data = LocalDateTime.parse(dataIso.length() > 19 ? dataIso.substring(0, 19) : dataIso);
            return data.format(DateTimeFormatter.ofPattern("dd MMM yyyy", new Locale("pt", "BR")));
        } catch (DateTimeParseException e) {
            return "--";
        }
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtIdTriagem, txtColetaVinculadaTriagem, txtLabelPesoTriagem, txtPesoTriagem,
                txtProgressoPercentTriagem, txtTriadoEm, txtDetalhesTriagem, btnContinuarTriagem,
                txtStatusTriagem;
        ProgressBar progressTriagem;
        View containerStatusTriagem;
        ImageView imgStatusTriagem;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtIdTriagem = itemView.findViewById(R.id.txtIdTriagem);
            txtColetaVinculadaTriagem = itemView.findViewById(R.id.txtColetaVinculadaTriagem);
            txtLabelPesoTriagem = itemView.findViewById(R.id.txtLabelPesoTriagem);
            txtPesoTriagem = itemView.findViewById(R.id.txtPesoTriagem);
            txtProgressoPercentTriagem = itemView.findViewById(R.id.txtProgressoPercentTriagem);
            progressTriagem = itemView.findViewById(R.id.progressTriagem);
            txtTriadoEm = itemView.findViewById(R.id.txtTriadoEm);
            txtDetalhesTriagem = itemView.findViewById(R.id.txtDetalhesTriagem);
            btnContinuarTriagem = itemView.findViewById(R.id.btnContinuarTriagem);
            txtStatusTriagem = itemView.findViewById(R.id.txtStatusTriagem);
            imgStatusTriagem = itemView.findViewById(R.id.imgStatusTriagem);
            containerStatusTriagem = itemView.findViewById(R.id.containerStatusTriagem);
        }
    }
}
