package com.example.renovai.view;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renovai.GestorBottomNav;
import com.example.renovai.GestorData;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.adapter.ListaAdapter;
import com.example.renovai.dto.response.GestorResponses.FuncionarioDetalhe;
import com.example.renovai.dto.response.RateioListaResponse;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Tela 4.7 — Listagem de rateios. Os rateios já fechados vêm da API; o rateio "aberto" do mês é
 * simulado aqui (a API não guarda rateio aberto) e leva à tela de configuração (4.7.1).
 */
public class GestorRateiosActivity extends GestorBaseActivity {

    private static class Linha {
        YearMonth mes; boolean aberto; String rateioId; double valor; long participantes;
    }

    private ListaAdapter<Linha> adapter;
    private List<RateioListaResponse> fechados = new ArrayList<>();
    private List<FuncionarioDetalhe> funcs = new ArrayList<>();
    private Double lucroAberto;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_gestor_lista, GestorBottomNav.Aba.NENHUMA)) return;
        titulo("Rateios");
        ((TextView) findViewById(R.id.txtTituloLista)).setText("Rateios");
        ((TextView) findViewById(R.id.txtSubtituloLista)).setText("Gerencie a distribuição de ganhos entre contribuidores");
        adapter = new ListaAdapter<>(R.layout.item_gestor_rateio, this::bind);
        RecyclerView r = findViewById(R.id.recyclerLista);
        r.setLayoutManager(new LinearLayoutManager(this));
        r.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!vivo() || funcId() == null) return;
        GestorData.rateios(false, (l, c) -> { fechados = l; if (vivo()) render(); });
        GestorData.funcionarios(false, (l, c) -> { funcs = l; if (vivo()) render(); });
        GestorData.financeiroMes(YearMonth.now(), (v, c) -> { lucroAberto = Math.max(0, v[0] - v[1]); if (vivo()) render(); });
    }

    private void render() {
        List<Linha> linhas = new ArrayList<>();
        YearMonth agora = YearMonth.now();
        boolean temAtual = false;
        List<Linha> feitos = new ArrayList<>();
        for (RateioListaResponse r : fechados) {
            LocalDateTime d = GestorUi.parse(r.getDataRateio());
            Linha l = new Linha();
            l.mes = d == null ? agora : YearMonth.from(d);
            l.rateioId = r.getRateioId();
            l.valor = r.getValorTotalDistribuido() == null ? 0 : r.getValorTotalDistribuido().doubleValue();
            l.participantes = r.getQuantidadePessoas() == null ? 0 : r.getQuantidadePessoas();
            if (l.mes.equals(agora)) temAtual = true;
            feitos.add(l);
        }
        if (!temAtual) {
            int ativos = 0;
            for (FuncionarioDetalhe f : funcs) if (!f.inativo() && !Boolean.TRUE.equals(f.pendente)) ativos++;
            Linha a = new Linha();
            a.mes = agora; a.aberto = true; a.participantes = ativos; a.valor = lucroAberto == null ? -1 : lucroAberto;
            linhas.add(a);
        }
        linhas.addAll(feitos);
        findViewById(R.id.txtVazio).setVisibility(linhas.isEmpty() ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.txtVazio)).setText("Nenhum rateio encontrado.");
        adapter.submit(linhas);
    }

    private void bind(View v, Linha l, int pos) {
        ((TextView) v.findViewById(R.id.txtRateioMes)).setText(String.format(GestorUi.BR, "%02d/%d", l.mes.getMonthValue(), l.mes.getYear()));
        TextView badge = v.findViewById(R.id.txtRateioBadge);
        badge.setText(l.aberto ? "Aberto" : "Concluído");
        GestorUi.badge(badge, l.aberto ? GestorUi.EM_NEGOCIACAO : GestorUi.CONCLUIDO);
        ((TextView) v.findViewById(R.id.txtRateioValor)).setText(l.valor < 0 ? "Calculando..." : GestorUi.dinheiro(l.valor));
        ((TextView) v.findViewById(R.id.txtRateioPart)).setText(String.valueOf(l.participantes));
        GestorUi.borda(v, Color.parseColor("#6FAE76"));
        v.findViewById(R.id.btnRateioDetalhes).setOnClickListener(x -> {
            Intent i = new Intent(this, GestorRateioDetalheActivity.class);
            if (l.aberto) i.putExtra(GestorRateioDetalheActivity.EXTRA_MES, l.mes.toString());
            else i.putExtra(GestorRateioDetalheActivity.EXTRA_RATEIO_ID, l.rateioId);
            startActivity(i);
        });
    }
}
