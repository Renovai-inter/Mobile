package com.example.renovai.view;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renovai.CooperadoSession;
import com.example.renovai.CooperadoUi;
import com.example.renovai.DetalheDialogHelper;
import com.example.renovai.R;
import com.example.renovai.adapter.ColetaListAdapter;
import com.example.renovai.controller.ColetaController;
import com.example.renovai.dto.response.ColetaResponse;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Tela 2.6 — Histórico de todas as coletas do cooperado (GET /coletas/por-cooperado/{id}). */
public class CooperadoColetasFragment extends TelaFragment {

    private TextView txtContadorColetas, txtOrdenarColetas, txtSemColetasList;
    private RecyclerView recyclerColetasList;
    private ColetaListAdapter adapter;
    private List<ColetaResponse> coletas = new ArrayList<>();
    private boolean ordemDecrescente = true;

    public CooperadoColetasFragment() {
        super(R.layout.activity_coletas_list);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        TextView txtNomeCooperativaHeader = findViewById(R.id.txtNomeCooperativaHeader);
        TextView txtIniciaisCooperado = findViewById(R.id.txtIniciaisCooperado);
        String cooperativa = CooperadoSession.getCooperativaNome();
        txtNomeCooperativaHeader.setText(com.example.renovai.GestorUi.nomeCabecalho(cooperativa));
        txtIniciaisCooperado.setText(CooperadoUi.gerarIniciais(CooperadoSession.getUsuarioNome()));
        findViewById(R.id.btnPerfilHeader)
                .setOnClickListener(
                        v -> abrirAba(R.id.nav_perfil_cooperado));

        txtContadorColetas = findViewById(R.id.txtContadorColetas);
        txtOrdenarColetas = findViewById(R.id.txtOrdenarColetas);
        txtSemColetasList = findViewById(R.id.txtSemColetasList);

        recyclerColetasList = findViewById(R.id.recyclerColetasList);
        recyclerColetasList.setLayoutManager(new LinearLayoutManager(tela()));
        adapter = new ColetaListAdapter(this::mostrarDetalheColeta);
        recyclerColetasList.setAdapter(adapter);

        txtOrdenarColetas.setOnClickListener(
                v -> {
                    ordemDecrescente = !ordemDecrescente;
                    txtOrdenarColetas.setText(ordemDecrescente ? "Mais recentes" : "Mais antigas");
                    atualizarLista();
                });

        FloatingActionButton fab = findViewById(R.id.fabNovaColetaList);
        fab.setOnClickListener(v -> startActivity(new Intent(tela(), AdicionarColetaActivity.class)));

    }

    @Override
    public void onResume() {
        super.onResume();
        if (!vivo() || !CooperadoSession.temCooperadoResolvido()) return;
        carregarColetas();
    }

    private void carregarColetas() {
        new ColetaController()
                .listarPorCooperado(
                        CooperadoSession.getFuncionarioId(),
                        new ColetaController.ListaCallback() {
                            @Override
                            public void onSuccess(List<ColetaResponse> resultado) {
                                if (!vivo()) return;
                                coletas = resultado;
                                atualizarLista();
                            }

                            @Override
                            public void onErro(String mensagem) {
                                if (!vivo()) return;
                                Toast.makeText(
                                                tela(),
                                                mensagem,
                                                Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
    }

    private void atualizarLista() {
        List<ColetaResponse> ordenadas = new ArrayList<>(coletas);
        ordenadas.sort(
                (a, b) -> {
                    if (a.getDataColeta() == null || b.getDataColeta() == null) return 0;
                    return ordemDecrescente
                            ? b.getDataColeta().compareTo(a.getDataColeta())
                            : a.getDataColeta().compareTo(b.getDataColeta());
                });

        txtContadorColetas.setText(
                ordenadas.size() + (ordenadas.size() == 1 ? " encontrada" : " encontradas"));
        boolean vazio = ordenadas.isEmpty();
        txtSemColetasList.setVisibility(vazio ? View.VISIBLE : View.GONE);
        recyclerColetasList.setVisibility(vazio ? View.GONE : View.VISIBLE);
        adapter.submitList(ordenadas);
    }

    private void mostrarDetalheColeta(ColetaResponse coleta) {
        double kg = coleta.getQuantidadeKg() != null ? coleta.getQuantidadeKg().doubleValue() : 0;
        DetalheDialogHelper.mostrar(
                tela(),
                "Detalhes da coleta",
                new String[][] {
                    {"QUANTIDADE", formatarKg(kg) + " kg"},
                    {"DATA", formatarData(coleta.getDataColeta())},
                    {"FEITO POR", coleta.getCooperadoNome()},
                    {
                        "TIPO",
                        com.example.renovai.GestorUi.tem(coleta.getTipoColeta(), "EXTERN")
                                ? "Externo"
                                : "Interno"
                    },
                    {"ROTA", coleta.getRotaId() != null ? coleta.getRotaId() : "-----"},
                    {"STATUS", coleta.getStatusAtual()},
                },
                coleta.getColetaId(),
                coleta.getImagemUrl());
    }

    private String formatarKg(double kg) {
        if (kg == Math.floor(kg)) return String.valueOf((long) kg);
        return String.format(Locale.getDefault(), "%.1f", kg);
    }

    private String formatarData(String dataIso) {
        if (dataIso == null) return "--";
        try {
            LocalDateTime data =
                    LocalDateTime.parse(dataIso.length() > 19 ? dataIso.substring(0, 19) : dataIso);
            return data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException e) {
            return "--";
        }
    }
}
