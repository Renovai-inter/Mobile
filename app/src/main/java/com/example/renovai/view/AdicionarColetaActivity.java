package com.example.renovai.view;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
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
import com.example.renovai.MaterialApiService;
import com.example.renovai.R;
import com.example.renovai.RotaApiService;
import com.example.renovai.controller.ColetaController;
import com.example.renovai.dto.response.ColetaResponse;
import com.example.renovai.dto.response.MaterialResponse;
import com.example.renovai.dto.response.RotaResponse;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Tela 2.2 — Registrar Coleta.
 *
 * <p>ATENÇÃO — o backend (Requests.ColetaRequest) só aceita cooperadoId, statusId, quantidadeKg,
 * imagemUrl, tipoColeta e rotaId. Os campos "Materiais coletados", "Necessita de triagem?" e "Data
 * prevista da triagem" existem na tela (fiéis ao wireframe) mas NÃO são enviados para a API — não
 * há onde persisti-los ainda. Isso está documentado em IMPLEMENTACAO.md. O mesmo vale para a foto:
 * não existe endpoint de upload de imagem em nenhum lugar da API, então a foto escolhida só aparece
 * na pré-visualização local e não é enviada.
 */
public class AdicionarColetaActivity extends AppCompatActivity {

    private EditText edtPesoColeta, edtDescricaoColeta, edtDataPrevistaTriagem;
    private TextView btnTipoInterno, btnTipoExterno, txtSemFotoColeta, btnAdicionarOutroMaterial;
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
    private final List<String> materiaisColetados = new ArrayList<>();
    private final List<RotaResponse> rotasCarregadas = new ArrayList<>();

    private final ActivityResultLauncher<String> seletorImagem =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri == null) return;
                        fotoSelecionada = uri;
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
        configurarSwitchTriagem();
        configurarDataPrevista();
        configurarFoto();
        configurarMateriais();
        configurarRegistrar();

        carregarCategorias();
        edtDescricaoColeta.setEnabled(false);
        edtDescricaoColeta.setHint("Descrição disponível em breve");
        edtDataPrevistaTriagem.setEnabled(false);
        switchNecessitaTriagem.setEnabled(false);
        spinnerCategoriaMaterial.setEnabled(false);
        btnSalvarMaterial.setEnabled(false);
        btnAdicionarOutroMaterial.setEnabled(false);
        findViewById(R.id.btnAnexarFoto).setEnabled(false);
        txtSemFotoColeta.setText("Anexo de foto disponível em breve");
    }

    private void bindViews() {
        edtPesoColeta = findViewById(R.id.edtPesoColeta);
        edtDescricaoColeta = findViewById(R.id.edtDescricaoColeta);
        edtDataPrevistaTriagem = findViewById(R.id.edtDataPrevistaTriagem);
        btnTipoInterno = findViewById(R.id.btnTipoInterno);
        btnTipoExterno = findViewById(R.id.btnTipoExterno);
        imgPreviewFotoColeta = findViewById(R.id.imgPreviewFotoColeta);
        txtSemFotoColeta = findViewById(R.id.txtSemFotoColeta);
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

    private void configurarSwitchTriagem() {
        switchNecessitaTriagem.setOnCheckedChangeListener(
                (buttonView, isChecked) ->
                        containerDataPrevista.setVisibility(isChecked ? View.VISIBLE : View.GONE));
    }

    private void configurarDataPrevista() {
        edtDataPrevistaTriagem.setOnClickListener(
                v -> {
                    Calendar agora = Calendar.getInstance();
                    new DatePickerDialog(
                                    this,
                                    (view, ano, mes, dia) ->
                                            edtDataPrevistaTriagem.setText(
                                                    String.format(
                                                            Locale.ROOT,
                                                            "%02d/%02d/%04d",
                                                            dia,
                                                            mes + 1,
                                                            ano)),
                                    agora.get(Calendar.YEAR),
                                    agora.get(Calendar.MONTH),
                                    agora.get(Calendar.DAY_OF_MONTH))
                            .show();
                });
    }

    private void configurarFoto() {
        findViewById(R.id.btnAnexarFoto).setOnClickListener(v -> seletorImagem.launch("image/*"));
    }

    private void configurarMateriais() {
        btnSalvarMaterial.setOnClickListener(
                v -> {
                    Object selecionado = spinnerCategoriaMaterial.getSelectedItem();
                    if (selecionado == null) {
                        Toast.makeText(
                                        this,
                                        "Selecione uma categoria de material.",
                                        Toast.LENGTH_SHORT)
                                .show();
                        return;
                    }
                    String categoria = selecionado.toString();
                    materiaisColetados.add(categoria);
                    adicionarLinhaMaterial(categoria);
                    containerMateriaisColeta.setVisibility(View.VISIBLE);
                    containerNovoMaterial.setVisibility(View.GONE);
                    btnAdicionarOutroMaterial.setVisibility(View.VISIBLE);
                });

        btnAdicionarOutroMaterial.setOnClickListener(
                v -> {
                    containerNovoMaterial.setVisibility(View.VISIBLE);
                    btnAdicionarOutroMaterial.setVisibility(View.GONE);
                });
    }

    private void adicionarLinhaMaterial(String categoria) {
        TextView linha = new TextView(this);
        linha.setText("•  " + categoria);
        linha.setTextColor(0xFF222222);
        linha.setTextSize(14f);
        linha.setPadding(0, 4, 0, 4);
        listaMateriaisColeta.addView(linha);
    }

    private void configurarRegistrar() {
        btnRegistrarColeta.setOnClickListener(v -> registrarColeta());
    }

    /**
     * Categorias reais dos materiais já cadastrados na cooperativa do cooperado (GET /materiais).
     */
    private void carregarCategorias() {
        MaterialApiService service = ApiClient.createService(MaterialApiService.class);
        service.listar()
                .enqueue(
                        new Callback<List<MaterialResponse>>() {
                            @Override
                            public void onResponse(
                                    Call<List<MaterialResponse>> call,
                                    Response<List<MaterialResponse>> response) {
                                List<String> categorias = new ArrayList<>();
                                if (response.isSuccessful() && response.body() != null) {
                                    LinkedHashSet<String> distintas = new LinkedHashSet<>();
                                    String cooperativaId = CooperadoSession.getCooperativaId();
                                    for (MaterialResponse m : response.body()) {
                                        boolean daCooperativa =
                                                m.getCooperativaId() == null
                                                        || (cooperativaId != null
                                                                && cooperativaId.equalsIgnoreCase(
                                                                        m.getCooperativaId()));
                                        if (daCooperativa && m.getCategoriaNome() != null) {
                                            distintas.add(m.getCategoriaNome());
                                        }
                                    }
                                    categorias.addAll(distintas);
                                }
                                if (categorias.isEmpty()) {
                                    // Fallback documentado — mesmas 4 categorias já citadas no
                                    // mapeamento
                                    // de telas (tela 5.6, Empresa), usado só se a cooperativa ainda
                                    // não
                                    // tiver nenhum material cadastrado.
                                    categorias.add("Plástico");
                                    categorias.add("Papel");
                                    categorias.add("Metal");
                                    categorias.add("Vidro");
                                }
                                spinnerCategoriaMaterial.setAdapter(
                                        new ArrayAdapter<>(
                                                AdicionarColetaActivity.this,
                                                android.R.layout.simple_spinner_dropdown_item,
                                                categorias));
                            }

                            @Override
                            public void onFailure(Call<List<MaterialResponse>> call, Throwable t) {
                                List<String> fallback = new ArrayList<>();
                                fallback.add("Plástico");
                                fallback.add("Papel");
                                fallback.add("Metal");
                                fallback.add("Vidro");
                                spinnerCategoriaMaterial.setAdapter(
                                        new ArrayAdapter<>(
                                                AdicionarColetaActivity.this,
                                                android.R.layout.simple_spinner_dropdown_item,
                                                fallback));
                            }
                        });
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
        String pesoTexto = edtPesoColeta.getText().toString().trim().replace(",", ".");
        BigDecimal peso;
        try {
            peso = new BigDecimal(pesoTexto);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Informe um peso válido.", Toast.LENGTH_SHORT).show();
            return;
        }

        String rotaId = null;
        if (tipoExterno) {
            int posicao = spinnerRotaOrigem.getSelectedItemPosition();
            if (posicao <= 0 || posicao > rotasCarregadas.size()) {
                Toast.makeText(this, "Selecione a rota de origem.", Toast.LENGTH_SHORT).show();
                return;
            }
            rotaId = rotasCarregadas.get(posicao - 1).getRotaId();
        }

        registrando = true;
        progressRegistrarColeta.setVisibility(View.VISIBLE);
        btnRegistrarColeta.setEnabled(false);

        new ColetaController()
                .criar(
                        CooperadoSession.getFuncionarioId(),
                        peso,
                        tipoExterno ? "EXTERNA" : "ENTREGA",
                        rotaId,
                        null,
                        new ColetaController.CriarCallback() {
                            @Override
                            public void onSuccess(ColetaResponse coleta) {
                                if (isFinishing() || isDestroyed()) return;
                                progressRegistrarColeta.setVisibility(View.GONE);
                                Toast.makeText(
                                                AdicionarColetaActivity.this,
                                                "Coleta registrada com sucesso.",
                                                Toast.LENGTH_SHORT)
                                        .show();
                                finish();
                            }

                            @Override
                            public void onErro(String mensagem) {
                                registrando = false;
                                if (isFinishing() || isDestroyed()) return;
                                progressRegistrarColeta.setVisibility(View.GONE);
                                btnRegistrarColeta.setEnabled(true);
                                Toast.makeText(
                                                AdicionarColetaActivity.this,
                                                mensagem,
                                                Toast.LENGTH_LONG)
                                        .show();
                            }
                        });
    }
}
