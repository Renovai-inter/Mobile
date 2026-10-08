package com.example.renovai.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Mapa esquemático da rota (como no wireframe): fundo de quarteirões, trajeto em ângulos retos e as
 * paradas numeradas — verde cheio = partida, vinho = fim, branco com borda verde = coletas.
 * A API não guarda coordenadas, então o desenho é ilustrativo (a ordem das paradas é a real).
 */
public class RotaMapView extends View {

    private int total = 0;
    private final Paint fundo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rua = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trajeto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pino = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borda = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint numero = new Paint(Paint.ANTI_ALIAS_FLAG);

    public RotaMapView(Context c) { super(c); init(); }
    public RotaMapView(Context c, AttributeSet a) { super(c, a); init(); }
    public RotaMapView(Context c, AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        fundo.setColor(Color.parseColor("#E3EBE3"));
        rua.setColor(Color.parseColor("#D9CDCD"));
        rua.setStyle(Paint.Style.STROKE);
        trajeto.setColor(Color.parseColor("#519059"));
        trajeto.setStyle(Paint.Style.STROKE);
        trajeto.setStrokeJoin(Paint.Join.MITER);
        borda.setStyle(Paint.Style.STROKE);
        borda.setColor(Color.parseColor("#519059"));
        numero.setTextAlign(Paint.Align.CENTER);
        numero.setFakeBoldText(true);
    }

    public void setTotalParadas(int n) {
        total = Math.max(0, n);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), d = getResources().getDisplayMetrics().density;
        RectF r = new RectF(0, 0, w, h);
        c.drawRoundRect(r, 14 * d, 14 * d, fundo);

        rua.setStrokeWidth(12 * d);
        for (int i = 1; i <= 3; i++) c.drawLine(w * i / 4f * 0.95f, 0, w * i / 4f * 0.95f, h, rua);
        for (int j = 1; j <= 2; j++) c.drawLine(0, h * j / 3f, w, h * j / 3f, rua);

        if (total < 1) return;
        float m = 30 * d;
        float[] xs = new float[total], ys = new float[total];
        for (int i = 0; i < total; i++) {
            float t = total == 1 ? 0f : i / (float) (total - 1);
            xs[i] = m + (w - 2 * m) * t;
            ys[i] = (h - m) - (h - 2 * m) * (i % 2 == 0 ? t : Math.min(1f, t + 0.12f)) ;
        }
        trajeto.setStrokeWidth(5 * d);
        Path p = new Path();
        p.moveTo(xs[0], ys[0]);
        for (int i = 1; i < total; i++) {
            if (i % 2 == 1) { p.lineTo(xs[i - 1], ys[i]); p.lineTo(xs[i], ys[i]); }
            else { p.lineTo(xs[i], ys[i - 1]); p.lineTo(xs[i], ys[i]); }
        }
        c.drawPath(p, trajeto);

        float raio = 11 * d;
        numero.setTextSize(11 * d);
        borda.setStrokeWidth(1.5f * d);
        for (int i = 0; i < total; i++) {
            boolean ini = i == 0, fim = i == total - 1 && total > 1;
            pino.setColor(ini ? Color.parseColor("#519059") : fim ? Color.parseColor("#882B4E") : Color.WHITE);
            c.drawCircle(xs[i], ys[i], raio, pino);
            if (!ini && !fim) c.drawCircle(xs[i], ys[i], raio, borda);
            numero.setColor(ini || fim ? Color.WHITE : Color.parseColor("#519059"));
            c.drawText(String.valueOf(i + 1), xs[i], ys[i] + 4 * d, numero);
        }
    }
}
