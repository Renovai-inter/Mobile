package com.example.renovai.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.renovai.GestorBottomNav;
import com.example.renovai.GestorData;
import com.example.renovai.GestorRelatorios;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.dto.response.ColetaResponse;
import com.example.renovai.dto.response.GestorResponses.FuncionarioDetalhe;
import com.example.renovai.dto.response.TriagemResponse;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Tela 4.8.1 — Detalhes da triagem: totais, participantes, materiais separados e exportação em PDF. */
public class GestorTriagemDetalheActivity extends GestorBaseActivity {

    public static final String EXTRA_COLETA_ID = "coletaId";

    private String coletaId;
    private List<TriagemResponse> linhas = new ArrayList<>();
    private List<TriagemResponse> todas = new ArrayList<>();
    private List<ColetaResponse> coletas = new ArrayList<>();
    private List<FuncionarioDetalhe> funcs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_gestor_triagem_detalhe, GestorBottomNav.Aba.NENHUMA)) return;
        titulo("Detalhes Triagem");
        coletaId = getIntent().getStringExtra(EXTRA_COLETA_ID);
        findViewById(R.id.btnExportar).setOnClickListener(v -> exportar());
        GestorData.triagens(false, (l, c) -> { todas = l; if (vivo()) render(); });
        GestorData.coletas(false, (l, c) -> { coletas = l; if (vivo()) render(); });
        GestorData.funcionarios(false, (l, c) -> { funcs = l; if (vivo()) render(); });
    }

    private String feitoPor() {
        for (ColetaResponse c : coletas) if (c.getColetaId() != null && c.getColetaId().equalsIgnoreCase(coletaId)) return c.getCooperadoNome();
        return null;
    }

    private void render() {
        linhas = new ArrayList<>();
        for (TriagemResponse t : todas) if (t.getColetaId() != null && t.getColetaId().equalsIgnoreCase(coletaId)) linhas.add(t);
        if (linhas.isEmpty()) return;

        boolean concluida = true; double total = 0; String data = null;
        Set<String> equipe = new LinkedHashSet<>();
        for (TriagemResponse t : linhas) {
            if (!GestorUi.tem(t.getStatusAtual(), "CONCLU")) concluida = false;
            total += com.example.renovai.controller.TriagemController.pesoReal(t).doubleValue();
            if (t.getDataTriagem() != null && (data == null || t.getDataTriagem().compareTo(data) < 0)) data = t.getDataTriagem();
            if (t.getCooperadosNomes() != null) equipe.addAll(t.getCooperadosNomes());
        }
        ((TextView) findViewById(R.id.txtTriagemTitulo)).setText("Triagem " + GestorUi.idCurto(coletaId));
        TextView st = findViewById(R.id.txtTriagemStatus);
        st.setText(concluida ? "Concluída" : "Em Andamento");
        GestorUi.badge(st, concluida ? GestorUi.CONCLUIDO : GestorUi.EM_NEGOCIACAO);
        ((TextView) findViewById(R.id.txtTriagemData)).setText("Realizada em " + GestorUi.data(data));
        ((TextView) findViewById(R.id.txtTriagemColeta)).setText(GestorUi.idCurto(coletaId));
        ((TextView) findViewById(R.id.txtTriagemEmpresa)).setText(feitoPor() == null ? "—" : feitoPor());
        ((TextView) findViewById(R.id.txtTriagemTotal)).setText(GestorUi.kg(total));
        ((TextView) findViewById(R.id.txtTriagemDuracao)).setText("—");
        ((TextView) findViewById(R.id.txtStatPeso)).setText(GestorUi.numero(total));
        ((TextView) findViewById(R.id.txtStatMateriais)).setText(String.valueOf(linhas.size()));
        ((TextView) findViewById(R.id.txtStatFunc)).setText(String.valueOf(equipe.size()));

        LinearLayout part = findViewById(R.id.listaParticipantes);
        part.removeAllViews();
        for (String nome : equipe) {
            FuncionarioDetalhe f = null;
            for (FuncionarioDetalhe x : funcs) if (x.nome != null && x.nome.equalsIgnoreCase(nome)) f = x;
            View v = LayoutInflater.from(this).inflate(R.layout.item_gestor_funcionario, part, false);
            GestorUi.borda(v.findViewById(R.id.cardFunc), 0xFF519059);
            ((TextView) v.findViewById(R.id.txtAvatarFunc)).setText(GestorUi.iniciais(nome));
            ((TextView) v.findViewById(R.id.txtFuncNome)).setText(nome);
            ((TextView) v.findViewById(R.id.txtFuncCargo)).setText(f != null && f.cargo != null ? f.cargo : "Função");
            if (f != null) {
                final String fid = f.funcionarioId;
                v.setOnClickListener(x -> { Intent i = new Intent(this, GestorFuncionarioDetalheActivity.class); i.putExtra(GestorFuncionarioDetalheActivity.EXTRA_ID, fid); startActivity(i); });
            }
            part.addView(v);
        }

        LinearLayout mats = findViewById(R.id.listaMateriaisSep);
        mats.removeAllViews();
        for (TriagemResponse t : linhas) {
            View v = LayoutInflater.from(this).inflate(R.layout.item_gestor_material_linha, mats, false);
            ((TextView) v.findViewById(R.id.txtMaterialNome)).setText(t.getMaterialCategoria() == null ? "Material" : t.getMaterialCategoria());
            ((TextView) v.findViewById(R.id.txtMaterialKg)).setText(GestorUi.kg(com.example.renovai.controller.TriagemController.pesoReal(t).doubleValue()));
            mats.addView(v);
        }
    }

    private void exportar() {
        if (linhas.isEmpty()) { toast("Aguarde o carregamento da triagem."); return; }
        View prog = findViewById(R.id.progresso);
        prog.setVisibility(View.VISIBLE);
        final List<TriagemResponse> copia = new ArrayList<>(linhas);
        final String por = feitoPor();
        new Thread(() -> {
            try {
                GestorRelatorios.Meta m = GestorRelatorios.gerarTriagem(getApplicationContext(), coletaId, copia, por);
                runOnUiThread(() -> { if (!vivo()) return; prog.setVisibility(View.GONE); toast("Relatório salvo em \"Seus Relatórios\"."); GestorRelatorios.abrir(this, m); });
            } catch (Exception e) {
                runOnUiThread(() -> { if (!vivo()) return; prog.setVisibility(View.GONE); toast("Não foi possível gerar o PDF: " + e.getMessage()); });
            }
        }).start();
    }
}
