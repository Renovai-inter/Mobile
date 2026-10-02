package com.example.renovai.view;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.renovai.GestorBottomNav;
import com.example.renovai.GestorData;
import com.example.renovai.GestorRelatorios;
import com.example.renovai.R;
import com.example.renovai.dto.response.ColetaResponse;
import com.example.renovai.dto.response.GestorResponses;
import com.example.renovai.dto.response.TriagemResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Tela 4.4.1 — Criar novo relatório (Negociações ou Impacto Ambiental) para um período. */
public class GestorCriarRelatorioActivity extends GestorBaseActivity {

    private boolean impacto = false, criando;
    private LocalDate inicio = LocalDate.now().minusDays(30), fim;
    private List<GestorResponses.Negociacao> negs = new ArrayList<>();
    private List<TriagemResponse> triagens = new ArrayList<>();
    private List<ColetaResponse> coletas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_gestor_criar_relatorio, GestorBottomNav.Aba.NENHUMA))
            return;
        titulo("Criar Relatório");
        findViewById(R.id.cardNegociacoes).setOnClickListener(v -> tipo(false));
        findViewById(R.id.cardImpacto).setOnClickListener(v -> tipo(true));
        findViewById(R.id.boxInicio).setOnClickListener(v -> escolher(true));
        findViewById(R.id.boxFim).setOnClickListener(v -> escolher(false));
        findViewById(R.id.btnCancelar).setOnClickListener(v -> finish());
        findViewById(R.id.btnCriarRelatorio).setOnClickListener(v -> criar());
        atualizar();
    }

    private void tipo(boolean imp) {
        if (criando) return;
        impacto = imp;
        findViewById(R.id.cardNegociacoes)
                .setBackgroundResource(
                        imp
                                ? R.drawable.card_unselected_background
                                : R.drawable.card_selected_background);
        findViewById(R.id.cardImpacto)
                .setBackgroundResource(
                        imp
                                ? R.drawable.card_selected_background
                                : R.drawable.card_unselected_background);
    }

    private void atualizar() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        ((TextView) findViewById(R.id.txtInicio)).setText(inicio.format(f));
        TextView tf = findViewById(R.id.txtFim);
        tf.setText(fim == null ? "Selecionar" : fim.format(f));
        tf.setTextColor(fim == null ? 0xFFA0A4AB : 0xFF111111);
    }

    private void escolher(boolean ehInicio) {
        if (criando) return;
        LocalDate base = ehInicio ? inicio : (fim != null ? fim : LocalDate.now());
        new DatePickerDialog(
                        this,
                        (v, a, m, d) -> {
                            LocalDate esc = LocalDate.of(a, m + 1, d);
                            if (ehInicio) inicio = esc;
                            else fim = esc;
                            atualizar();
                        },
                        base.getYear(),
                        base.getMonthValue() - 1,
                        base.getDayOfMonth())
                .show();
    }

    private void criar() {
        if (criando) return;
        if (fim == null) {
            toast("Selecione a data final do período.");
            return;
        }
        if (fim.isBefore(inicio)) {
            toast("A data final não pode ser anterior à inicial.");
            return;
        }
        criando = true;
        findViewById(R.id.edtNomeRelatorio).setEnabled(false);
        final View prog = findViewById(R.id.progresso);
        prog.setVisibility(View.VISIBLE);
        findViewById(R.id.btnCriarRelatorio).setEnabled(false);
        final int total = impacto ? 2 : 1;
        final AtomicInteger pend = new AtomicInteger(total);
        final AtomicInteger falhas = new AtomicInteger();
        final Runnable fimBusca =
                () -> {
                    if (pend.decrementAndGet() != 0 || !vivo()) return;
                    if (falhas.get() > 0) falha(prog, "todos os dados do relatório", -1);
                    else gerar(prog);
                };

        if (!impacto) {
            GestorData.api()
                    .negociacoes(coopId())
                    .enqueue(
                            new Callback<List<GestorResponses.Negociacao>>() {
                                @Override
                                public void onResponse(
                                        Call<List<GestorResponses.Negociacao>> c,
                                        Response<List<GestorResponses.Negociacao>> r) {
                                    if (r.isSuccessful() && r.body() != null) negs = r.body();
                                    else falhas.incrementAndGet();
                                    fimBusca.run();
                                }

                                @Override
                                public void onFailure(
                                        Call<List<GestorResponses.Negociacao>> c, Throwable t) {
                                    falhas.incrementAndGet();
                                    fimBusca.run();
                                }
                            });
        } else {
            GestorData.api()
                    .triagens(coopId())
                    .enqueue(
                            new Callback<List<TriagemResponse>>() {
                                @Override
                                public void onResponse(
                                        Call<List<TriagemResponse>> c,
                                        Response<List<TriagemResponse>> r) {
                                    if (r.isSuccessful() && r.body() != null) triagens = r.body();
                                    else falhas.incrementAndGet();
                                    fimBusca.run();
                                }

                                @Override
                                public void onFailure(Call<List<TriagemResponse>> c, Throwable t) {
                                    falhas.incrementAndGet();
                                    fimBusca.run();
                                }
                            });
            GestorData.api()
                    .coletas(coopId())
                    .enqueue(
                            new Callback<List<ColetaResponse>>() {
                                @Override
                                public void onResponse(
                                        Call<List<ColetaResponse>> c,
                                        Response<List<ColetaResponse>> r) {
                                    if (r.isSuccessful() && r.body() != null) coletas = r.body();
                                    else falhas.incrementAndGet();
                                    fimBusca.run();
                                }

                                @Override
                                public void onFailure(Call<List<ColetaResponse>> c, Throwable t) {
                                    falhas.incrementAndGet();
                                    fimBusca.run();
                                }
                            });
        }
    }

    private void falha(View prog, String o, int code) {
        criando = false;
        if (!vivo()) return;
        findViewById(R.id.edtNomeRelatorio).setEnabled(true);
        prog.setVisibility(View.GONE);
        findViewById(R.id.btnCriarRelatorio).setEnabled(true);
        toast(
                code < 0
                        ? "Sem conexão com o servidor."
                        : "Não foi possível carregar " + o + " (erro " + code + ").");
    }

    private void gerar(View prog) {
        String digitado =
                ((EditText) findViewById(R.id.edtNomeRelatorio)).getText().toString().trim();
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM");
        String nome =
                !digitado.isEmpty()
                        ? digitado
                        : (impacto ? "Impacto Ambiental " : "Negociações ")
                                + inicio.format(f)
                                + " a "
                                + fim.format(f);
        final boolean tipoImpacto = impacto;
        final LocalDate dataInicial = inicio, dataFinal = fim;
        new Thread(
                        () -> {
                            try {
                                GestorRelatorios.Meta m =
                                        tipoImpacto
                                                ? GestorRelatorios.gerarImpacto(
                                                        getApplicationContext(),
                                                        nome,
                                                        dataInicial,
                                                        dataFinal,
                                                        triagens,
                                                        coletas)
                                                : GestorRelatorios.gerarNegociacoes(
                                                        getApplicationContext(),
                                                        nome,
                                                        dataInicial,
                                                        dataFinal,
                                                        negs);
                                runOnUiThread(
                                        () -> {
                                            if (!vivo()) return;
                                            prog.setVisibility(View.GONE);
                                            toast("Relatório criado.");
                                            GestorRelatorios.abrir(this, m);
                                            finish();
                                        });
                            } catch (Exception e) {
                                runOnUiThread(
                                        () -> {
                                            if (!vivo()) return;
                                            criando = false;
                                            prog.setVisibility(View.GONE);
                                            findViewById(R.id.edtNomeRelatorio).setEnabled(true);
                                            findViewById(R.id.btnCriarRelatorio).setEnabled(true);
                                            toast(
                                                    "Não foi possível gerar o PDF: "
                                                            + e.getMessage());
                                        });
                            }
                        })
                .start();
    }
}
