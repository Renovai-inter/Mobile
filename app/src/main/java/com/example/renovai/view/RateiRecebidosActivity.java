package com.example.renovai.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renovai.CooperadoBottomNav;
import com.example.renovai.CooperadoSession;
import com.example.renovai.CooperadoUi;
import com.example.renovai.R;
import com.example.renovai.adapter.RateiListAdapter;
import com.example.renovai.controller.RateiController;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

/**
 * Tela 2.5 — Rateios Recebidos.
 *
 * <p>A API não tem GET /rateios/por-cooperado — ver RateiController (mobile) para como a lista é
 * montada (por-cooperativa + distribuição de cada rateio) e IMPLEMENTACAO.md para a heurística de
 * status (Fechado/Pendente/Agendada), que também não existe como campo pronto na API.
 */
public class RateiRecebidosActivity extends AppCompatActivity {

    private TextView txtContadorRateios, txtOrdenarRateios, txtSemRateios;
    private RecyclerView recyclerRateios;
    private RateiListAdapter adapter;
    private List<RateiListAdapter.Item> itens = new ArrayList<>();
    private boolean maisAntigasPrimeiro = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rateios);

        if (!CooperadoSession.temCooperadoResolvido()) {
            Toast.makeText(
                            this,
                            "Não foi possível identificar o cooperado. Faça login novamente.",
                            Toast.LENGTH_LONG)
                    .show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        TextView txtNomeCooperativaHeader = findViewById(R.id.txtNomeCooperativaHeader);
        TextView txtIniciaisCooperado = findViewById(R.id.txtIniciaisCooperado);
        String cooperativa = CooperadoSession.getCooperativaNome();
        txtNomeCooperativaHeader.setText(cooperativa != null ? cooperativa : "Nome Cooperativa");
        txtIniciaisCooperado.setText(CooperadoUi.gerarIniciais(CooperadoSession.getUsuarioNome()));
        findViewById(R.id.btnPerfilHeader)
                .setOnClickListener(
                        v -> startActivity(new Intent(this, PerfilCooperadoActivity.class)));

        txtContadorRateios = findViewById(R.id.txtContadorRateios);
        txtOrdenarRateios = findViewById(R.id.txtOrdenarRateios);
        txtSemRateios = findViewById(R.id.txtSemRateios);

        recyclerRateios = findViewById(R.id.recyclerRateios);
        recyclerRateios.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RateiListAdapter();
        recyclerRateios.setAdapter(adapter);

        txtOrdenarRateios.setOnClickListener(
                v -> {
                    maisAntigasPrimeiro = !maisAntigasPrimeiro;
                    txtOrdenarRateios.setText(
                            maisAntigasPrimeiro ? "Mais antigas" : "Mais recentes");
                    atualizarLista();
                });

        BottomNavigationView nav = findViewById(R.id.bottomNavigationCooperado);
        CooperadoBottomNav.configurar(this, nav, CooperadoBottomNav.Aba.RATEIOS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isFinishing() || !CooperadoSession.temCooperadoResolvido()) return;
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
                                if (isFinishing() || isDestroyed()) return;
                                itens = resultado;
                                atualizarLista();
                            }

                            @Override
                            public void onErro(String mensagem) {
                                if (isFinishing() || isDestroyed()) return;
                                Toast.makeText(
                                                RateiRecebidosActivity.this,
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
