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
import com.example.renovai.adapter.TriagemListAdapter;
import com.example.renovai.controller.TriagemController;
import com.example.renovai.dto.response.TriagemResponse;

import java.util.ArrayList;
import java.util.List;

/** Tela 2.7 — Histórico de todas as triagens do cooperado (GET /triagens/por-cooperado/{id}). */
public class CooperadoTriagensFragment extends TelaFragment {

    private TextView txtContadorTriagens, txtOrdenarTriagens, txtSemTriagensList;
    private RecyclerView recyclerTriagensList;
    private TriagemListAdapter adapter;
    private List<TriagemListAdapter.Grupo> grupos = new ArrayList<>();
    private boolean ordemDecrescente = true;

    public CooperadoTriagensFragment() {
        super(R.layout.activity_triagens_list);
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

        txtContadorTriagens = findViewById(R.id.txtContadorTriagens);
        txtOrdenarTriagens = findViewById(R.id.txtOrdenarTriagens);
        txtSemTriagensList = findViewById(R.id.txtSemTriagensList);

        recyclerTriagensList = findViewById(R.id.recyclerTriagensList);
        recyclerTriagensList.setLayoutManager(new LinearLayoutManager(tela()));
        adapter =
                new TriagemListAdapter(
                        new TriagemListAdapter.OnTriagemClickListener() {
                            @Override
                            public void onDetalhesClick(TriagemListAdapter.Grupo grupo) {
                                mostrarDetalheTriagem(grupo);
                            }

                            @Override
                            public void onContinuarClick(TriagemListAdapter.Grupo grupo) {
                                Intent intent =
                                        new Intent(
                                                tela(),
                                                CompletarTriagemActivity.class);
                                intent.putExtra(
                                        CompletarTriagemActivity.EXTRA_COLETA_ID, grupo.coletaId);
                                startActivity(intent);
                            }
                        });
        recyclerTriagensList.setAdapter(adapter);

        txtOrdenarTriagens.setOnClickListener(
                v -> {
                    ordemDecrescente = !ordemDecrescente;
                    txtOrdenarTriagens.setText(ordemDecrescente ? "Mais recentes" : "Mais antigas");
                    atualizarLista();
                });

    }

    @Override
    public void onResume() {
        super.onResume();
        if (!vivo() || !CooperadoSession.temCooperadoResolvido()) return;
        carregarTriagens();
    }

    private void carregarTriagens() {
        new TriagemController()
                .listarPorCooperado(
                        CooperadoSession.getFuncionarioId(),
                        new TriagemController.ListaCallback() {
                            @Override
                            public void onSuccess(List<TriagemResponse> triagens) {
                                if (!vivo()) return;
                                grupos = TriagemController.agruparPorColeta(triagens);
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
        List<TriagemListAdapter.Grupo> ordenados = new ArrayList<>(grupos);
        ordenados.sort(
                (a, b) -> {
                    String da = a.dataConclusaoIso, db = b.dataConclusaoIso;
                    if (da == null && db == null) return 0;
                    if (da == null) return ordemDecrescente ? 1 : -1;
                    if (db == null) return ordemDecrescente ? -1 : 1;
                    return ordemDecrescente ? db.compareTo(da) : da.compareTo(db);
                });

        txtContadorTriagens.setText(
                ordenados.size() + (ordenados.size() == 1 ? " encontrada" : " encontradas"));
        boolean vazio = ordenados.isEmpty();
        txtSemTriagensList.setVisibility(vazio ? View.VISIBLE : View.GONE);
        recyclerTriagensList.setVisibility(vazio ? View.GONE : View.VISIBLE);
        adapter.submitList(ordenados);
    }

    private void mostrarDetalheTriagem(TriagemListAdapter.Grupo grupo) {
        double kg = grupo.pesoAtualKg != null ? grupo.pesoAtualKg.doubleValue() : 0;
        DetalheDialogHelper.mostrar(
                tela(),
                "Detalhes da triagem",
                new String[][] {
                    {"COLETA VINCULADA", grupo.coletaId},
                    {
                        "PESO",
                        (kg == Math.floor(kg) ? String.valueOf((long) kg) : String.valueOf(kg))
                                + " kg"
                    },
                    {"PROGRESSO", grupo.progressoPercentual + "%"},
                    {"STATUS", grupo.status.name()},
                },
                com.example.renovai.TipoImagem.TRIAGEM,
                grupo.triagemIdReferencia,
                grupo.imagemUrl);
    }
}
