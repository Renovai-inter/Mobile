package com.example.renovai.view;


import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.renovai.ApiClient;
import com.example.renovai.CooperadoSession;
import com.example.renovai.GestorUi;
import com.example.renovai.MaterialApiService;
import com.example.renovai.R;
import com.example.renovai.RotaApiService;
import com.example.renovai.controller.ColetaController;
import com.example.renovai.dto.request.ColetaRequest;
import com.example.renovai.dto.response.ColetaResponse;
import com.example.renovai.dto.response.MaterialResponse;
import com.example.renovai.dto.response.RotaResponse;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Tela 2.2 — Registrar Coleta.
 *
 * <p>POST /coletas leva peso, tipo (e rota, se externa), foto (Cloudinary), se precisa de triagem
 * e os materiais com o peso de cada um. Com materiais, a API exige a equipe do cooperado e que a
 * soma dos pesos seja o peso da coleta; sem triagem, os materiais entram direto no estoque.
 * Descrição e data prevista da triagem não existem no banco da API: depois que a coleta é criada,
 * vão para o Firestore (ColetaDetalhes).
 */
public class AdicionarColetaActivity extends AppCompatActivity {

    /** Um material escolhido para a coleta e o peso dele. */
    private static final class Item {
        final MaterialResponse material;
        BigDecimal kg;

        Item(MaterialResponse material, BigDecimal kg) {
            this.material = material;
            this.kg = kg;
        }
    }

    private EditText edtPesoColeta, edtDescricaoColeta, edtDataPrevistaTriagem, edtPesoMaterial;
    private TextView btnTipoInterno, btnTipoExterno, txtSemFotoColeta, btnAdicionarOutroMaterial;
    private TextView txtDicaTriagem, txtSomaMateriais;
    private android.widget.ImageView imgPreviewFotoColeta;
    private LinearLayout containerDataPrevista,
            containerRotaOrigem,
            containerNovoMaterial,
            containerMateriaisColeta,
            listaMateriaisColeta;
    private Spinner spinnerRotaOrigem, spinnerCategoriaMaterial;
    private Switch switchNecessitaTriagem;
    private MaterialButton btnSalvarMaterial, btnRegistrarColeta;
    private ProgressBar progressRegistrarColeta;

    private boolean tipoExterno = false, registrando;
    private Uri fotoSelecionada;
    // URL do Cloudinary da foto já enviada: se a API falhar, o "Registrar" de novo não sobe a foto outra vez.
    private String fotoUrlEnviada;
    private LocalDate dataPrevista;
    private String equipeId;
    private final List<MaterialResponse> materiaisDaCooperativa = new ArrayList<>();
    private final List<Item> itens = new ArrayList<>();
    private final List<RotaResponse> rotasCarregadas = new ArrayList<>();

    private final ActivityResultLauncher<String> seletorImagem =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri == null) return;
                        fotoSelecionada = uri;
                        fotoUrlEnviada = null;
                        imgPreviewFotoColeta.setImageURI(uri);
                        imgPreviewFotoColeta.setVisibility(View.VISIBLE);
                        txtSemFotoColeta.setVisibility(View.GONE);
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adicionar_coleta);
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

        findViewById(R.id.btnVoltarAdicionarColeta).setOnClickListener(v -> finish());

        bindViews();
        configurarTipoColeta();
        configurarTriagem();
        configurarDataPrevista();
        configurarFoto();
        configurarMateriais();
        btnRegistrarColeta.setOnClickListener(v -> registrarColeta());

        carregarMateriais();
        // a equipe só é exigida quando a coleta leva materiais
        new ColetaController().equipeDoCooperado(CooperadoSession.getFuncionarioId(), id -> equipeId = id);

        boolean fotoDisponivel = com.example.renovai.CloudinaryUpload.estaConfigurado();
        findViewById(R.id.btnAnexarFoto).setEnabled(fotoDisponivel);
        txtSemFotoColeta.setText(
                fotoDisponivel ? "Nenhuma foto anexada" : "Envio de foto não configurado");
    }

    private void bindViews() {
        edtPesoColeta = findViewById(R.id.edtPesoColeta);
        edtDescricaoColeta = findViewById(R.id.edtDescricaoColeta);
        edtDataPrevistaTriagem = findViewById(R.id.edtDataPrevistaTriagem);
        edtPesoMaterial = findViewById(R.id.edtPesoMaterial);
        btnTipoInterno = findViewById(R.id.btnTipoInterno);
        btnTipoExterno = findViewById(R.id.btnTipoExterno);
        imgPreviewFotoColeta = findViewById(R.id.imgPreviewFotoColeta);
        txtSemFotoColeta = findViewById(R.id.txtSemFotoColeta);
        txtDicaTriagem = findViewById(R.id.txtDicaTriagem);
        txtSomaMateriais = findViewById(R.id.txtSomaMateriais);
        containerDataPrevista = findViewById(R.id.containerDataPrevista);
        containerRotaOrigem = findViewById(R.id.containerRotaOrigem);
        spinnerRotaOrigem = findViewById(R.id.spinnerRotaOrigem);
        spinnerCategoriaMaterial = findViewById(R.id.spinnerCategoriaMaterial);
        switchNecessitaTriagem = findViewById(R.id.switchNecessitaTriagem);
        containerNovoMaterial = findViewById(R.id.containerNovoMaterial);
        containerMateriaisColeta = findViewById(R.id.containerMateriaisColeta);
        listaMateriaisColeta = findViewById(R.id.listaMateriaisColeta);
        btnAdicionarOutroMaterial = findViewById(R.id.btnAdicionarOutroMaterial);
        btnSalvarMaterial = findViewById(R.id.btnSalvarMaterial);
        btnRegistrarColeta = findViewById(R.id.btnRegistrarColeta);
        progressRegistrarColeta = findViewById(R.id.progressRegistrarColeta);
    }

    private void configurarTipoColeta() {
        atualizarTipoColetaUi();
        btnTipoInterno.setOnClickListener(
                v -> {
                    tipoExterno = false;
                    atualizarTipoColetaUi();
                });
        btnTipoExterno.setOnClickListener(
                v -> {
                    tipoExterno = true;
                    atualizarTipoColetaUi();
                    carregarRotasSeNecessario();
                });
    }

    private void atualizarTipoColetaUi() {
        btnTipoInterno.setBackgroundResource(
                tipoExterno ? 0 : R.drawable.toggle_selected_cooperado_background);
        btnTipoInterno.setTextColor(tipoExterno ? 0xFF555555 : 0xFFFFFFFF);
        btnTipoExterno.setBackgroundResource(
                tipoExterno ? R.drawable.toggle_selected_cooperado_background : 0);
        btnTipoExterno.setTextColor(tipoExterno ? 0xFFFFFFFF : 0xFF555555);
        containerRotaOrigem.setVisibility(tipoExterno ? View.VISIBLE : View.GONE);
    }

    private void configurarTriagem() {
        switchNecessitaTriagem.setOnCheckedChangeListener((b, marcado) -> atualizarTriagemUi());
        atualizarTriagemUi();
    }

    /**
     * Com 2 ou mais materiais a triagem é obrigatória (o switch fica travado ligado). A data prevista
     * só aparece com triagem, e a dica explica o que acontece em cada caso.
     */
    private void atualizarTriagemUi() {
        boolean obrigatoria = itens.size() >= 2;
        if (obrigatoria && !switchNecessitaTriagem.isChecked()) switchNecessitaTriagem.setChecked(true);
        switchNecessitaTriagem.setEnabled(!obrigatoria);
        boolean triagem = switchNecessitaTriagem.isChecked();
        containerDataPrevista.setVisibility(triagem ? View.VISIBLE : View.GONE);
        txtDicaTriagem.setText(
                obrigatoria
                        ? "Obrigatório para 2+ materiais"
                        : triagem
                                ? "Os materiais passam pela triagem antes do estoque"
                                : itens.isEmpty()
                                        ? "Sem triagem, informe abaixo o material e o peso"
                                        : "O material vai direto para o estoque");
    }

    private void configurarDataPrevista() {
        edtDataPrevistaTriagem.setOnClickListener(
                v -> {
                    Calendar agora = Calendar.getInstance();
                    DatePickerDialog dialogo =
                            new DatePickerDialog(
                                    this,
                                    (view, ano, mes, dia) -> {
                                        dataPrevista = LocalDate.of(ano, mes + 1, dia);
                                        edtDataPrevistaTriagem.setText(
                                                String.format(
                                                        Locale.ROOT, "%02d/%02d/%04d", dia, mes + 1, ano));
                                    },
                                    agora.get(Calendar.YEAR),
                                    agora.get(Calendar.MONTH),
                                    agora.get(Calendar.DAY_OF_MONTH));
                    // a triagem não pode ficar prevista para antes de hoje
                    dialogo.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
                    dialogo.show();
                });
    }

    private void configurarFoto() {
        findViewById(R.id.btnAnexarFoto).setOnClickListener(v -> seletorImagem.launch("image/*"));
    }

    private void configurarMateriais() {
        btnSalvarMaterial.setOnClickListener(v -> salvarMaterial());
        btnAdicionarOutroMaterial.setOnClickListener(
                v -> {
                    containerNovoMaterial.setVisibility(View.VISIBLE);
                    btnAdicionarOutroMaterial.setVisibility(View.GONE);
                });
        edtPesoColeta.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void afterTextChanged(Editable s) { atualizarSoma(); }
        });
    }

    private void salvarMaterial() {
        int posicao = spinnerCategoriaMaterial.getSelectedItemPosition();
        if (posicao < 0 || posicao >= materiaisDaCooperativa.size()) {
            toast("Selecione um material.");
            return;
        }
        BigDecimal kg = peso(edtPesoMaterial);
        if (kg == null) {
            toast("Informe o peso deste material.");
            return;
        }
        MaterialResponse escolhido = materiaisDaCooperativa.get(posicao);
        // a API não aceita o mesmo material duas vezes: soma no que já está na lista
        Item existente = null;
        for (Item it : itens)
            if (it.material.getMaterialId().equals(escolhido.getMaterialId())) existente = it;
        if (existente != null) existente.kg = existente.kg.add(kg);
        else itens.add(new Item(escolhido, kg));

        edtPesoMaterial.setText("");
        containerNovoMaterial.setVisibility(View.GONE);
        btnAdicionarOutroMaterial.setVisibility(View.VISIBLE);
        renderMateriais();
    }

    private void renderMateriais() {
        listaMateriaisColeta.removeAllViews();
        for (Item it : itens) {
            LinearLayout linha = new LinearLayout(this);
            linha.setOrientation(LinearLayout.HORIZONTAL);
            linha.setGravity(Gravity.CENTER_VERTICAL);

            TextView nome = new TextView(this);
            nome.setText("•  " + nomeMaterial(it.material) + " — " + GestorUi.kg(it.kg.doubleValue()));
            nome.setTextColor(0xFF222222);
            nome.setTextSize(14f);
            nome.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView remover = new TextView(this);
            remover.setText("Remover");
            remover.setTextColor(getColor(R.color.vinho));
            remover.setTextSize(12f);
            int p = GestorUi.dp(this, 8);
            remover.setPadding(p, p, p, p);
            remover.setOnClickListener(v -> {
                itens.remove(it);
                renderMateriais();
            });

            linha.addView(nome);
            linha.addView(remover);
            listaMateriaisColeta.addView(linha);
        }
        boolean vazio = itens.isEmpty();
        containerMateriaisColeta.setVisibility(vazio ? View.GONE : View.VISIBLE);
        if (vazio) {
            containerNovoMaterial.setVisibility(View.VISIBLE);
            btnAdicionarOutroMaterial.setVisibility(View.GONE);
        }
        atualizarSoma();
        atualizarTriagemUi();
    }

    /** "Soma dos materiais: 8 kg de 10 kg" — verde quando bate com o peso da coleta. */
    private void atualizarSoma() {
        if (itens.isEmpty()) return;
        BigDecimal soma = somaMateriais();
        BigDecimal total = peso(edtPesoColeta);
        boolean bate = total != null && soma.compareTo(total) == 0;
        txtSomaMateriais.setText(
                "Soma dos materiais: " + GestorUi.kg(soma.doubleValue())
                        + (total == null ? "" : " de " + GestorUi.kg(total.doubleValue()) + " da coleta"));
        txtSomaMateriais.setTextColor(bate ? getColor(R.color.verde) : getColor(R.color.vinho));
    }

    private BigDecimal somaMateriais() {
        BigDecimal soma = BigDecimal.ZERO;
        for (Item it : itens) soma = soma.add(it.kg);
        return soma;
    }

    private static String nomeMaterial(MaterialResponse m) {
        return m.getCategoriaNome() != null ? m.getCategoriaNome() : "Material";
    }

    /** Materiais cadastrados da cooperativa do cooperado (GET /materiais), com o id de cada um. */
    private void carregarMateriais() {
        MaterialApiService service = ApiClient.createService(MaterialApiService.class);
        service.listar()
                .enqueue(
                        new Callback<List<MaterialResponse>>() {
                            @Override
                            public void onResponse(
                                    Call<List<MaterialResponse>> call,
                                    Response<List<MaterialResponse>> response) {
                                if (isFinishing() || isDestroyed()) return;
                                materiaisDaCooperativa.clear();
                                if (response.isSuccessful() && response.body() != null) {
                                    String cooperativaId = CooperadoSession.getCooperativaId();
                                    for (MaterialResponse m : response.body()) {
                                        boolean daCooperativa =
                                                m.getCooperativaId() == null
                                                        || (cooperativaId != null
                                                                && cooperativaId.equalsIgnoreCase(
                                                                        m.getCooperativaId()));
                                        if (daCooperativa && m.getMaterialId() != null)
                                            materiaisDaCooperativa.add(m);
                                    }
                                }
                                mostrarMateriais();
                            }

                            @Override
                            public void onFailure(Call<List<MaterialResponse>> call, Throwable t) {
                                if (isFinishing() || isDestroyed()) return;
                                materiaisDaCooperativa.clear();
                                mostrarMateriais();
                            }
                        });
    }

    private void mostrarMateriais() {
        List<String> nomes = new ArrayList<>();
        for (MaterialResponse m : materiaisDaCooperativa) {
            String nome = nomeMaterial(m);
            if (m.getPrecoSugerido() != null)
                nome += " · " + GestorUi.dinheiro(m.getPrecoSugerido().doubleValue()) + "/kg";
            nomes.add(nome);
        }
        boolean tem = !nomes.isEmpty();
        if (!tem) nomes.add("Nenhum material cadastrado na cooperativa");
        spinnerCategoriaMaterial.setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nomes));
        spinnerCategoriaMaterial.setEnabled(tem);
        btnSalvarMaterial.setEnabled(tem);
        edtPesoMaterial.setEnabled(tem);
    }

    private void carregarRotasSeNecessario() {
        if (!rotasCarregadas.isEmpty()) return;
        String cooperativaId = CooperadoSession.getCooperativaId();
        if (cooperativaId == null) return;

        RotaApiService service = ApiClient.createService(RotaApiService.class);
        service.listarAtivasPorCooperativa(cooperativaId)
                .enqueue(
                        new Callback<List<RotaResponse>>() {
                            @Override
                            public void onResponse(
                                    Call<List<RotaResponse>> call,
                                    Response<List<RotaResponse>> response) {
                                if (!response.isSuccessful() || response.body() == null) return;
                                rotasCarregadas.clear();
                                rotasCarregadas.addAll(response.body());
                                List<String> nomes = new ArrayList<>();
                                nomes.add("Escolha a rota de origem dos materiais");
                                for (RotaResponse rota : rotasCarregadas) {
                                    nomes.add(
                                            rota.getNome() != null
                                                    ? rota.getNome()
                                                    : "Rota sem nome");
                                }
                                spinnerRotaOrigem.setAdapter(
                                        new ArrayAdapter<>(
                                                AdicionarColetaActivity.this,
                                                android.R.layout.simple_spinner_dropdown_item,
                                                nomes));
                            }

                            @Override
                            public void onFailure(Call<List<RotaResponse>> call, Throwable t) {
                                // sem rotas carregadas — o botão Registrar vai barrar o envio se
                                // Externo estiver selecionado, pedindo pra tentar de novo
                            }
                        });
    }

    private void registrarColeta() {
        if (registrando) return;
        BigDecimal peso = peso(edtPesoColeta);
        if (peso == null) {
            toast("Informe um peso válido.");
            return;
        }

        String rotaId = null;
        if (tipoExterno) {
            int posicao = spinnerRotaOrigem.getSelectedItemPosition();
            if (posicao <= 0 || posicao > rotasCarregadas.size()) {
                toast("Selecione a rota de origem.");
                return;
            }
            rotaId = rotasCarregadas.get(posicao - 1).getRotaId();
        }

        boolean triagem = switchNecessitaTriagem.isChecked();
        if (!triagem && itens.isEmpty()) {
            toast("Sem triagem, informe o material e o peso em \"Materiais coletados\".");
            return;
        }
        if (!itens.isEmpty()) {
            BigDecimal soma = somaMateriais();
            if (soma.compareTo(peso) != 0) {
                toast("A soma dos materiais (" + GestorUi.kg(soma.doubleValue())
                        + ") precisa ser igual ao peso da coleta (" + GestorUi.kg(peso.doubleValue()) + ").");
                return;
            }
            if (equipeId == null) {
                toast("Você ainda não está em uma equipe. Peça ao gestor para incluir você em uma"
                        + " equipe para registrar os materiais da coleta.");
                return;
            }
        }

        List<ColetaRequest.Material> materiais = new ArrayList<>();
        for (Item it : itens) materiais.add(new ColetaRequest.Material(it.material.getMaterialId(), it.kg));
        String descricao = edtDescricaoColeta.getText().toString();
        LocalDate data = triagem ? dataPrevista : null;

        registrando = true;
        progressRegistrarColeta.setVisibility(View.VISIBLE);
        btnRegistrarColeta.setEnabled(false);

        final String rotaFinal = rotaId;
        Runnable enviar = () -> enviarColeta(
                ColetaController.montarRequest(
                                CooperadoSession.getFuncionarioId(),
                                peso,
                                tipoExterno ? "EXTERNA" : "ENTREGA",
                                rotaFinal,
                                fotoUrlEnviada)
                        .comTriagem(triagem)
                        .comMateriais(equipeId, materiais),
                descricao,
                data);

        if (fotoSelecionada == null || fotoUrlEnviada != null) {
            enviar.run();
            return;
        }

        // 1º a foto vai para o Cloudinary; 2º a URL vai junto com a coleta para a API.
        com.example.renovai.CloudinaryUpload.enviar(
                this,
                fotoSelecionada,
                com.example.renovai.TipoImagem.COLETA,
                new com.example.renovai.CloudinaryUpload.Retorno() {
                    @Override
                    public void sucesso(String url) {
                        fotoUrlEnviada = url;
                        if (isFinishing() || isDestroyed()) return;
                        enviar.run();
                    }

                    @Override
                    public void erro(String mensagem) {
                        falhou(mensagem);
                    }
                });
    }

    private void enviarColeta(ColetaRequest request, String descricao, LocalDate dataPrevistaTriagem) {
        new ColetaController()
                .criar(
                        request,
                        new ColetaController.CriarCallback() {
                            @Override
                            public void onSuccess(ColetaResponse coleta) {
                                com.example.renovai.ColetaDetalhes.salvar(
                                        coleta.getColetaId(), descricao, dataPrevistaTriagem);
                                if (isFinishing() || isDestroyed()) return;
                                progressRegistrarColeta.setVisibility(View.GONE);
                                toast("Coleta registrada com sucesso.");
                                finish();
                            }

                            @Override
                            public void onErro(String mensagem) {
                                falhou(mensagem);
                            }
                        });
    }

    private void falhou(String mensagem) {
        registrando = false;
        if (isFinishing() || isDestroyed()) return;
        progressRegistrarColeta.setVisibility(View.GONE);
        btnRegistrarColeta.setEnabled(true);
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show();
    }

    /** Peso digitado (aceita vírgula), ou null se vazio, inválido ou zero. */
    private static BigDecimal peso(EditText campo) {
        String texto = campo.getText().toString().trim().replace(",", ".");
        if (texto.isEmpty()) return null;
        try {
            BigDecimal valor = new BigDecimal(texto);
            return valor.compareTo(new BigDecimal("0.001")) < 0 ? null : valor.stripTrailingZeros();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void toast(String mensagem) {
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show();
    }
}
