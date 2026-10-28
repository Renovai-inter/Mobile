package com.example.renovai.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renovai.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lista horizontal de "Cooperativas Recentes" da Home. */
public class CooperativaCardAdapter extends RecyclerView.Adapter<CooperativaCardAdapter.ViewHolder> {

    public static class Item {
        public final String nome;
        public final String imagemUrl;
        public final Double mediaAvaliacoes;
        public final Long totalAvaliacoes;

        public Item(String nome, String imagemUrl, Double mediaAvaliacoes, Long totalAvaliacoes) {
            this.nome = nome;
            this.imagemUrl = imagemUrl;
            this.mediaAvaliacoes = mediaAvaliacoes;
            this.totalAvaliacoes = totalAvaliacoes;
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
                .inflate(R.layout.item_cooperativa_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = itens.get(position);

        holder.txtNome.setText(item.nome != null ? item.nome : "Cooperativa");

        if (item.mediaAvaliacoes != null && item.totalAvaliacoes != null && item.totalAvaliacoes > 0) {
            holder.txtAvaliacao.setText(
                    String.format(Locale.getDefault(), "%.1f ★ (%d)", item.mediaAvaliacoes, item.totalAvaliacoes));
        } else {
            holder.txtAvaliacao.setText("Sem avaliações");
        }

        com.example.renovai.Imagens.carregar(
                holder.imgCooperativa, item.imagemUrl, true, com.example.renovai.TipoImagem.COOPERATIVA);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        com.example.renovai.Imagens.limpar(holder.imgCooperativa);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCooperativa;
        TextView txtNome, txtAvaliacao;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCooperativa = itemView.findViewById(R.id.imgCooperativa);
            txtNome = itemView.findViewById(R.id.txtNome);
            txtAvaliacao = itemView.findViewById(R.id.txtAvaliacao);
        }
    }
}
