package com.example.renovai.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Adapter genérico: um layout de item + uma função que preenche a view (evita uma classe por lista). */
public class ListaAdapter<T> extends RecyclerView.Adapter<ListaAdapter.VH> {

    public interface Binder<T> {
        void bind(View v, T item, int posicao);
    }

    private final int layout;
    private final Binder<T> binder;
    private final List<T> itens = new ArrayList<>();

    public ListaAdapter(int layout, Binder<T> binder) {
        this.layout = layout;
        this.binder = binder;
    }

    public void submit(List<T> novos) {
        itens.clear();
        if (novos != null) itens.addAll(novos);
        notifyDataSetChanged();
    }

    public List<T> itens() {
        return itens;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        binder.bind(h.itemView, itens.get(position), position);
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    public static class VH extends RecyclerView.ViewHolder {
        VH(View v) {
            super(v);
        }
    }
}
