package com.example.renovai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Base64;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.renovai.view.EscolheActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Renderiza o HTML real em WebView, sem conexão externa, em celular e largura de desktop. */
@RunWith(AndroidJUnit4.class)
public class EmailLayoutTest {
    @Test public void templateEm320E560SemCortesPreservaMascoteECodigo() throws Exception {
        Context fixtures = InstrumentationRegistry.getInstrumentation().getContext();
        String html;
        byte[] png;
        try (java.io.InputStream in = fixtures.getAssets().open("email/codigo-redefinicao.html")) {
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (java.io.InputStream in = fixtures.getAssets().open("email/nimo.png")) { png = in.readAllBytes(); }
        String rendered = html.replace("{{CODIGO}}", "012345").replace("{{VALIDADE}}", "7")
                .replace("cid:nimo", "data:image/png;base64," + Base64.encodeToString(png, Base64.NO_WRAP));
        for (int width : new int[] {320, 560}) {
            CountDownLatch loaded = new CountDownLatch(1);
            WebView[] web = new WebView[1];
            try (ActivityScenario<EscolheActivity> scene = ActivityScenario.launch(EscolheActivity.class)) {
                scene.onActivity(a -> {
                    WebView w = new WebView(a);
                    web[0] = w;
                    w.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    w.getSettings().setJavaScriptEnabled(true);
                    w.getSettings().setBlockNetworkLoads(true);
                    w.getSettings().setUseWideViewPort(true);
                    w.setWebViewClient(new WebViewClient() {
                        @Override public void onPageFinished(WebView view, String url) { loaded.countDown(); }
                    });
                    int pixels = Math.round(width * a.getResources().getDisplayMetrics().density);
                    a.setContentView(w, new android.view.ViewGroup.LayoutParams(pixels, -1));
                    w.loadDataWithBaseURL("https://preview.invalid/", rendered, "text/html", "UTF-8", null);
                });
                assertTrue("HTML carregado", loaded.await(30, TimeUnit.SECONDS));
                CountDownLatch inspected = new CountDownLatch(1);
                AtomicReference<String> result = new AtomicReference<>();
                scene.onActivity(a -> web[0].evaluateJavascript(
                        "JSON.stringify({overflow:document.documentElement.scrollWidth>innerWidth+1,"
                                + "mascote:document.images[0].complete&&document.images[0].naturalWidth>0,"
                                + "codigo:document.body.innerText.includes('012345')})",
                        value -> { result.set(value); inspected.countDown(); }));
                assertTrue(inspected.await(10, TimeUnit.SECONDS));
                String status = result.get().replace("\\\"", "\"");
                assertTrue(status, status.contains("\"overflow\":false"));
                assertTrue(status, status.contains("\"mascote\":true"));
                assertTrue(status, status.contains("\"codigo\":true"));
                scene.onActivity(a -> {
                    WebView w = web[0];
                    Bitmap capture = Bitmap.createBitmap(w.getWidth(), w.getHeight(), Bitmap.Config.ARGB_8888);
                    w.draw(new Canvas(capture));
                    File dir = new File(a.getExternalFilesDir(null), "interface");
                    dir.mkdirs();
                    try (FileOutputStream out = new FileOutputStream(new File(dir, "email-" + width + ".png"))) {
                        capture.compress(Bitmap.CompressFormat.PNG, 100, out);
                    } catch (Exception e) { throw new AssertionError(e); }
                    capture.recycle();
                    w.destroy();
                });
            }
        }
    }
}
