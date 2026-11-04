package com.example.renovai;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.renovai.view.EscolheActivity;
import com.example.renovai.view.FotoActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

/** Exercita componentes reais sem autenticar, alterar contas ou acessar a API. */
@RunWith(AndroidJUnit4.class)
public class InterfaceRenovaiTest {
    @Test public void confirmarECancelarMantemCallbacks() {
        AtomicInteger confirmacoes = new AtomicInteger();
        try (ActivityScenario<EscolheActivity> scene = ActivityScenario.launch(EscolheActivity.class)) {
            scene.onActivity(a -> {
                AlertDialog dialog = new RenovaiDialogBuilder(a).setTitle("Confirmar alteração")
                        .setMessage("Deseja salvar os dados informados?")
                        .setPositiveButton("Salvar", (d,w) -> confirmacoes.incrementAndGet())
                        .setNegativeButton("Cancelar", null).show();
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
                assertEquals(0, confirmacoes.get());
                AlertDialog novo = new RenovaiDialogBuilder(a).setTitle("Confirmar alteração")
                        .setPositiveButton("Salvar", (d,w) -> confirmacoes.incrementAndGet()).show();
                novo.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertEquals(1, confirmacoes.get());
        }
    }

    @Test public void fecharNaoExecutaConfirmacaoEEnviaCancelamento() {
        AtomicInteger cancelamentos = new AtomicInteger(), confirmacoes = new AtomicInteger();
        try (ActivityScenario<EscolheActivity> scene = ActivityScenario.launch(EscolheActivity.class)) {
            scene.onActivity(a -> {
                AlertDialog d = new RenovaiDialogBuilder(a).setTitle("Cancelar operação")
                        .setPositiveButton("Confirmar", (v,w) -> confirmacoes.incrementAndGet()).create();
                d.setOnCancelListener(ignored -> cancelamentos.incrementAndGet());
                d.show();
                d.getWindow().getDecorView().findViewWithTag("renovai.fechar").performClick();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertEquals(1, cancelamentos.get());
            assertEquals(0, confirmacoes.get());
        }
    }

    @Test public void selecaoMultiplaMantemValoresEAcaoNeutra() {
        boolean[] selected = {true, false};
        AtomicInteger acao = new AtomicInteger();
        AlertDialog[] popup = new AlertDialog[1];
        try (ActivityScenario<EscolheActivity> scene = ActivityScenario.launch(EscolheActivity.class)) {
            scene.onActivity(a -> {
                popup[0] = new RenovaiDialogBuilder(a).setTitle("Materiais de interesse")
                        .setMultiChoiceItems(new String[] {"Alumínio", "Papel"}, selected,
                                (dialog, pos, checked) -> selected[pos] = checked)
                        .setPositiveButton("Salvar", null)
                        .setNeutralButton("Salvar progresso", (dialog,w) -> acao.incrementAndGet())
                        .setNegativeButton("Cancelar", null).show();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scene.onActivity(a -> {
                assertTrue(popup[0].getListView().isItemChecked(0));
                assertFalse(popup[0].getListView().isItemChecked(1));
                popup[0].getButton(AlertDialog.BUTTON_NEUTRAL).performClick();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertEquals(1, acao.get());
        }
    }

    @Test public void formularioLongoMantemAcoesDentroDaTela() throws Exception {
        AlertDialog[] dialog = new AlertDialog[1];
        try (ActivityScenario<EscolheActivity> scene = ActivityScenario.launch(EscolheActivity.class)) {
            scene.onActivity(a -> {
                LinearLayout campos = new LinearLayout(a);
                campos.setOrientation(LinearLayout.VERTICAL);
                for (int i = 0; i < 20; i++) {
                    EditText campo = new EditText(a);
                    campo.setText("Informação atual do usuário " + i);
                    campos.addView(campo);
                }
                dialog[0] = new RenovaiDialogBuilder(a).setTitle("Dados do perfil com título longo")
                        .setView(campos).setPositiveButton("Salvar alterações", null)
                        .setNegativeButton("Cancelar", null).show();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scene.onActivity(a -> {
                Rect r = new Rect();
                assertTrue(dialog[0].getButton(AlertDialog.BUTTON_POSITIVE).getGlobalVisibleRect(r));
                assertTrue(r.height() > 0);
                assertTrue(r.bottom <= a.getResources().getDisplayMetrics().heightPixels);
                assertTrue(dialog[0].getWindow().getDecorView().getWidth()
                        <= a.getResources().getDisplayMetrics().widthPixels);
            });
            screenshot("formulario");
        }
    }

    @Test public void popupReferenciaTemAcoesVerticaisEEstadoDeSalvamento() throws Exception {
        AlertDialog[] dialog = new AlertDialog[1];
        try (ActivityScenario<EscolheActivity> scene = ActivityScenario.launch(EscolheActivity.class)) {
            scene.onActivity(a -> dialog[0] = new RenovaiDialogBuilder(a)
                    .setTitle("Sair da conta").setMessage("Deseja sair da conta?")
                    .setPositiveButton("Sair", null).setNegativeButton("Cancelar", null).show());
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scene.onActivity(a -> {
                View positive = dialog[0].getButton(AlertDialog.BUTTON_POSITIVE);
                View negative = dialog[0].getButton(AlertDialog.BUTTON_NEGATIVE);
                View close = dialog[0].getWindow().getDecorView().findViewWithTag("renovai.fechar");
                Rect area = new Rect();
                assertTrue(close.getGlobalVisibleRect(area));
                assertEquals(close.getHeight(), area.height());
                assertEquals(close.getWidth(), area.width());
                assertEquals(positive.getParent(), negative.getParent());
                assertEquals(LinearLayout.VERTICAL, ((LinearLayout) positive.getParent()).getOrientation());
                RenovaiDialogBuilder.emAndamento(dialog[0], true);
                assertFalse(positive.isEnabled());
                assertFalse(dialog[0].getWindow().getDecorView().findViewWithTag("renovai.fechar").isEnabled());
                RenovaiDialogBuilder.emAndamento(dialog[0], false);
                assertTrue(positive.isEnabled());
            });
            screenshot("popup");
        }
    }

    @Test public void fotoLocalAbreInternamenteEFechaSemMudarNavegacao() throws Exception {
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File arquivo = new File(app.getCacheDir(), "teste-foto-interface.png");
        Bitmap image = Bitmap.createBitmap(100, 250, Bitmap.Config.ARGB_8888);
        new Canvas(image).drawColor(Color.rgb(81,143,93));
        try (FileOutputStream out = new FileOutputStream(arquivo)) { image.compress(Bitmap.CompressFormat.PNG, 100, out); }
        image.recycle();
        Uri uri = FileProvider.getUriForFile(app, app.getPackageName() + ".fileprovider", arquivo);
        Intent intent = new Intent(app, FotoActivity.class).setData(uri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try (ActivityScenario<FotoActivity> scene = ActivityScenario.launch(intent)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            screenshot("foto");
            scene.onActivity(a -> {
                assertNotNull(a.findViewById(R.id.containerFoto));
                assertTrue(androidx.core.view.WindowCompat.getInsetsController(
                        a.getWindow(), a.getWindow().getDecorView()).isAppearanceLightStatusBars());
                a.findViewById(R.id.btnFecharFoto).performClick();
                assertTrue(a.isFinishing());
            });
        }
    }

    @Test public void perfilEmpresaTemSecoesECamposComIdsProprios() {
        try (ActivityScenario<EscolheActivity> scene = ActivityScenario.launch(EscolheActivity.class)) {
            scene.onActivity(a -> {
                View perfil = a.getLayoutInflater().inflate(R.layout.activity_empresa_perfil, null, false);
                for (int id : new int[] {R.id.campoNomeE, R.id.campoEmailE, R.id.campoCnpjE,
                        R.id.campoEnderecoE, R.id.campoTelefoneE, R.id.campoCidadeE, R.id.campoDescricaoE}) {
                    assertNotNull(perfil.findViewById(id));
                    assertNotNull(perfil.findViewById(id).findViewById(R.id.txtCampoValor));
                }
            });
        }
    }

    private void screenshot(String nome) throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File dir = new File(context.getExternalFilesDir(null), "interface");
        assertTrue(dir.exists() || dir.mkdirs());
        Bitmap imagem = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(imagem);
        try (FileOutputStream out = new FileOutputStream(new File(dir, nome + ".png"))) {
            imagem.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        imagem.recycle();
    }
}
