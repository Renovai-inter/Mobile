package com.example.renovai.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renovai.GestorBottomNav;
import com.example.renovai.GestorData;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.adapter.ColetaListAdapter;
import com.example.renovai.dto.response.ColetaResponse;

import java.util.ArrayList;
import java.util.List;

/** Tela 4.2 — Listagem de coletas com filtros de estado (Todos/Agendado/Pendente/Imediata) e origem (Interna/Externa). */
public class GestorColetasActivity extends GestorBaseActivity {

    private ColetaListAdapter adapter;
    private List<ColetaResponse> todas = new ArrayList<>();
    private int estado = 0, origem = 0;
    private boolean recentesPrimeiro = true;
    private TextView contador, ordenar, vazio;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_gestor_coletas, GestorBottomNav.Aba.COLETAS)) return;
        contador = findViewById(R.id.txtContador);
        ordenar = findViewById(R.id.txtOrdenar);
        vazio = findViewById(R.id.txtVazio);

        GestorUi.segmentado(findViewById(R.id.trilhaEstado), new String[]{"Todos", "Agendado", "Pendente", "Imediata"}, 0, false, i -> { estado = i; render(); });
        GestorUi.segmentado(findViewById(R.id.trilhaOrigem), new String[]{"Todas", "Interna", "Externa"}, 0, false, i -> { origem = i; render(); });

        adapter = new ColetaListAdapter(c -> {
            Intent i = new Intent(this, GestorColetaDetalheActivity.class);
            i.putExtra(GestorColetaDetalheActivity.EXTRA_COLETA_ID, c.getColetaId());
            startActivity(i);
        });
        RecyclerView r = findViewById(R.id.recyclerColetas);
        r.setLayoutManager(new LinearLayoutManager(this));
        r.setAdapter(adapter);

        ordenar.setOnClickListener(v -> {
            recentesPrimeiro = !recentesPrimeiro;
            ordenar.setText(recentesPrimeiro ? "Mais recentes" : "Mais antigas");
            render();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!vivo() || funcId() == null) return;
        GestorData.coletas(false, new GestorData.Ouvinte<List<ColetaResponse>>() {
            @Override public void aoReceber(List<ColetaResponse> l, boolean c) { todas = l; if (vivo()) render(); }
            @Override public void aoErro(String m) { if (vivo()) toast(m); }
        });
    }

    /** 1 = Agendado, 2 = Pendente, 3 = Imediata, 0 = demais (só aparecem em "Todos"). */
    static int estadoColeta(String s) {
        if (GestorUi.tem(s, "AGEND")) return 1;
        if (GestorUi.tem(s, "IMEDIAT", "URGENT")) return 3;
        if (s == null || s.trim().isEmpty() || GestorUi.tem(s, "PENDENT", "ABERT")) return 2;
        return 0;
    }

    private void render() {
        List<ColetaResponse> f = new ArrayList<>();
        for (ColetaResponse c : todas) {
            if (estado != 0 && estadoColeta(c.getStatusAtual()) != estado) continue;
            boolean ext = GestorUi.tem(c.getTipoColeta(), "EXTERN");
            if (origem == 1 && ext) continue;
            if (origem == 2 && !ext) continue;
            f.add(c);
        }
        f.sort((a, c) -> recentesPrimeiro
                ? String.valueOf(c.getDataColeta()).compareTo(String.valueOf(a.getDataColeta()))
                : String.valueOf(a.getDataColeta()).compareTo(String.valueOf(c.getDataColeta())));
        contador.setText(f.size() + (f.size() == 1 ? " encontrada" : " encontradas"));
        vazio.setVisibility(f.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.submitList(f);
    }
}
