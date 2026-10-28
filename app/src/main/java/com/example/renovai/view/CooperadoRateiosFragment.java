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
import com.example.renovai.R;
import com.example.renovai.adapter.RateiListAdapter;
import com.example.renovai.controller.RateiController;

import java.util.ArrayList;
import java.util.List;

/**
 * Tela 2.5 — Rateios Recebidos.
 *
 * <p>A API não tem GET /rateios/por-cooperado — ver RateiController (mobile) para como a lista é
 * montada (por-cooperativa + distribuição de cada rateio) e IMPLEMENTACAO.md para a heurística de
 * status (Fechado/Pendente/Agendada), que também não existe como campo pronto na API.
 */
public class CooperadoRateiosFragment extends TelaFragment {

    private TextView txtContadorRateios, txtOrdenarRateios, txtSemRateios;
    private RecyclerView recyclerRateios;
    private RateiListAdapter adapter;
    private List<RateiListAdapter.Item> itens = new ArrayList<>();
    private boolean maisAntigasPrimeiro = true;

    public CooperadoRateiosFragment() {
        super(R.layout.activity_rateios);
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

        txtContadorRateios = findViewById(R.id.txtContadorRateios);
        txtOrdenarRateios = findViewById(R.id.txtOrdenarRateios);
        txtSemRateios = findViewById(R.id.txtSemRateios);

        recyclerRateios = findViewById(R.id.recyclerRateios);
        recyclerRateios.setLayoutManager(new LinearLayoutManager(tela()));
        adapter = new RateiListAdapter();
        recyclerRateios.setAdapter(adapter);

        txtOrdenarRateios.setOnClickListener(
                v -> {
                    maisAntigasPrimeiro = !maisAntigasPrimeiro;
                    txtOrdenarRateios.setText(
                            maisAntigasPrimeiro ? "Mais antigas" : "Mais recentes");
                    atualizarLista();
                });

    }

    @Override
    public void onResume() {
        super.onResume();
        if (!vivo() || !CooperadoSession.temCooperadoResolvido()) return;
        carregarRateios();
    }

    private void carregarRateios() {
        new RateiController()
                .listarRecebidosPorCooperado(
                        CooperadoSession.getCooperativaId(),
                        CooperadoSession.getFuncionarioId(),
                        new RateiController.ListaCallback() {
                            @Override
                            public void onSuccess(List<RateiListAdapter.Item> resultado) {
                                if (!vivo()) return;
                                itens = resultado;
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
        List<RateiListAdapter.Item> ordenados = new ArrayList<>(itens);
        ordenados.sort(
                (a, b) -> {
                    if (a.dataRateioIso == null || b.dataRateioIso == null) return 0;
                    return maisAntigasPrimeiro
                            ? a.dataRateioIso.compareTo(b.dataRateioIso)
                            : b.dataRateioIso.compareTo(a.dataRateioIso);
                });

        txtContadorRateios.setText(
                ordenados.size() + (ordenados.size() == 1 ? " encontrado" : " encontrados"));
        boolean vazio = ordenados.isEmpty();
        txtSemRateios.setVisibility(vazio ? View.VISIBLE : View.GONE);
        recyclerRateios.setVisibility(vazio ? View.GONE : View.VISIBLE);
        adapter.submitList(ordenados);
    }
}
