package com.example.renovai.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.renovai.ImagemUrl;
import com.example.renovai.R;
import com.example.renovai.TelaInsets;

/** Visualização interna: foto inteira, pinça, arrasto e toque duplo para restaurar. */
public class FotoActivity extends AppCompatActivity {
    private ZoomFoto foto;

    public static void abrir(Context context, Uri uri) {
        if (uri == null || !("content".equals(uri.getScheme())
                || ImagemUrl.normalizar(uri.toString()) != null)) return;
        Intent i = new Intent(context, FotoActivity.class).setData(uri);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(i);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_foto);
        TelaInsets.conteudo(this, false);
        androidx.core.view.WindowInsetsControllerCompat barras =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        barras.setAppearanceLightStatusBars(true);
        barras.setAppearanceLightNavigationBars(true);
        findViewById(R.id.btnFecharFoto).setOnClickListener(v -> finish());
        foto = new ZoomFoto(this);
        foto.setContentDescription(getString(R.string.foto_abrir));
        ((android.widget.FrameLayout) findViewById(R.id.containerFoto)).addView(foto,
                new android.widget.FrameLayout.LayoutParams(-1, -1));
        findViewById(R.id.btnRecarregarFoto).setOnClickListener(v -> carregar());
        carregar();
    }

    private void carregar() {
        Uri uri = getIntent().getData();
        if (uri == null) { finish(); return; }
        findViewById(R.id.progressoFoto).setVisibility(View.VISIBLE);
        findViewById(R.id.btnRecarregarFoto).setVisibility(View.GONE);
        Glide.with(this).load(uri).fitCenter().listener(new RequestListener<Drawable>() {
            @Override public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean first) {
                findViewById(R.id.progressoFoto).setVisibility(View.GONE);
                findViewById(R.id.btnRecarregarFoto).setVisibility(View.VISIBLE);
                Toast.makeText(FotoActivity.this, R.string.foto_indisponivel, Toast.LENGTH_LONG).show();
                return false;
            }
            @Override public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource source, boolean first) {
                findViewById(R.id.progressoFoto).setVisibility(View.GONE);
                return false;
            }
        }).into(foto);
    }

    static class ZoomFoto extends androidx.appcompat.widget.AppCompatImageView {
        private final Matrix transform = new Matrix();
        private float scale = 1f;
        private final ScaleGestureDetector pinch;
        private final GestureDetector gestures;
        ZoomFoto(Context context) {
            super(context);
            setScaleType(ImageView.ScaleType.MATRIX);
            pinch = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScale(ScaleGestureDetector detector) {
                    float next = Math.max(1f, Math.min(4f, scale * detector.getScaleFactor()));
                    transform.postScale(next / scale, next / scale, detector.getFocusX(), detector.getFocusY());
                    scale = next;
                    limitar();
                    return true;
                }
            });
            gestures = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override public boolean onDown(MotionEvent e) { return true; }
                @Override public boolean onDoubleTap(MotionEvent e) { ajustar(); return true; }
                @Override public boolean onSingleTapUp(MotionEvent e) { performClick(); return true; }
                @Override public boolean onScroll(MotionEvent a, MotionEvent b, float dx, float dy) {
                    if (scale > 1f && !pinch.isInProgress()) { transform.postTranslate(-dx, -dy); limitar(); }
                    return true;
                }
            });
        }
        @Override public void setImageDrawable(Drawable drawable) { super.setImageDrawable(drawable); post(this::ajustar); }
        @Override protected void onSizeChanged(int w, int h, int ow, int oh) { super.onSizeChanged(w, h, ow, oh); ajustar(); }
        private void ajustar() {
            if (getDrawable() == null || getWidth() == 0 || getHeight() == 0) return;
            int w = getDrawable().getIntrinsicWidth(), h = getDrawable().getIntrinsicHeight();
            if (w <= 0 || h <= 0) return;
            float fit = Math.min((float) getWidth() / w, (float) getHeight() / h);
            transform.setScale(fit, fit);
            transform.postTranslate((getWidth() - w * fit) / 2, (getHeight() - h * fit) / 2);
            scale = 1;
            setImageMatrix(transform);
        }
        private void limitar() {
            if (getDrawable() == null) return;
            android.graphics.RectF r = new android.graphics.RectF(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
            transform.mapRect(r);
            float dx = r.width() <= getWidth() ? (getWidth() - r.width()) / 2 - r.left
                    : r.left > 0 ? -r.left : r.right < getWidth() ? getWidth() - r.right : 0;
            float dy = r.height() <= getHeight() ? (getHeight() - r.height()) / 2 - r.top
                    : r.top > 0 ? -r.top : r.bottom < getHeight() ? getHeight() - r.bottom : 0;
            transform.postTranslate(dx, dy);
            setImageMatrix(transform);
        }
        @Override public boolean onTouchEvent(MotionEvent event) {
            pinch.onTouchEvent(event); gestures.onTouchEvent(event); return true;
        }
        @Override public boolean performClick() { super.performClick(); return true; }
    }
}
