package com.example.renovai.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renovai.GestorBottomNav;
import com.example.renovai.GestorData;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.adapter.TriagemListAdapter;
import com.example.renovai.controller.TriagemController;
import com.example.renovai.dto.response.TriagemResponse;

import java.util.ArrayList;
import java.util.List;

/** Tela 4.8 — Triagens: Todas / Em Andamento / Concluídas, com atalho para registrar uma nova. */
public class GestorTriagensActivity extends GestorBaseActivity {

    private TriagemListAdapter adapter;
    private List<TriagemListAdapter.Grupo> grupos = new ArrayList<>();
    private int filtro = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_gestor_lista, GestorBottomNav.Aba.NENHUMA)) return;
        titulo("Triagens");
        ((TextView) findViewById(R.id.txtTituloLista)).setText("Triagens");
        ((TextView) findViewById(R.id.txtSubtituloLista)).setText("Gerencie o processamento de materiais");
        TextView acao = findViewById(R.id.btnAcao);
        acao.setText("+  Registrar");
        acao.setVisibility(View.VISIBLE);
        acao.setOnClickListener(v -> startActivity(new Intent(this, GestorNovaTriagemActivity.class)));

        View trilha = findViewById(R.id.trilhaLista);
        trilha.setVisibility(View.VISIBLE);
        GestorUi.segmentado((android.widget.LinearLayout) trilha, new String[]{"Todas", "Em Andamento", "Concluídas"}, 0, false, i -> { filtro = i; render(); });

        adapter = new TriagemListAdapter(new TriagemListAdapter.OnTriagemClickListener() {
            @Override public void onDetalhesClick(TriagemListAdapter.Grupo g) {
                Intent i = new Intent(GestorTriagensActivity.this, GestorTriagemDetalheActivity.class);
                i.putExtra(GestorTriagemDetalheActivity.EXTRA_COLETA_ID, g.coletaId);
                startActivity(i);
            }
            @Override public void onContinuarClick(TriagemListAdapter.Grupo g) {
                Intent i = new Intent(GestorTriagensActivity.this, CompletarTriagemActivity.class);
                i.putExtra(CompletarTriagemActivity.EXTRA_COLETA_ID, g.coletaId);
                startActivity(i);
            }
        });
        RecyclerView r = findViewById(R.id.recyclerLista);
        r.setLayoutManager(new LinearLayoutManager(this));
        r.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!vivo() || funcId() == null) return;
        GestorData.triagens(false, new GestorData.Ouvinte<List<TriagemResponse>>() {
            @Override public void aoReceber(List<TriagemResponse> l, boolean c) {
                grupos = new ArrayList<>(TriagemController.agruparPorColeta(l));
                grupos.sort((a, x) -> String.valueOf(x.dataConclusaoIso).compareTo(String.valueOf(a.dataConclusaoIso)));
                if (vivo()) render();
            }
            @Override public void aoErro(String m) { if (vivo()) toast(m); }
        });
    }

    private void render() {
        List<TriagemListAdapter.Grupo> f = new ArrayList<>();
        for (TriagemListAdapter.Grupo g : grupos) {
            boolean concluida = g.status == TriagemListAdapter.StatusGrupo.CONCLUIDA;
            if (filtro == 0 || (filtro == 1 && !concluida) || (filtro == 2 && concluida)) f.add(g);
        }
        TextView vazio = findViewById(R.id.txtVazio);
        vazio.setText("Nenhuma triagem encontrada.");
        vazio.setVisibility(f.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.submitList(f);
    }
}
