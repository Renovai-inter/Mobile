package com.example.renovai.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.renovai.CooperadoSession;
import com.example.renovai.GestorBottomNav;
import com.example.renovai.GestorData;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.dto.response.ColetaResponse;
import com.example.renovai.dto.response.GestorResponses;
import com.example.renovai.dto.response.TriagemResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tela 4.2.1 — Coleta detalhada: imagem, materiais e pesos, cooperado responsável e rota vinculada. */
public class GestorColetaDetalheActivity extends GestorBaseActivity {

    public static final String EXTRA_COLETA_ID = "coletaId";

    private String coletaId;
    private List<ColetaResponse> coletas = new ArrayList<>();
    private List<TriagemResponse> triagens = new ArrayList<>();
    private List<GestorResponses.Rota> rotas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_gestor_coleta_detalhe, GestorBottomNav.Aba.NENHUMA)) return;
        titulo("Detalhes");
        coletaId = getIntent().getStringExtra(EXTRA_COLETA_ID);
        GestorData.coletas(false, (l, c) -> { coletas = l; if (vivo()) render(); });
        GestorData.triagens(false, (l, c) -> { triagens = l; if (vivo()) render(); });
        GestorData.rotas(false, (l, c) -> { rotas = l; if (vivo()) render(); });
    }

    private void render() {
        ColetaResponse col = null;
        for (ColetaResponse c : coletas) if (c.getColetaId() != null && c.getColetaId().equalsIgnoreCase(coletaId)) col = c;
        if (col == null) return;

        ((TextView) findViewById(R.id.txtQuantidade)).setText(GestorUi.kg(col.getQuantidadeKg() == null ? 0d : col.getQuantidadeKg().doubleValue()));
        ((TextView) findViewById(R.id.txtData)).setText(GestorUi.data(col.getDataColeta()));
        ((TextView) findViewById(R.id.txtFeitoPor)).setText(col.getCooperadoNome() == null ? "—" : col.getCooperadoNome());
        boolean ext = GestorUi.tem(col.getTipoColeta(), "EXTERN");
        ((TextView) findViewById(R.id.txtMetodo)).setText(ext ? "Externo" : "Interno");

        String rotaNome = "-----", origem = "Cooperativa " + (CooperadoSession.getCooperativaNome() == null ? "" : CooperadoSession.getCooperativaNome());
        if (col.getRotaId() != null) {
            for (GestorResponses.Rota r : rotas) {
                if (col.getRotaId().equalsIgnoreCase(r.rotaId)) {
                    rotaNome = r.nome == null ? "-----" : r.nome;
                    if (r.enderecos != null && !r.enderecos.isEmpty()) {
                        GestorResponses.RotaEnd e = r.enderecos.get(0);
                        origem = GestorRotaTexto.endereco(e);
                    } else origem = rotaNome;
                }
            }
        } else if (ext) origem = "—";
        ((TextView) findViewById(R.id.txtRota)).setText(rotaNome);
        ((TextView) findViewById(R.id.txtOrigem)).setText(origem.trim());

        LinearLayout lista = findViewById(R.id.listaMateriais);
        lista.removeAllViews();
        Map<String, Double> porMaterial = new LinkedHashMap<>();
        int linhas = 0; boolean algumPeso = false;
        for (TriagemResponse t : triagens) {
            if (t.getColetaId() == null || !t.getColetaId().equalsIgnoreCase(coletaId)) continue;
            linhas++;
            double kg = com.example.renovai.controller.TriagemController.pesoReal(t).doubleValue();
            if (kg > 0) { algumPeso = true; porMaterial.merge(t.getMaterialCategoria() == null ? "Material" : t.getMaterialCategoria(), kg, Double::sum); }
        }
        for (Map.Entry<String, Double> e : porMaterial.entrySet()) {
            View v = LayoutInflater.from(this).inflate(R.layout.item_gestor_material_linha, lista, false);
            ((TextView) v.findViewById(R.id.txtMaterialNome)).setText(e.getKey());
            ((TextView) v.findViewById(R.id.txtMaterialKg)).setText(GestorUi.kg(e.getValue()));
            lista.addView(v);
        }
        findViewById(R.id.txtSemMateriais).setVisibility(porMaterial.isEmpty() ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.txtTriada)).setText(algumPeso ? "Sim" : (linhas > 0 ? "Em andamento" : "Não"));

        ImageView foto = findViewById(R.id.imgFotoColeta);
        if (col.getImagemUrl() != null && !col.getImagemUrl().trim().isEmpty()) Glide.with(this).load(col.getImagemUrl()).into(foto);
    }
}
