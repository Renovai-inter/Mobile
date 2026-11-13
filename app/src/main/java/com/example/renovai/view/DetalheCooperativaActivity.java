package com.example.renovai.view;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.renovai.ApiClient;
import com.example.renovai.CloudinaryUpload;
import com.example.renovai.CooperadoSession;
import com.example.renovai.GestorApiService;
import com.example.renovai.Imagens;
import com.example.renovai.R;
import com.example.renovai.TipoImagem;
import com.example.renovai.dto.request.GestorRequests;
import com.example.renovai.dto.response.CooperativaResponse;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Informações da cooperativa aberta ao tocar no nome do cabeçalho do gestor. */
public class DetalheCooperativaActivity extends AppCompatActivity {
    private CooperativaResponse cooperativa;
    private Uri uriCamera;
    private boolean enviandoFoto;
    private ActivityResultLauncher<Uri> camera;
    private ActivityResultLauncher<String> galeria;

    public static void abrir(Context context) {
        context.startActivity(new android.content.Intent(context, DetalheCooperativaActivity.class));
    }

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhe_cooperativa);
        com.example.renovai.TelaInsets.conteudo(this, false);
        findViewById(R.id.btnVoltarDetalheCooperativa).setOnClickListener(v -> finish());
        camera = registerForActivityResult(new ActivityResultContracts.TakePicture(), salvo -> {
            if (salvo && uriCamera != null) enviarFoto(uriCamera);
        });
        galeria = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) enviarFoto(uri);
        });
        String cargo = CooperadoSession.getCargo();
        boolean gestor = cargo != null && (cargo.toUpperCase(Locale.ROOT).contains("GESTOR")
                || cargo.equalsIgnoreCase("ADMIN_COOPERATIVA"));
        View alterarFoto = findViewById(R.id.btnAlterarFotoCooperativa);
        alterarFoto.setVisibility(gestor ? View.VISIBLE : View.GONE);
        alterarFoto.setOnClickListener(v -> escolherFoto());
        carregar();
    }

    private void carregar() {
        String id = CooperadoSession.getCooperativaId();
        if (id == null || id.isBlank()) {
            Toast.makeText(this, "Não foi possível identificar a cooperativa.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        ApiClient.createService(GestorApiService.class).minhaCooperativa().enqueue(new Callback<>() {
            @Override public void onResponse(Call<CooperativaResponse> call, Response<CooperativaResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                ((ProgressBar) findViewById(R.id.progressoCooperativaDetalhe)).setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(DetalheCooperativaActivity.this, "Não foi possível carregar os dados da cooperativa.", Toast.LENGTH_LONG).show();
                    return;
                }
                cooperativa = response.body();
                renderizar();
            }
            @Override public void onFailure(Call<CooperativaResponse> call, Throwable t) {
                if (!isFinishing() && !isDestroyed()) {
                    ((ProgressBar) findViewById(R.id.progressoCooperativaDetalhe)).setVisibility(View.GONE);
                    Toast.makeText(DetalheCooperativaActivity.this, "Sem conexão com o servidor.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void renderizar() {
        ((TextView) findViewById(R.id.txtNomeCooperativaDetalhe)).setText(valor(cooperativa.getNome(), "Cooperativa"));
        ((TextView) findViewById(R.id.txtDescricaoCooperativaDetalhe)).setText("Descrição\n" + valor(cooperativa.getDescricao(), "Não informada."));
        ((TextView) findViewById(R.id.txtCidadeCooperativaDetalhe)).setText("Cidade\n" + valor(cooperativa.getCidade(), "Não informada"));
        ((TextView) findViewById(R.id.txtHorarioCooperativaDetalhe)).setText("Horário de funcionamento\n" + valor(cooperativa.getHorarioFuncionamento(), "Não informado"));
        ((TextView) findViewById(R.id.txtContatoCooperativaDetalhe)).setText("Contato preferencial\n" + valor(cooperativa.getContatoPreferencial(), "Não informado"));
        Integer quantidade = cooperativa.getNumeroCooperados();
        ((TextView) findViewById(R.id.txtNumeroCooperadosDetalhe)).setText("Cooperados\n" + (quantidade == null ? "Não informado" : quantidade));
        Imagens.carregar((ImageView) findViewById(R.id.imgFotoCooperativaDetalhe), cooperativa.getImagemUrl(), true, TipoImagem.COOPERATIVA);
        findViewById(R.id.dadosCooperativaDetalhe).setVisibility(View.VISIBLE);
    }

    private void escolherFoto() {
        if (enviandoFoto) return;
        if (!CloudinaryUpload.estaConfigurado()) {
            Toast.makeText(this, "Envio de fotos não está configurado.", Toast.LENGTH_LONG).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Foto da cooperativa")
                .setItems(new String[]{"Tirar foto", "Escolher da galeria"}, (dialog, opcao) -> {
                    if (opcao == 0) abrirCamera(); else galeria.launch("image/*");
                }).show();
    }

    private void abrirCamera() {
        try {
            File arquivo = File.createTempFile("cooperativa_", ".jpg", getCacheDir());
            uriCamera = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", arquivo);
            camera.launch(uriCamera);
        } catch (IOException | IllegalArgumentException e) {
            Toast.makeText(this, "Não foi possível abrir a câmera. Escolha uma foto da galeria.", Toast.LENGTH_LONG).show();
        }
    }

    private void enviarFoto(Uri uri) {
        enviandoFoto = true;
        findViewById(R.id.btnAlterarFotoCooperativa).setEnabled(false);
        CloudinaryUpload.enviar(this, uri, TipoImagem.COOPERATIVA, new CloudinaryUpload.Retorno() {
            @Override public void sucesso(String url) { salvarUrl(url); }
            @Override public void erro(String mensagem) {
                enviandoFoto = false;
                if (!isFinishing() && !isDestroyed()) {
                    findViewById(R.id.btnAlterarFotoCooperativa).setEnabled(true);
                    Toast.makeText(DetalheCooperativaActivity.this, mensagem, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void salvarUrl(String url) {
        ApiClient.createService(GestorApiService.class)
                .atualizarFotoCooperativa(new GestorRequests.ImagemCooperativa(url))
                .enqueue(new Callback<>() {
                    @Override public void onResponse(Call<CooperativaResponse> call, Response<CooperativaResponse> response) {
                        enviandoFoto = false;
                        if (isFinishing() || isDestroyed()) return;
                        findViewById(R.id.btnAlterarFotoCooperativa).setEnabled(true);
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(DetalheCooperativaActivity.this,
                                    response.code() == 403 ? "Apenas o gestor da cooperativa pode alterar a foto." : "Não foi possível salvar a foto da cooperativa.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        cooperativa = response.body();
                        renderizar();
                        Toast.makeText(DetalheCooperativaActivity.this, "Foto da cooperativa atualizada.", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onFailure(Call<CooperativaResponse> call, Throwable t) {
                        enviandoFoto = false;
                        if (!isFinishing() && !isDestroyed()) {
                            findViewById(R.id.btnAlterarFotoCooperativa).setEnabled(true);
                            Toast.makeText(DetalheCooperativaActivity.this, "Sem conexão ao salvar a foto.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private static String valor(String s, String vazio) { return s == null || s.isBlank() ? vazio : s; }
}
