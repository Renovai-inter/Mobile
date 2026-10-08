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
import com.example.renovai.CooperadoPreload;
import com.example.renovai.CooperadoSession;
import com.example.renovai.CooperadoUi;
import com.example.renovai.DetalheDialogHelper;
import com.example.renovai.R;
import com.example.renovai.adapter.ColetaListAdapter;
import com.example.renovai.adapter.TriagemListAdapter;
import com.example.renovai.controller.ColetaController;
import com.example.renovai.controller.TriagemController;
import com.example.renovai.dto.response.ColetaResponse;
import com.example.renovai.dto.response.TriagemResponse;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tela 2.1 — Home do Cooperado. Todos os dados vêm de endpoints que já existem (ou foram criados
 * seguindo exatamente o padrão do backend — ver IMPLEMENTACAO.md): GET /coletas/por-cooperado/{id}
 * e GET /triagens/por-cooperado/{id}.
 *
 * <p>Os buckets do KPI (Pendentes / Em andamento / Concluídos do mês) são uma heurística deste app
 * — a API não expõe esses três buckets prontos (ver comentário em processarKpis()).
 */
public class CooperadoHomeActivity extends AppCompatActivity {

    private TextView txtKpiPendentes, txtKpiPendentesDetalhe;
    private TextView txtKpiAndamento, txtKpiAndamentoDetalhe;
    private TextView txtKpiConcluidos, txtKpiConcluidosDetalhe;
    private TextView txtSemColetas, txtSemTriagens;
    private RecyclerView recyclerColetasRecentes, recyclerTriagensRecentes;
    private ColetaListAdapter coletaAdapter;
    private TriagemListAdapter triagemAdapter;

    private List<ColetaResponse> coletasCarregadas;
    private List<TriagemResponse> triagensCarregadas;
    private boolean coletasProntas = false;
    private boolean triagensProntas = false;

    private void carregarDadosDoCache() {
        if (isFinishing() || isDestroyed()) return;
        if (CooperadoPreload.getUltimoErro() != null)
            Toast.makeText(this, CooperadoPreload.getUltimoErro(), Toast.LENGTH_LONG).show();
        coletasCarregadas = CooperadoPreload.getColetas();
        triagensCarregadas = CooperadoPreload.getTriagens();

        coletasProntas = true;
        triagensProntas = true;

        tentarProcessar();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cooperado_home);

        if (!CooperadoSession.temCooperadoResolvido()) {
            redirecionarParaLogin();
            return;
        }

        configurarHeader();
        configurarKpis();
        configurarListas();
        configurarAcoes();

        BottomNavigationView nav = findViewById(R.id.bottomNavigationCooperado);
        CooperadoBottomNav.configurar(this, nav, CooperadoBottomNav.Aba.HOME);
    }

    private void carregarDadosDoPreload() {
        coletasCarregadas = CooperadoPreload.getColetas();
        triagensCarregadas = CooperadoPreload.getTriagens();

        coletasProntas = true;
        triagensProntas = true;

        tentarProcessar();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isFinishing() || !CooperadoSession.temCooperadoResolvido()) return;
        if (CooperadoPreload.estaValido()) {
            carregarDadosDoCache();
        } else {
            CooperadoPreload.carregar(this::carregarDadosDoCache);
        }
    }

    private void redirecionarParaLogin() {
        Toast.makeText(
                        this,
                        "Não foi possível identificar o cooperado. Faça login novamente.",
                        Toast.LENGTH_LONG)
                .show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void configurarHeader() {
        TextView txtNomeCooperativaHeader = findViewById(R.id.txtNomeCooperativaHeader);
        TextView txtIniciaisCooperado = findViewById(R.id.txtIniciaisCooperado);

        String cooperativa = CooperadoSession.getCooperativaNome();
        txtNomeCooperativaHeader.setText(cooperativa != null ? cooperativa : "Nome Cooperativa");
        txtIniciaisCooperado.setText(CooperadoUi.gerarIniciais(CooperadoSession.getUsuarioNome()));

        findViewById(R.id.btnPerfilHeader)
                .setOnClickListener(
                        v -> startActivity(new Intent(this, PerfilCooperadoActivity.class)));
    }

    private void configurarKpis() {
        txtKpiPendentes = findViewById(R.id.txtKpiPendentes);
        txtKpiPendentesDetalhe = findViewById(R.id.txtKpiPendentesDetalhe);
        txtKpiAndamento = findViewById(R.id.txtKpiAndamento);
        txtKpiAndamentoDetalhe = findViewById(R.id.txtKpiAndamentoDetalhe);
        txtKpiConcluidos = findViewById(R.id.txtKpiConcluidos);
        txtKpiConcluidosDetalhe = findViewById(R.id.txtKpiConcluidosDetalhe);
        txtSemColetas = findViewById(R.id.txtSemColetas);
        txtSemTriagens = findViewById(R.id.txtSemTriagens);
    }

    private void configurarListas() {
        recyclerColetasRecentes = findViewById(R.id.recyclerColetasRecentes);
        recyclerColetasRecentes.setLayoutManager(new LinearLayoutManager(this));
        coletaAdapter = new ColetaListAdapter(this::mostrarDetalheColeta);
        recyclerColetasRecentes.setAdapter(coletaAdapter);

        recyclerTriagensRecentes = findViewById(R.id.recyclerTriagensRecentes);
        recyclerTriagensRecentes.setLayoutManager(new LinearLayoutManager(this));
        triagemAdapter =
                new TriagemListAdapter(
                        new TriagemListAdapter.OnTriagemClickListener() {
                            @Override
                            public void onDetalhesClick(TriagemListAdapter.Grupo grupo) {
                                mostrarDetalheTriagem(grupo);
                            }

                            @Override
                            public void onContinuarClick(TriagemListAdapter.Grupo grupo) {
                                abrirCompletarTriagem(grupo);
                            }
                        });
        recyclerTriagensRecentes.setAdapter(triagemAdapter);
    }

    private void configurarAcoes() {
        findViewById(R.id.txtVerTodasColetas)
                .setOnClickListener(
                        v -> startActivity(new Intent(this, ListagemColetasActivity.class)));
        findViewById(R.id.txtVerTodasTriagens)
                .setOnClickListener(
                        v -> startActivity(new Intent(this, ListagemTriagensActivity.class)));

        FloatingActionButton fab = findViewById(R.id.fabNovaColeta);
        fab.setOnClickListener(v -> startActivity(new Intent(this, AdicionarColetaActivity.class)));
    }

    private void carregarDados() {
        coletasProntas = false;
        triagensProntas = false;
        String cooperadoId = CooperadoSession.getFuncionarioId();

        new ColetaController()
                .listarPorCooperado(
                        cooperadoId,
                        new ColetaController.ListaCallback() {
                            @Override
                            public void onSuccess(List<ColetaResponse> coletas) {
                                coletasCarregadas = coletas;
                                coletasProntas = true;
                                tentarProcessar();
                            }

                            @Override
                            public void onErro(String mensagem) {
                                coletasCarregadas = new ArrayList<>();
                                coletasProntas = true;
                                Toast.makeText(
                                                CooperadoHomeActivity.this,
                                                mensagem,
                                                Toast.LENGTH_SHORT)
                                        .show();
                                tentarProcessar();
                            }
                        });

        new TriagemController()
                .listarPorCooperado(
                        cooperadoId,
                        new TriagemController.ListaCallback() {
                            @Override
                            public void onSuccess(List<TriagemResponse> triagens) {
                                triagensCarregadas = triagens;
                                triagensProntas = true;
                                tentarProcessar();
                            }

                            @Override
                            public void onErro(String mensagem) {
                                triagensCarregadas = new ArrayList<>();
                                triagensProntas = true;
                                tentarProcessar();
                            }
                        });
    }

    private void tentarProcessar() {
        if (!coletasProntas || !triagensProntas || isFinishing()) return;
        List<TriagemListAdapter.Grupo> grupos =
                TriagemController.agruparPorColeta(triagensCarregadas);
        processarKpis(grupos);
        processarRecentes(grupos);
    }

    /**
     * Heurística de classificação (a API não tem esses 3 buckets prontos): - Pendentes: coleta
     * "Pendente"/"Aberta" + triagem-grupo PENDENTE. - Em andamento: coleta "Agendada" +
     * triagem-grupo EM_ANDAMENTO. - Concluídos do mês: coleta "Concluída" com dataColeta no mês
     * atual + triagem-grupo CONCLUIDA com data no mês atual.
     */
    private void processarKpis(List<TriagemListAdapter.Grupo> grupos) {
        int coletasPendentes = 0, coletasAndamento = 0, coletasConcluidasMes = 0;
        LocalDateTime agora = LocalDateTime.now();

        for (ColetaResponse c : coletasCarregadas) {
            String status =
                    c.getStatusAtual() != null ? c.getStatusAtual().toUpperCase(Locale.ROOT) : "";
            if (status.contains("AGEND")) {
                coletasAndamento++;
            } else if (status.contains("CONCLU") || status.contains("ESTOQUE")) {
                if (dataNoMesAtual(c.getDataColeta(), agora)) coletasConcluidasMes++;
            } else if (!status.contains("CANCEL")) {
                coletasPendentes++;
            }
        }

        int triagensPendentes = 0, triagensAndamento = 0, triagensConcluidasMes = 0;
        for (TriagemListAdapter.Grupo g : grupos) {
            switch (g.status) {
                case PENDENTE:
                    triagensPendentes++;
                    break;
                case EM_ANDAMENTO:
                    triagensAndamento++;
                    break;
                case CONCLUIDA:
                    if (dataNoMesAtual(g.dataConclusaoIso, agora)) triagensConcluidasMes++;
                    break;
            }
        }

        txtKpiPendentes.setText(dois(coletasPendentes + triagensPendentes));
        txtKpiPendentesDetalhe.setText(
                dois(coletasPendentes) + " coletas\n" + dois(triagensPendentes) + " triagens");

        txtKpiAndamento.setText(dois(coletasAndamento + triagensAndamento));
        txtKpiAndamentoDetalhe.setText(
                dois(coletasAndamento) + " coletas\n" + dois(triagensAndamento) + " triagens");

        txtKpiConcluidos.setText(dois(coletasConcluidasMes + triagensConcluidasMes));
        txtKpiConcluidosDetalhe.setText(
                dois(coletasConcluidasMes)
                        + " coletas\n"
                        + dois(triagensConcluidasMes)
                        + " triagens");
    }

    private void processarRecentes(List<TriagemListAdapter.Grupo> grupos) {
        List<ColetaResponse> coletasOrdenadas = new ArrayList<>(coletasCarregadas);
        coletasOrdenadas.sort(
                (a, b) -> {
                    if (a.getDataColeta() == null || b.getDataColeta() == null) return 0;
                    return b.getDataColeta().compareTo(a.getDataColeta());
                });
        boolean semColetas = coletasOrdenadas.isEmpty();
        txtSemColetas.setVisibility(semColetas ? View.VISIBLE : View.GONE);
        recyclerColetasRecentes.setVisibility(semColetas ? View.GONE : View.VISIBLE);
        coletaAdapter.submitList(coletasOrdenadas.subList(0, Math.min(3, coletasOrdenadas.size())));

        List<TriagemListAdapter.Grupo> gruposOrdenados = new ArrayList<>(grupos);
        gruposOrdenados.sort(
                (a, b) -> {
                    String da = a.dataConclusaoIso, db = b.dataConclusaoIso;
                    if (da == null && db == null) return 0;
                    if (da == null) return -1;
                    if (db == null) return 1;
                    return db.compareTo(da);
                });
        boolean semTriagens = gruposOrdenados.isEmpty();
        txtSemTriagens.setVisibility(semTriagens ? View.VISIBLE : View.GONE);
        recyclerTriagensRecentes.setVisibility(semTriagens ? View.GONE : View.VISIBLE);
        triagemAdapter.submitList(gruposOrdenados.subList(0, Math.min(3, gruposOrdenados.size())));
    }

    private boolean dataNoMesAtual(String dataIso, LocalDateTime agora) {
        if (dataIso == null) return false;
        try {
            LocalDateTime data =
                    LocalDateTime.parse(dataIso.length() > 19 ? dataIso.substring(0, 19) : dataIso);
            return data.getYear() == agora.getYear()
                    && data.getMonthValue() == agora.getMonthValue();
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private String dois(int numero) {
        return String.format(Locale.ROOT, "%02d", numero);
    }

    private void mostrarDetalheColeta(ColetaResponse coleta) {
        double kg = coleta.getQuantidadeKg() != null ? coleta.getQuantidadeKg().doubleValue() : 0;
        DetalheDialogHelper.mostrar(
                this,
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
                });
    }

    private void mostrarDetalheTriagem(TriagemListAdapter.Grupo grupo) {
        double kg = grupo.pesoAtualKg != null ? grupo.pesoAtualKg.doubleValue() : 0;
        DetalheDialogHelper.mostrar(
                this,
                "Detalhes da triagem",
                new String[][] {
                    {"COLETA VINCULADA", grupo.coletaId},
                    {"PESO", formatarKg(kg) + " kg"},
                    {"PROGRESSO", grupo.progressoPercentual + "%"},
                    {"STATUS", grupo.status.name()},
                    {"DATA", formatarData(grupo.dataConclusaoIso)},
                });
    }

    private void abrirCompletarTriagem(TriagemListAdapter.Grupo grupo) {
        Intent intent = new Intent(this, CompletarTriagemActivity.class);
        intent.putExtra(CompletarTriagemActivity.EXTRA_COLETA_ID, grupo.coletaId);
        startActivity(intent);
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
