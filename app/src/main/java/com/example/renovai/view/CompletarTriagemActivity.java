package com.example.renovai.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.renovai.CooperadoSession;
import com.example.renovai.CooperadoUi;
import com.example.renovai.R;
import com.example.renovai.controller.TriagemController;
import com.example.renovai.dto.response.TriagemResponse;
import com.google.android.material.button.MaterialButton;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

/**
 * Tela 2.3 — Completar Triagem.
 *
 * <p>Recebe o coletaId (não um triagemId) porque uma "triagem", nessa API, é UMA LINHA POR MATERIAL
 * (ver TriagemResponse) — uma coleta com N materiais gera N registros de Triagem, todos com o mesmo
 * coletaId. Esta tela busca todos eles com GET /triagens/por-coleta/{coletaId} e mostra um por
 * material.
 */
public class CompletarTriagemActivity extends AppCompatActivity {

    public static final String EXTRA_COLETA_ID = "coletaId";

    private TextView txtIdColetaVinculada,
            txtQuantidadeMateriais,
            txtContadorEquipe,
            txtProgressoGeralPercent,
            txtKgSeparados,
            txtKgTotal,
            txtTriagemCompleta;
    private android.widget.ImageView imgFotoColetaVinculada;
    private LinearLayout listaEquipe, listaMateriaisTriagem;
    private ProgressBar progressGeralTriagem, progressCompletarTriagem;
    private MaterialButton btnSepararMaterial;

    private String coletaId;
    private List<TriagemResponse> materiais;
    private boolean salvando;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completar_triagem);
        com.example.renovai.TelaInsets.conteudo(this, false);

        if (!CooperadoSession.temCooperadoResolvido()) {
            Toast.makeText(
                            this,
                            "Não foi possível identificar o cooperado. Faça login novamente.",
                            Toast.LENGTH_LONG)
                    .show();
            finish();
            return;
        }

        coletaId = getIntent().getStringExtra(EXTRA_COLETA_ID);
        if (coletaId == null) {
            Toast.makeText(this, "Coleta não informada.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        findViewById(R.id.btnVoltarCompletarTriagem).setOnClickListener(v -> finish());
        bindViews();

        String idCurto =
                coletaId.length() >= 6
                        ? coletaId.substring(0, 6).toUpperCase(Locale.ROOT)
                        : coletaId;
        txtIdColetaVinculada.setText("ID: " + idCurto);

        btnSepararMaterial.setOnClickListener(v -> abrirDialogProximoMaterial());

        carregarMateriais();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (materiais != null) carregarMateriais();
    }

    private void bindViews() {
        txtIdColetaVinculada = findViewById(R.id.txtIdColetaVinculada);
        txtQuantidadeMateriais = findViewById(R.id.txtQuantidadeMateriais);
        imgFotoColetaVinculada = findViewById(R.id.imgFotoColetaVinculada);
        txtContadorEquipe = findViewById(R.id.txtContadorEquipe);
        listaEquipe = findViewById(R.id.listaEquipe);
        listaMateriaisTriagem = findViewById(R.id.listaMateriaisTriagem);
        txtProgressoGeralPercent = findViewById(R.id.txtProgressoGeralPercent);
        progressGeralTriagem = findViewById(R.id.progressGeralTriagem);
        txtKgSeparados = findViewById(R.id.txtKgSeparados);
        txtKgTotal = findViewById(R.id.txtKgTotal);
        btnSepararMaterial = findViewById(R.id.btnSepararMaterial);
        txtTriagemCompleta = findViewById(R.id.txtTriagemCompleta);
        progressCompletarTriagem = findViewById(R.id.progressCompletarTriagem);
    }

    private void carregarMateriais() {
        new TriagemController()
                .listarPorColeta(
                        coletaId,
                        new TriagemController.ListaCallback() {
                            @Override
                            public void onSuccess(List<TriagemResponse> triagens) {
                                if (isFinishing() || isDestroyed()) return;
                                materiais = triagens;
                                preencherTela();
                            }

                            @Override
                            public void onErro(String mensagem) {
                                if (isFinishing() || isDestroyed()) return;
                                Toast.makeText(
                                                CompletarTriagemActivity.this,
                                                mensagem,
                                                Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
    }

    private void preencherTela() {
        txtQuantidadeMateriais.setText("Quantidade de materiais: " + materiais.size());

        if (!materiais.isEmpty()
                && materiais.get(0).getImagemUrl() != null
                && !materiais.get(0).getImagemUrl().trim().isEmpty()) {
            Glide.with(this).load(materiais.get(0).getImagemUrl()).into(imgFotoColetaVinculada);
        }

        preencherEquipe();
        preencherMateriais();
        preencherProgresso();
    }

    private void preencherEquipe() {
        listaEquipe.removeAllViews();
        List<String> nomes = !materiais.isEmpty() ? materiais.get(0).getCooperadosNomes() : null;
        if (nomes == null) nomes = new java.util.ArrayList<>();

        txtContadorEquipe.setText(String.valueOf(nomes.size()));
        String meuNome = CooperadoSession.getUsuarioNome();

        for (String nome : nomes) {
            View linha =
                    LayoutInflater.from(this)
                            .inflate(R.layout.item_equipe_membro, listaEquipe, false);
            TextView txtIniciais = linha.findViewById(R.id.txtIniciaisMembro);
            TextView txtNome = linha.findViewById(R.id.txtNomeMembro);
            txtIniciais.setText(CooperadoUi.gerarIniciais(nome));
            boolean voce = meuNome != null && meuNome.equalsIgnoreCase(nome);
            txtNome.setText(voce ? nome + " (Você)" : nome);
            listaEquipe.addView(linha);
        }
    }

    private void preencherMateriais() {
        listaMateriaisTriagem.removeAllViews();
        for (TriagemResponse material : materiais) {
            View linha =
                    LayoutInflater.from(this)
                            .inflate(R.layout.item_material_triagem, listaMateriaisTriagem, false);
            TextView txtNome = linha.findViewById(R.id.txtNomeMaterialTriagem);
            TextView txtStatus = linha.findViewById(R.id.txtStatusMaterialTriagem);
            TextView txtKg = linha.findViewById(R.id.txtKgMaterialTriagem);

            txtNome.setText(
                    material.getMaterialCategoria() != null
                            ? material.getMaterialCategoria()
                            : "Material");

            double kg = TriagemController.pesoReal(material.getQuantidadeKg()).doubleValue();
            EstadoMaterial estado = classificarMaterial(material);
            txtStatus.setText(estado.rotulo);
            txtStatus.setTextColor(estado.cor);
            txtKg.setText(formatarKg(kg) + " kg");
            txtKg.setTextColor(estado.cor);

            linha.setOnClickListener(v -> abrirDialogSeparar(material));
            listaMateriaisTriagem.addView(linha);
        }
    }

    private void preencherProgresso() {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal separado = BigDecimal.ZERO;
        boolean todosConcluidos = !materiais.isEmpty();

        for (TriagemResponse m : materiais) {
            BigDecimal kg = TriagemController.pesoReal(m);
            total = total.add(kg);
            boolean concluido =
                    m.getStatusAtual() != null
                            && m.getStatusAtual().toUpperCase(Locale.ROOT).contains("CONCLU");
            if (concluido) {
                separado = separado.add(kg);
            } else {
                todosConcluidos = false;
            }
        }

        int percentual =
                total.compareTo(BigDecimal.ZERO) > 0
                        ? separado.multiply(BigDecimal.valueOf(100))
                                .divide(total, 0, RoundingMode.HALF_UP)
                                .intValue()
                        : 0;

        txtProgressoGeralPercent.setText(percentual + "%");
        progressGeralTriagem.setProgress(percentual);
        txtKgSeparados.setText(formatarKg(separado.doubleValue()) + " kg separados");
        txtKgTotal.setText(formatarKg(total.doubleValue()) + " kg total");

        btnSepararMaterial.setVisibility(todosConcluidos ? View.GONE : View.VISIBLE);
        txtTriagemCompleta.setVisibility(todosConcluidos ? View.VISIBLE : View.GONE);
    }

    private void abrirDialogProximoMaterial() {
        for (TriagemResponse material : materiais) {
            EstadoMaterial estado = classificarMaterial(material);
            if (estado != EstadoMaterial.SEPARADO) {
                abrirDialogSeparar(material);
                return;
            }
        }
        Toast.makeText(this, "Todos os materiais já foram separados.", Toast.LENGTH_SHORT).show();
    }

    private void abrirDialogSeparar(TriagemResponse material) {
        if (salvando) return;
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_separar_material, null);
        TextView txtNomeMaterialDialog = view.findViewById(R.id.txtNomeMaterialDialog);
        EditText edtPesoSeparado = view.findViewById(R.id.edtPesoSeparado);
        EditText edtPesoRejeitado = view.findViewById(R.id.edtPesoRejeitado);

        txtNomeMaterialDialog.setText(material.getMaterialCategoria());
        if (!TriagemController.semPeso(material.getQuantidadeKg())) {
            edtPesoSeparado.setText(
                    material.getQuantidadeKg().stripTrailingZeros().toPlainString());
        }
        if (!TriagemController.semPeso(material.getQuantidadeKg())
                && material.getQuantidadeRejeitoKg() != null
                && material.getQuantidadeRejeitoKg().compareTo(BigDecimal.ZERO) > 0) {
            edtPesoRejeitado.setText(
                    material.getQuantidadeRejeitoKg().stripTrailingZeros().toPlainString());
        }

        AlertDialog dialogo =
                new AlertDialog.Builder(this)
                        .setTitle("Separar material")
                        .setView(view)
                        .setPositiveButton("Concluir este material", null)
                        .setNeutralButton("Salvar progresso", null)
                        .setNegativeButton("Cancelar", null)
                        .show();
        for (int botao : new int[] {AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEUTRAL}) {
            dialogo.getButton(botao)
                    .setOnClickListener(
                            v -> {
                                BigDecimal peso = lerPeso(edtPesoSeparado);
                                BigDecimal rejeito =
                                        edtPesoRejeitado.getText().toString().trim().isEmpty()
                                                ? BigDecimal.ZERO
                                                : lerPeso(edtPesoRejeitado);
                                String erro = TriagemController.validarPesos(peso, rejeito);
                                if (erro != null) {
                                    Toast.makeText(this, erro, Toast.LENGTH_LONG).show();
                                    return;
                                }
                                dialogo.dismiss();
                                if (botao == AlertDialog.BUTTON_POSITIVE)
                                    concluirMaterial(material, peso, rejeito);
                                else salvarProgressoMaterial(material, peso, rejeito);
                            });
        }
    }

    private BigDecimal lerPeso(EditText edt) {
        String texto = edt.getText().toString().trim().replace(",", ".");
        if (texto.isEmpty()) return null;
        try {
            return new BigDecimal(texto);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void salvarProgressoMaterial(
            TriagemResponse material, BigDecimal peso, BigDecimal rejeito) {
        if (salvando) return;
        if (peso == null) {
            Toast.makeText(this, "Informe o peso separado.", Toast.LENGTH_SHORT).show();
            return;
        }
        progressCompletarTriagem.setVisibility(View.VISIBLE);
        salvando = true;
        btnSepararMaterial.setEnabled(false);
        new TriagemController()
                .salvarProgresso(
                        material.getTriagemId(),
                        material.getEquipeId(),
                        material.getColetaId(),
                        material.getMaterialId(),
                        peso,
                        rejeito,
                        material.getImagemUrl(),
                        new TriagemController.SalvarCallback() {
                            @Override
                            public void onSuccess(TriagemResponse triagem) {
                                salvando = false;
                                if (isFinishing() || isDestroyed()) return;
                                btnSepararMaterial.setEnabled(true);
                                progressCompletarTriagem.setVisibility(View.GONE);
                                Toast.makeText(
                                                CompletarTriagemActivity.this,
                                                "Progresso salvo.",
                                                Toast.LENGTH_SHORT)
                                        .show();
                                carregarMateriais();
                            }

                            @Override
                            public void onErro(String mensagem) {
                                salvando = false;
                                if (isFinishing() || isDestroyed()) return;
                                btnSepararMaterial.setEnabled(true);
                                progressCompletarTriagem.setVisibility(View.GONE);
                                Toast.makeText(
                                                CompletarTriagemActivity.this,
                                                mensagem,
                                                Toast.LENGTH_LONG)
                                        .show();
                            }
                        });
    }

    private void concluirMaterial(
            TriagemResponse material, BigDecimal quantidadeFinalKg, BigDecimal rejeito) {
        if (salvando) return;
        salvando = true;
        btnSepararMaterial.setEnabled(false);
        progressCompletarTriagem.setVisibility(View.VISIBLE);
        TriagemController controller = new TriagemController();
        TriagemController.SalvarCallback resultado =
                new TriagemController.SalvarCallback() {
                    @Override
                    public void onSuccess(TriagemResponse triagem) {
                        salvando = false;
                        if (isFinishing() || isDestroyed()) return;
                        btnSepararMaterial.setEnabled(true);
                        progressCompletarTriagem.setVisibility(View.GONE);
                        Toast.makeText(
                                        CompletarTriagemActivity.this,
                                        "Material marcado como separado.",
                                        Toast.LENGTH_SHORT)
                                .show();
                        carregarMateriais();
                    }

                    @Override
                    public void onErro(String mensagem) {
                        salvando = false;
                        if (isFinishing() || isDestroyed()) return;
                        btnSepararMaterial.setEnabled(true);
                        progressCompletarTriagem.setVisibility(View.GONE);
                        Toast.makeText(CompletarTriagemActivity.this, mensagem, Toast.LENGTH_LONG)
                                .show();
                    }
                };
        controller.salvarProgresso(
                material.getTriagemId(),
                material.getEquipeId(),
                material.getColetaId(),
                material.getMaterialId(),
                quantidadeFinalKg,
                rejeito,
                material.getImagemUrl(),
                new TriagemController.SalvarCallback() {
                    public void onSuccess(TriagemResponse atualizada) {
                        controller.concluir(
                                material.getTriagemId(), quantidadeFinalKg, null, resultado);
                    }

                    public void onErro(String m) {
                        resultado.onErro(m);
                    }
                });
    }

    private enum EstadoMaterial {
        NAO_SEPARADO("Não Separado", 0xFFC2185B),
        EM_SEPARACAO("Em Separação", 0xFFC2185B),
        SEPARADO("Separado", 0xFF2E7D32);

        final String rotulo;
        final int cor;

        EstadoMaterial(String rotulo, int cor) {
            this.rotulo = rotulo;
            this.cor = cor;
        }
    }

    /**
     * Heurística (a API não devolve um enum fixo de status — ver statusAtual em TriagemResponse):
     * sem peso ainda (ou só o peso marcador de 0,001 kg criado pelo gestor) = Não Separado; status
     * contendo "CONCLU" = Separado; qualquer outra coisa com peso > 0 = Em Separação.
     */
    private EstadoMaterial classificarMaterial(TriagemResponse material) {
        boolean concluido =
                material.getStatusAtual() != null
                        && material.getStatusAtual().toUpperCase(Locale.ROOT).contains("CONCLU");
        if (concluido) return EstadoMaterial.SEPARADO;
        if (TriagemController.semPeso(material.getQuantidadeKg()))
            return EstadoMaterial.NAO_SEPARADO;
        return EstadoMaterial.EM_SEPARACAO;
    }

    private String formatarKg(double kg) {
        if (kg == Math.floor(kg)) return String.valueOf((long) kg);
        return String.format(Locale.getDefault(), "%.1f", kg);
    }
}
