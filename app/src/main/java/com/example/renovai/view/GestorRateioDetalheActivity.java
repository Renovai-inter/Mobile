package com.example.renovai.view;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.renovai.GestorBottomNav;
import com.example.renovai.GestorCache;
import com.example.renovai.GestorData;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.dto.request.GestorRequests;
import com.example.renovai.dto.response.ColetaResponse;
import com.example.renovai.dto.response.GestorResponses;
import com.example.renovai.dto.response.GestorResponses.FuncionarioDetalhe;
import com.example.renovai.dto.response.TriagemResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Tela 4.7.1 — Rateio de lucros. Aberto: o gestor escolhe o tipo (1 = igual → POST
 * /rateios/executar-geral, 2 = proporcional a coletas + triagens → POST
 * /rateios/executar-proporcional) e fecha; a divisão é calculada pela API, então a tela mostra uma
 * PRÉVIA somente leitura (a API não aceita porcentagens editadas). Concluído: só leitura, com a
 * distribuição gravada (GET /rateios/{id}).
 */
public class GestorRateioDetalheActivity extends GestorBaseActivity {

    public static final String EXTRA_MES = "mes", EXTRA_RATEIO_ID = "rateioId";

    private static class Linha {
        FuncionarioDetalhe f;
        boolean marcado = true;
        double pct, pontos;
        View view;
        CheckBox chk;
        EditText edt;
        TextView valor;
    }

    private YearMonth mes;
    private String rateioId;
    private boolean aberto;
    private int tipo = 0;
    private double lucroTotal, despesas;
    private final List<Linha> linhas = new ArrayList<>();
    private List<FuncionarioDetalhe> funcs = new ArrayList<>();
    private List<ColetaResponse> coletas = new ArrayList<>();
    private List<TriagemResponse> triagens = new ArrayList<>();
    private boolean construido = false, operando, financeiroPronto;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_gestor_rateio_detalhe, GestorBottomNav.Aba.NENHUMA)) return;
        titulo("Rateio de lucros");
        rateioId = getIntent().getStringExtra(EXTRA_RATEIO_ID);
        String m = getIntent().getStringExtra(EXTRA_MES);
        aberto = rateioId == null;
        mes = m != null ? YearMonth.parse(m) : YearMonth.now();
        subtitulo("Referente a: " + GestorUi.nomeMes(mes));

        GestorUi.segmentado(
                findViewById(R.id.trilhaTipo),
                new String[] {"Tipo 1", "Tipo 2"},
                0,
                true,
                i -> {
                    if (!aberto) return;
                    tipo = i;
                    toast(
                            i == 0
                                    ? "Tipo 1: divisão igual entre os participantes."
                                    : "Tipo 2: proporcional às coletas e triagens do mês.");
                    recalcular();
                    atualizarUi();
                });
        findViewById(R.id.btnFecharRateio).setOnClickListener(v -> fechar());
        findViewById(R.id.btnLancarGastos).setOnClickListener(v -> lancarGastos());

        if (aberto) carregarAberto();
        else carregarFechado();
    }

    // ───────── rateio aberto ─────────
    private void carregarAberto() {
        ((TextView) findViewById(R.id.txtRateioStatus)).setText("Status: Aberto");
        GestorData.funcionarios(
                false,
                (l, c) -> {
                    funcs = l;
                    if (vivo()) montar();
                });
        GestorData.coletas(
                false,
                (l, c) -> {
                    coletas = l;
                    if (vivo()) montar();
                });
        GestorData.triagens(
                false,
                (l, c) -> {
                    triagens = l;
                    if (vivo()) montar();
                });
        carregarFinanceiro();
    }

    private void carregarFinanceiro() {
        financeiroPronto = false;
        GestorData.financeiroMes(
                mes,
                new GestorData.Ouvinte<double[]>() {
                    public void aoReceber(double[] v, boolean cache) {
                        lucroTotal = v[0];
                        despesas = v[1];
                        financeiroPronto = true;
                        if (vivo()) {
                            recalcular();
                            atualizarUi();
                        }
                    }

                    public void aoErro(String m) {
                        financeiroPronto = false;
                        if (vivo()) toast(m);
                    }
                });
    }

    private void montar() {

        if (funcs.isEmpty()) return;
        construido = true;
        LinearLayout raiz = findViewById(R.id.listaDistribuicao);
        raiz.removeAllViews();
        linhas.clear();
        for (FuncionarioDetalhe f : funcs) {
            if (f.inativo()) continue;
            Linha l = new Linha();
            l.f = f;
            l.view =
                    LayoutInflater.from(this)
                            .inflate(R.layout.item_gestor_rateio_linha, raiz, false);
            l.chk = l.view.findViewById(R.id.chkParticipa);
            l.edt = l.view.findViewById(R.id.edtPercentual);
            l.valor = l.view.findViewById(R.id.txtRateioValorLinha);
            ((TextView) l.view.findViewById(R.id.txtRateioNome))
                    .setText(f.nome == null ? "—" : f.nome);
            // a API divide entre todos os cooperados ativos e não aceita edição: participação e %
            // ficam só para leitura
            l.chk.setChecked(true);
            l.chk.setEnabled(false);
            l.edt.setEnabled(false);
            raiz.addView(l.view);
            linhas.add(l);
        }
        calcularPontos();
        recalcular();
        atualizarUi();
    }

    private void calcularPontos() {
        for (Linha l : linhas) {
            double p = 0;
            for (ColetaResponse c : coletas)
                if (l.f.funcionarioId != null
                        && l.f.funcionarioId.equalsIgnoreCase(c.getCooperadoId())
                        && GestorUi.noMes(c.getDataColeta(), mes)) p++;
            for (TriagemResponse t : triagens) {
                if (t.getCooperadosNomes() == null
                        || l.f.nome == null
                        || !GestorUi.noMes(t.getDataTriagem(), mes)) continue;
                for (String n : t.getCooperadosNomes())
                    if (l.f.nome.equalsIgnoreCase(n)) {
                        p++;
                        break;
                    }
            }
            l.pontos = p;
        }
    }

    private void recalcular() {
        double pontos = 0;
        for (Linha l : linhas) pontos += l.pontos;
        for (Linha l : linhas)
            l.pct =
                    linhas.isEmpty()
                            ? 0
                            : tipo == 0
                                    ? 100d / linhas.size()
                                    : pontos == 0 ? 0 : 100d * l.pontos / pontos;
    }

    private double distribuir() {
        return Math.max(
                0, lucroTotal); // executar-geral/proporcional distribuem vendas brutas na API atual
    }

    private void atualizarUi() {
        ((TextView) findViewById(R.id.txtLucroTotal)).setText(GestorUi.dinheiro(lucroTotal));
        ((TextView) findViewById(R.id.txtDespesas)).setText(GestorUi.dinheiro(despesas));
        ((TextView) findViewById(R.id.txtLucroDistribuir)).setText(GestorUi.dinheiro(distribuir()));
        int part = 0;
        double soma = 0;
        for (Linha l : linhas) {
            if (l.marcado) {
                part++;
                soma += l.pct;
            }
            if (!l.edt.hasFocus())
                l.edt.setText(
                        l.marcado
                                ? String.format(GestorUi.BR, "%.1f", l.pct).replace(",0", "")
                                : "");
            l.valor.setText(l.marcado ? GestorUi.dinheiro(distribuir() * l.pct / 100) : "-");
            l.view.setAlpha(l.marcado ? 1f : 0.55f);
        }
        ((TextView) findViewById(R.id.txtParticipantes)).setText("Participantes: " + part);
        TextView ts = findViewById(R.id.txtSomaPercentual);
        boolean ok = Math.abs(soma - 100) < 0.11 || part == 0;
        ts.setText("Soma das porcentagens: " + String.format(GestorUi.BR, "%.1f%%", soma));
        ts.setTextColor(Color.parseColor(ok ? "#2E7D32" : "#C62828"));
    }

    private void ocupar(boolean on) {
        operando = on;
        if (!vivo()) return;
        findViewById(R.id.progresso).setVisibility(on ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnFecharRateio).setEnabled(!on);
        findViewById(R.id.btnLancarGastos).setEnabled(!on);
        findViewById(R.id.edtAgua).setEnabled(!on);
        findViewById(R.id.edtEnergia).setEnabled(!on);
    }

    private void lancarGastos() {
        if (!aberto || operando) return;
        EditText campoAgua = findViewById(R.id.edtAgua),
                campoEnergia = findViewById(R.id.edtEnergia);
        Double agua = GestorUi.parseValor(campoAgua.getText().toString());
        Double energia = GestorUi.parseValor(campoEnergia.getText().toString());
        if ((!campoAgua.getText().toString().trim().isEmpty()
                        && (agua == null || !Double.isFinite(agua) || agua < 0))
                || (!campoEnergia.getText().toString().trim().isEmpty()
                        && (energia == null || !Double.isFinite(energia) || energia < 0))) {
            toast("Informe valores válidos e não negativos.");
            return;
        }
        int quantidade =
                (agua != null && agua > 0 ? 1 : 0) + (energia != null && energia > 0 ? 1 : 0);
        if (quantidade == 0) {
            toast("Informe o gasto de água e/ou energia.");
            return;
        }
        ocupar(true);
        java.util.concurrent.atomic.AtomicInteger pend =
                new java.util.concurrent.atomic.AtomicInteger(quantidade);
        java.util.concurrent.atomic.AtomicInteger falhas =
                new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.BiConsumer<EditText, Boolean> retorno =
                (campo, ok) -> {
                    if (!ok) falhas.incrementAndGet();
                    if (vivo() && ok) campo.setText("");
                    if (pend.decrementAndGet() != 0) return;
                    ocupar(false);
                    if (!vivo()) return;
                    toast(
                            falhas.get() == 0
                                    ? "Gastos lançados."
                                    : "Nem todos os gastos foram confirmados. Confira os"
                                          + " lançamentos antes de repetir.");
                    carregarFinanceiro();
                };
        if (agua != null && agua > 0) lancar("Água", agua, ok -> retorno.accept(campoAgua, ok));
        if (energia != null && energia > 0)
            lancar("Energia", energia, ok -> retorno.accept(campoEnergia, ok));
    }

    private void lancar(String nome, double valor, java.util.function.Consumer<Boolean> fim) {
        GestorData.api()
                .despesas(coopId())
                .enqueue(
                        new Callback<List<GestorResponses.Despesa>>() {
                            public void onResponse(
                                    Call<List<GestorResponses.Despesa>> c,
                                    Response<List<GestorResponses.Despesa>> r) {
                                if (!r.isSuccessful() || r.body() == null) {
                                    fim.accept(false);
                                    return;
                                }
                                for (GestorResponses.Despesa d : r.body()) {
                                    if (GestorUi.norm(d.nome).equals(GestorUi.norm(nome))) {
                                        registrar(d.despesaId, valor, fim);
                                        return;
                                    }
                                }
                                GestorData.api()
                                        .criarDespesa(
                                                new GestorRequests.Despesa(
                                                        coopId(), nome, "VARIAVEL"))
                                        .enqueue(
                                                new Callback<GestorResponses.Despesa>() {
                                                    public void onResponse(
                                                            Call<GestorResponses.Despesa> c2,
                                                            Response<GestorResponses.Despesa> r2) {
                                                        if (r2.isSuccessful() && r2.body() != null)
                                                            registrar(
                                                                    r2.body().despesaId,
                                                                    valor,
                                                                    fim);
                                                        else fim.accept(false);
                                                    }

                                                    public void onFailure(
                                                            Call<GestorResponses.Despesa> c2,
                                                            Throwable t) {
                                                        fim.accept(false);
                                                    }
                                                });
                            }

                            public void onFailure(
                                    Call<List<GestorResponses.Despesa>> c, Throwable t) {
                                fim.accept(false);
                            }
                        });
    }

    private void registrar(
            String despesaId, double valor, java.util.function.Consumer<Boolean> fim) {
        GestorData.api()
                .lancarDespesa(
                        new GestorRequests.Lancamento(despesaId, valor, mes.atDay(1).toString()))
                .enqueue(
                        new Callback<GestorResponses.Lancamento>() {
                            public void onResponse(
                                    Call<GestorResponses.Lancamento> c,
                                    Response<GestorResponses.Lancamento> r) {
                                fim.accept(r.isSuccessful());
                            }

                            public void onFailure(Call<GestorResponses.Lancamento> c, Throwable t) {
                                fim.accept(false);
                            }
                        });
    }

    private void fechar() {
        if (!aberto || operando) return;
        if (!financeiroPronto) {
            toast("Aguarde a confirmação dos valores do período.");
            return;
        }
        if (lucroTotal <= 0) {
            toast("Não há vendas registradas neste mês para distribuir.");
            return;
        }
        if (linhas.isEmpty()) {
            toast("Nenhum cooperado ativo para receber o rateio.");
            return;
        }
        GestorUi.confirmar(
                this,
                "Fechar rateio",
                "Fechar o rateio de "
                        + GestorUi.nomeMes(mes)
                        + " ("
                        + (tipo == 0 ? "Tipo 1 — divisão igual" : "Tipo 2 — proporcional")
                        + ")? "
                        + "A distribuição entre os "
                        + linhas.size()
                        + " cooperados ativos é calculada pela API. Esta ação não pode ser"
                        + " desfeita.",
                "Fechar rateio",
                this::enviar);
    }

    /**
     * Usa o cálculo da própria API: executar-geral (Tipo 1) ou executar-proporcional (Tipo 2),
     * sobre o mês inteiro.
     */
    private void enviar() {
        if (operando) return;
        ocupar(true);
        View prog = findViewById(R.id.progresso);
        prog.setVisibility(View.VISIBLE);
        GestorRequests.RateioPeriodo body =
                GestorRequests.RateioPeriodo.paraMes(funcId(), coopId(), mes);
        Callback<GestorResponses.RateioRealizado> cb =
                new Callback<GestorResponses.RateioRealizado>() {
                    @Override
                    public void onResponse(
                            Call<GestorResponses.RateioRealizado> c,
                            Response<GestorResponses.RateioRealizado> r) {
                        if (r.isSuccessful()) sucesso(prog);
                        else {
                            ocupar(false);
                            if (vivo()) toast(mensagem(r, "Não foi possível fechar o rateio"));
                        }
                    }

                    @Override
                    public void onFailure(Call<GestorResponses.RateioRealizado> c, Throwable t) {
                        ocupar(false);
                        if (vivo())
                            toast(
                                    "Não foi possível confirmar o rateio. Consulte a listagem antes"
                                        + " de repetir.");
                    }
                };
        if (tipo == 0) GestorData.api().rateioGeral(body).enqueue(cb);
        else GestorData.api().rateioProporcional(body).enqueue(cb);
    }

    private void sucesso(View prog) {
        com.example.renovai.CooperadoPreload.invalidar();
        if (!vivo()) return;
        prog.setVisibility(View.GONE);
        GestorData.invalidar(GestorCache.RATEIOS);
        toast("Rateio fechado e distribuído.");
        finish();
    }

    private static String mensagem(Response<?> r, String padrao) {
        try {
            if (r.errorBody() != null) {
                com.google.gson.JsonObject o =
                        com.google.gson.JsonParser.parseString(r.errorBody().string())
                                .getAsJsonObject();
                if (o.has("mensagem")) return o.get("mensagem").getAsString();
            }
        } catch (Exception ignored) {
        }
        return padrao + " (erro " + r.code() + ").";
    }

    // ───────── rateio já fechado ─────────
    private void carregarFechado() {
        View gastos = findViewById(R.id.cardGastos);
        gastos.setVisibility(View.GONE);
        findViewById(R.id.btnFecharRateio).setVisibility(View.GONE);
        TextView st = findViewById(R.id.txtRateioStatus);
        st.setText("Status: Concluído");
        st.setTextColor(Color.parseColor("#2E7D32"));
        LinearLayout trilha = findViewById(R.id.trilhaTipo);
        GestorData.api()
                .rateio(rateioId)
                .enqueue(
                        new Callback<GestorResponses.RateioDetalhe>() {
                            @Override
                            public void onResponse(
                                    Call<GestorResponses.RateioDetalhe> c,
                                    Response<GestorResponses.RateioDetalhe> r) {
                                if (!r.isSuccessful() || r.body() == null || !vivo()) {
                                    toast("Não foi possível carregar o rateio.");
                                    return;
                                }
                                mostrarFechado(r.body());
                            }

                            @Override
                            public void onFailure(
                                    Call<GestorResponses.RateioDetalhe> c, Throwable t) {
                                toast("Sem conexão com o servidor.");
                            }
                        });
    }

    private void mostrarFechado(GestorResponses.RateioDetalhe d) {
        LocalDateTime data = GestorUi.parse(d.dataRateio);
        if (data != null) {
            mes = YearMonth.from(data);
            subtitulo("Referente a: " + GestorUi.nomeMes(mes));
        }
        tipo = GestorUi.tem(d.tipoRateio, "PROPORC") ? 1 : 0;
        LinearLayout trilha = findViewById(R.id.trilhaTipo);
        GestorUi.segmentado(trilha, new String[] {"Tipo 1", "Tipo 2"}, tipo, true, null);
        for (int i = 0; i < trilha.getChildCount(); i++) trilha.getChildAt(i).setClickable(false);
        double total = d.valorTotalDistribuido == null ? 0 : d.valorTotalDistribuido;
        ((TextView) findViewById(R.id.txtLucroDistribuir)).setText(GestorUi.dinheiro(total));
        GestorData.financeiroMes(
                mes,
                (v, c) -> {
                    if (vivo()) {
                        ((TextView) findViewById(R.id.txtLucroTotal))
                                .setText(GestorUi.dinheiro(v[0]));
                        ((TextView) findViewById(R.id.txtDespesas))
                                .setText(GestorUi.dinheiro(v[1]));
                    }
                });
        LinearLayout raiz = findViewById(R.id.listaDistribuicao);
        raiz.removeAllViews();
        int part = 0;
        if (d.funcionarios != null)
            for (GestorResponses.ResultadoInd f : d.funcionarios) {
                View v =
                        LayoutInflater.from(this)
                                .inflate(R.layout.item_gestor_rateio_linha, raiz, false);
                CheckBox chk = v.findViewById(R.id.chkParticipa);
                chk.setChecked(true);
                chk.setEnabled(false);
                ((TextView) v.findViewById(R.id.txtRateioNome)).setText(f.funcionarioNome);
                EditText e = v.findViewById(R.id.edtPercentual);
                e.setEnabled(false);
                e.setText(
                        f.percentualParticipacao == null
                                ? ""
                                : String.format(GestorUi.BR, "%.1f", f.percentualParticipacao)
                                        .replace(",0", ""));
                ((TextView) v.findViewById(R.id.txtRateioValorLinha))
                        .setText(GestorUi.dinheiro(f.valorRateio));
                raiz.addView(v);
                part++;
            }
        ((TextView) findViewById(R.id.txtParticipantes)).setText("Participantes: " + part);
    }
}
