package com.example.renovai;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;

/** Exercita layouts reais sem abrir contas, fazer login ou gravar dados na API. */
@RunWith(AndroidJUnit4.class)
public class LayoutsAreasTest {
    private Context contexto(float fonte) {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration config = new Configuration(target.getResources().getConfiguration());
        config.fontScale = fonte;
        return new ContextThemeWrapper(
                target.createConfigurationContext(config), R.style.Theme_Renovai);
    }

    private int px(Context c, int dp) {
        return Math.round(dp * c.getResources().getDisplayMetrics().density);
    }

    private void medir(View v, Context c, int largura, int altura) {
        v.measure(
                View.MeasureSpec.makeMeasureSpec(px(c, largura), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(px(c, altura), View.MeasureSpec.EXACTLY));
        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
    }

    @Test
    public void barraCooperadoNaoAcumulaInsetNemCortaItens() {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            Context c = contexto(1.3f);
                            View raiz =
                                    LayoutInflater.from(c)
                                            .inflate(
                                                    R.layout.activity_cooperado_home,
                                                    new FrameLayout(c),
                                                    false);
                            BottomNavigationView nav =
                                    raiz.findViewById(R.id.bottomNavigationCooperado);
                            int altura = nav.getLayoutParams().height,
                                    fundo = nav.getPaddingBottom();
                            TelaInsets.navegacao(nav);
                            WindowInsetsCompat inset =
                                    new WindowInsetsCompat.Builder()
                                            .setInsets(
                                                    WindowInsetsCompat.Type.systemBars(),
                                                    Insets.of(0, px(c, 24), 0, px(c, 32)))
                                            .build();
                            for (int i = 0; i < 8; i++)
                                ViewCompat.dispatchApplyWindowInsets(nav, inset);
                            assertEquals(fundo + px(c, 32), nav.getPaddingBottom());
                            assertEquals(altura + px(c, 32), nav.getLayoutParams().height);
                            medir(raiz, c, 320, 740);
                            ViewGroup menu = (ViewGroup) nav.getChildAt(0);
                            assertEquals(5, menu.getChildCount());
                            for (int i = 0; i < menu.getChildCount(); i++) {
                                View item = menu.getChildAt(i);
                                assertTrue(item.getHeight() > 0);
                                assertTrue(
                                        item.getBottom()
                                                <= nav.getHeight() - nav.getPaddingBottom());
                            }
                            WindowInsetsCompat gestos =
                                    new WindowInsetsCompat.Builder()
                                            .setInsets(
                                                    WindowInsetsCompat.Type.systemBars(),
                                                    Insets.of(0, px(c, 24), 0, px(c, 16)))
                                            .build();
                            ViewCompat.dispatchApplyWindowInsets(nav, gestos);
                            assertEquals(altura + px(c, 16), nav.getLayoutParams().height);
                            medir(raiz, c, 320, 740);
                            try {
                                Bitmap bitmap =
                                        Bitmap.createBitmap(
                                                nav.getWidth(),
                                                nav.getHeight(),
                                                Bitmap.Config.ARGB_8888);
                                nav.draw(new Canvas(bitmap));
                                File arquivo =
                                        new File(
                                                c.getExternalFilesDir(null),
                                                "cooperado-nav-320-fonte130.png");
                                try (FileOutputStream out = new FileOutputStream(arquivo)) {
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                                }
                                bitmap.recycle();
                            } catch (Exception e) {
                                throw new AssertionError(e);
                            }
                        });
    }

    @Test
    public void telasDasTresAreasInflamEmLarguraEstreitaComFonteMaior() {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            Context c = contexto(1.3f);
                            int[] telas = {
                                R.layout.activity_cooperado_home,
                                R.layout.activity_coletas_list,
                                R.layout.activity_triagens_list,
                                R.layout.activity_rateios,
                                R.layout.activity_perfil_cooperado,
                                R.layout.activity_adicionar_coleta,
                                R.layout.activity_completar_triagem,
                                R.layout.activity_gestor_home,
                                R.layout.activity_gestor_coletas,
                                R.layout.activity_gestor_coleta_detalhe,
                                R.layout.activity_gestor_estoque,
                                R.layout.activity_gestor_criar_relatorio,
                                R.layout.activity_gestor_lista,
                                R.layout.activity_gestor_pedido_detalhe,
                                R.layout.activity_gestor_chat,
                                R.layout.activity_gestor_perfil,
                                R.layout.activity_gestor_rateio_detalhe,
                                R.layout.activity_gestor_nova_triagem,
                                R.layout.activity_gestor_triagem_detalhe,
                                R.layout.activity_gestor_funcionarios,
                                R.layout.activity_gestor_funcionario_detalhe,
                                R.layout.activity_gestor_nova_rota,
                                R.layout.activity_gestor_rota_detalhe,
                                R.layout.activity_motorista_home,
                                R.layout.activity_motorista_rotas,
                                R.layout.activity_motorista_rota_detalhe,
                                R.layout.activity_motorista_perfil
                            };
                            for (int layout : telas) {
                                View raiz =
                                        LayoutInflater.from(c)
                                                .inflate(layout, new FrameLayout(c), false);
                                medir(raiz, c, 320, 740);
                                assertEquals(
                                        c.getResources().getResourceEntryName(layout),
                                        px(c, 320),
                                        raiz.getMeasuredWidth());
                            }
                        });
    }
}
