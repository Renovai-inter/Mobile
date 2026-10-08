package com.example.renovai.view;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renovai.GestorBottomNav;
import com.example.renovai.GestorRelatorios;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.adapter.ListaAdapter;

import java.util.List;

/** Tela 4.4 — Seus relatórios: lista com miniatura do PDF, período e acesso para abrir/baixar. */
public class GestorRelatoriosActivity extends GestorBaseActivity {

    private ListaAdapter<GestorRelatorios.Meta> adapter;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_gestor_lista, GestorBottomNav.Aba.NENHUMA)) return;
        titulo("Seus Relatórios");
        ((TextView) findViewById(R.id.txtTituloLista)).setText("Relatórios");
        ((TextView) findViewById(R.id.txtSubtituloLista)).setText("Crie e baixe relatorios de negociações ou Impacto ambiental");
        TextView acao = findViewById(R.id.btnAcao);
        acao.setText("+  Registrar");
        acao.setVisibility(View.VISIBLE);
        acao.setOnClickListener(v -> startActivity(new Intent(this, GestorCriarRelatorioActivity.class)));

        adapter = new ListaAdapter<>(R.layout.item_gestor_relatorio, (v, m, i) -> {
            TextView nome = v.findViewById(R.id.txtRelatorioNome);
            nome.setText(m.nome);
            nome.setPaintFlags(nome.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            ((TextView) v.findViewById(R.id.txtRelatorioPeriodo)).setText("Período: " + GestorUi.data(m.inicio) + " - " + GestorUi.data(m.fim));
            String tipo = GestorRelatorios.NEGOCIACOES.equals(m.tipo) ? "Negociações" : GestorRelatorios.IMPACTO.equals(m.tipo) ? "Impacto Ambiental" : "Triagem";
            ((TextView) v.findViewById(R.id.txtRelatorioTipo)).setText(tipo + " • toque para abrir, segure para mais opções");
            ImageView img = v.findViewById(R.id.imgRelatorio);
            Bitmap bmp = GestorRelatorios.miniatura(m);
            if (bmp != null) img.setImageBitmap(bmp); else img.setImageDrawable(null);
            v.setOnClickListener(x -> GestorRelatorios.abrir(this, m));
            v.setOnLongClickListener(x -> {
                new AlertDialog.Builder(this).setTitle(m.nome)
                        .setItems(new String[]{"Abrir", "Compartilhar / baixar", "Excluir"}, (d, w) -> {
                            if (w == 0) GestorRelatorios.abrir(this, m);
                            else if (w == 1) GestorRelatorios.compartilhar(this, m);
                            else GestorUi.confirmar(this, "Excluir relatório", "Excluir \"" + m.nome + "\"?", "Excluir", () -> { GestorRelatorios.excluir(this, m); carregar(); });
                        }).show();
                return true;
            });
        });
        RecyclerView r = findViewById(R.id.recyclerLista);
        r.setLayoutManager(new LinearLayoutManager(this));
        r.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (vivo() && funcId() != null) carregar();
    }

    private void carregar() {
        List<GestorRelatorios.Meta> l = GestorRelatorios.listar(this);
        TextView vazio = findViewById(R.id.txtVazio);
        vazio.setText("Nenhum relatório criado ainda. Toque em \"Registrar\".");
        vazio.setVisibility(l.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.submit(l);
    }
}
