package com.example.renovai.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.renovai.GestorUi;
import com.example.renovai.MotoristaBottomNav;
import com.example.renovai.MotoristaData;
import com.example.renovai.MotoristaRotas;
import com.example.renovai.R;
import com.example.renovai.dto.response.GestorResponses;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Tela 3.1 — Home do Motorista: contadores do dia e as 3 seções de rotas. */
public class MotoristaHomeActivity extends MotoristaBaseActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_motorista_home, MotoristaBottomNav.Aba.HOME)) return;
        LocalDate hoje = LocalDate.now();
        String rotulo =
                hoje.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, GestorUi.BR)
                        + ", "
                        + hoje.format(DateTimeFormatter.ofPattern("dd 'de' MMMM", GestorUi.BR));
        ((TextView) findViewById(R.id.txtDataHoje)).setText(capitalizar(rotulo));
    }

    private static String capitalizar(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!vivo() || coopId() == null) return;
        MotoristaData.rotas(
                true,
                new com.example.renovai.GestorData.Ouvinte<List<GestorResponses.Rota>>() {
                    @Override
                    public void aoReceber(List<GestorResponses.Rota> l, boolean c) {
                        if (vivo()) render(l);
                    }

                    @Override
                    public void aoErro(String m) {
                        if (vivo()) toast(m);
                    }
                });
    }

    private void render(List<GestorResponses.Rota> todas) {
        MotoristaRotas.Grupo g = MotoristaRotas.classificar(todas);

        ((TextView) findViewById(R.id.txtQtdAndamento)).setText("—");
        ((TextView) findViewById(R.id.txtQtdConcluidas)).setText("—");
        ((TextView) findViewById(R.id.txtQtdAtribuidas))
                .setText(String.valueOf(g.disponiveis.size()));

        preencher(
                R.id.listaAndamento,
                R.id.txtSemAndamento,
                java.util.Collections.emptyList(),
                false);
        preencher(R.id.listaAgendadas, R.id.txtSemAgendadas, g.disponiveis, false);
        preencher(R.id.listaConcluidas, R.id.txtSemConcluidas, g.inativas, true);
    }

    private void preencher(
            int idLista, int idVazio, List<GestorResponses.Rota> lista, boolean concluidas) {
        LinearLayout raiz = findViewById(idLista);
        raiz.removeAllViews();
        findViewById(idVazio).setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
        for (GestorResponses.Rota r : lista) {
            View v = LayoutInflater.from(this).inflate(R.layout.item_motorista_rota, raiz, false);
            boolean iniciada = false;
            MotoristaRotaCard.bind(
                    this,
                    v,
                    r,
                    iniciada,
                    concluidas,
                    () -> MotoristaRotaCard.abrirDetalhe(this, r.rotaId));
            raiz.addView(v);
        }
    }
}
