package com.example.renovai.view;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.renovai.ApiClient;
import com.example.renovai.CloudinaryUpload;
import com.example.renovai.CooperadoSession;
import com.example.renovai.EmpresaData;
import com.example.renovai.MaterialApiService;
import com.example.renovai.R;
import com.example.renovai.TipoImagem;
import com.example.renovai.dto.request.MaterialRequest;
import com.example.renovai.dto.response.CategoriaMaterialResponse;
import com.example.renovai.dto.response.MaterialResponse;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Cadastrar material da cooperativa (aberto pelo "+" da Home do Cooperado): categoria, foto
 * (Cloudinary, pasta Renovai/materiais), preço sugerido por kg e se está disponível para venda.
 * Vai para POST /materiais com a cooperativa de quem está logado.
 */
public class AdicionarMaterialActivity extends AppCompatActivity {

    private Spinner spinnerCategoria;
    private TextView txtCategoriaPai, txtSemFoto;
    private ImageView imgPreview;
    private EditText edtPreco;
    private Switch switchDisponivel;
    private MaterialButton btnCadastrar;
    private ProgressBar progresso;

    private final List<CategoriaMaterialResponse> categorias = new ArrayList<>();
    private Uri fotoSelecionada;
    // foto já enviada ao Cloudinary: se a API falhar, tentar de novo não sobe a foto outra vez
    private String fotoUrlEnviada;
    private boolean enviando;

    private final ActivityResultLauncher<String> seletorImagem =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri == null) return;
                        fotoSelecionada = uri;
                        fotoUrlEnviada = null;
                        imgPreview.setImageURI(uri);
                        imgPreview.setVisibility(View.VISIBLE);
                        txtSemFoto.setVisibility(View.GONE);
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adicionar_material);
        com.example.renovai.TelaInsets.conteudo(this, false);

        if (!CooperadoSession.temCooperadoResolvido() || CooperadoSession.getCooperativaId() == null) {
            Toast.makeText(this, "Não foi possível identificar a cooperativa. Faça login novamente.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        spinnerCategoria = findViewById(R.id.spinnerCategoriaNovoMaterial);
        txtCategoriaPai = findViewById(R.id.txtCategoriaPai);
        txtSemFoto = findViewById(R.id.txtSemFotoMaterial);
        imgPreview = findViewById(R.id.imgPreviewFotoMaterial);
        edtPreco = findViewById(R.id.edtPrecoMaterial);
        switchDisponivel = findViewById(R.id.switchMaterialDisponivel);
        btnCadastrar = findViewById(R.id.btnCadastrarMaterial);
        progresso = findViewById(R.id.progressCadastrarMaterial);

        findViewById(R.id.btnVoltarAdicionarMaterial).setOnClickListener(v -> finish());
        boolean fotoDisponivel = CloudinaryUpload.estaConfigurado();
        View anexar = findViewById(R.id.btnAnexarFotoMaterial);
        anexar.setEnabled(fotoDisponivel);
        anexar.setOnClickListener(v -> seletorImagem.launch("image/*"));
        if (!fotoDisponivel) txtSemFoto.setText("Envio de foto não configurado");
        btnCadastrar.setOnClickListener(v -> cadastrar());
        spinnerCategoria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int posicao, long id) {
                mostrarCategoriaPai(posicao);
            }

            @Override
            public void onNothingSelected(AdapterView<?> p) {}
        });

        carregarCategorias();
    }

    private MaterialApiService api() {
        return ApiClient.createService(MaterialApiService.class);
    }

    private void carregarCategorias() {
        btnCadastrar.setEnabled(false);
        api().categorias().enqueue(new Callback<List<CategoriaMaterialResponse>>() {
            @Override
            public void onResponse(Call<List<CategoriaMaterialResponse>> c, Response<List<CategoriaMaterialResponse>> r) {
                if (isFinishing() || isDestroyed()) return;
                categorias.clear();
                if (r.isSuccessful() && r.body() != null)
                    for (CategoriaMaterialResponse cat : r.body())
                        if (cat.getCategoriaId() != null && cat.getNomeCategoria() != null) categorias.add(cat);
                categorias.sort((a, b) -> a.getNomeCategoria().compareToIgnoreCase(b.getNomeCategoria()));
                List<String> nomes = new ArrayList<>();
                for (CategoriaMaterialResponse cat : categorias) nomes.add(cat.getNomeCategoria());
                if (nomes.isEmpty()) nomes.add(r.isSuccessful() ? "Nenhuma categoria cadastrada" : "Não foi possível carregar");
                spinnerCategoria.setAdapter(new ArrayAdapter<>(AdicionarMaterialActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, nomes));
                btnCadastrar.setEnabled(!categorias.isEmpty());
            }

            @Override
            public void onFailure(Call<List<CategoriaMaterialResponse>> c, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(AdicionarMaterialActivity.this, "Sem conexão com o servidor.", Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Mostra de qual categoria a escolhida faz parte (ex.: PET → Plástico). */
    private void mostrarCategoriaPai(int posicao) {
        String pai = posicao >= 0 && posicao < categorias.size() ? categorias.get(posicao).getCategoriaPaiNome() : null;
        txtCategoriaPai.setVisibility(pai == null || pai.isEmpty() ? View.GONE : View.VISIBLE);
        if (pai != null) txtCategoriaPai.setText("Faz parte de: " + pai);
    }

    private void cadastrar() {
        if (enviando) return;
        int posicao = spinnerCategoria.getSelectedItemPosition();
        if (posicao < 0 || posicao >= categorias.size()) {
            Toast.makeText(this, "Escolha a categoria do material.", Toast.LENGTH_SHORT).show();
            return;
        }
        BigDecimal preco = null;
        String texto = edtPreco.getText().toString().trim().replace(",", ".");
        if (!texto.isEmpty()) {
            try {
                preco = new BigDecimal(texto);
            } catch (NumberFormatException e) {
                preco = BigDecimal.valueOf(-1);
            }
            if (preco.signum() < 0) {
                Toast.makeText(this, "Informe um preço válido.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        String categoriaId = categorias.get(posicao).getCategoriaId();
        boolean disponivel = switchDisponivel.isChecked();
        BigDecimal precoFinal = preco;

        enviando = true;
        progresso.setVisibility(View.VISIBLE);
        btnCadastrar.setEnabled(false);
        if (fotoSelecionada == null || fotoUrlEnviada != null) {
            enviar(categoriaId, precoFinal, disponivel, fotoUrlEnviada);
            return;
        }
        // 1º a foto vai para o Cloudinary; 2º a URL vai junto com o material para a API
        CloudinaryUpload.enviar(this, fotoSelecionada, TipoImagem.MATERIAL, new CloudinaryUpload.Retorno() {
            @Override
            public void sucesso(String url) {
                fotoUrlEnviada = url;
                if (isFinishing() || isDestroyed()) return;
                enviar(categoriaId, precoFinal, disponivel, url);
            }

            @Override
            public void erro(String mensagem) {
                falhou(mensagem);
            }
        });
    }

    private void enviar(String categoriaId, BigDecimal preco, boolean disponivel, String fotoUrl) {
        MaterialRequest pedido =
                new MaterialRequest(categoriaId, CooperadoSession.getCooperativaId(), preco, disponivel, fotoUrl);
        api().criar(pedido).enqueue(new Callback<MaterialResponse>() {
            @Override
            public void onResponse(Call<MaterialResponse> c, Response<MaterialResponse> r) {
                if (isFinishing() || isDestroyed()) return;
                if (!r.isSuccessful()) {
                    falhou(EmpresaData.erro(r));
                    return;
                }
                Toast.makeText(AdicionarMaterialActivity.this, "Material cadastrado.", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Call<MaterialResponse> c, Throwable t) {
                falhou("Não foi possível conectar ao servidor.");
            }
        });
    }

    private void falhou(String mensagem) {
        enviando = false;
        if (isFinishing() || isDestroyed()) return;
        progresso.setVisibility(View.GONE);
        btnCadastrar.setEnabled(true);
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show();
    }
}
