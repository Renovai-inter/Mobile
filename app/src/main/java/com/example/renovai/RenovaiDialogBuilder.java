package com.example.renovai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Diálogo nativo com o cartão, as folhas e as ações de Renovai_Popup_Mobile.html. */
public class RenovaiDialogBuilder extends MaterialAlertDialogBuilder {
    private CharSequence titulo;
    private boolean cancelavel = true;

    public RenovaiDialogBuilder(Context context) { super(context); }

    @Override public RenovaiDialogBuilder setMessage(CharSequence value) { super.setMessage(value); return this; }
    @Override public RenovaiDialogBuilder setMessage(int value) { super.setMessage(value); return this; }
    @Override public RenovaiDialogBuilder setPositiveButton(CharSequence text, android.content.DialogInterface.OnClickListener listener) { super.setPositiveButton(text, listener); return this; }
    @Override public RenovaiDialogBuilder setPositiveButton(int text, android.content.DialogInterface.OnClickListener listener) { super.setPositiveButton(text, listener); return this; }
    @Override public RenovaiDialogBuilder setNegativeButton(CharSequence text, android.content.DialogInterface.OnClickListener listener) { super.setNegativeButton(text, listener); return this; }
    @Override public RenovaiDialogBuilder setNegativeButton(int text, android.content.DialogInterface.OnClickListener listener) { super.setNegativeButton(text, listener); return this; }
    @Override public RenovaiDialogBuilder setNeutralButton(CharSequence text, android.content.DialogInterface.OnClickListener listener) { super.setNeutralButton(text, listener); return this; }
    @Override public RenovaiDialogBuilder setNeutralButton(int text, android.content.DialogInterface.OnClickListener listener) { super.setNeutralButton(text, listener); return this; }

    @Override public RenovaiDialogBuilder setTitle(CharSequence title) {
        titulo = title;
        super.setTitle(title);
        return this;
    }

    @Override public RenovaiDialogBuilder setTitle(int title) {
        return setTitle(getContext().getText(title));
    }

    @Override public RenovaiDialogBuilder setCancelable(boolean value) {
        cancelavel = value;
        super.setCancelable(value);
        return this;
    }

    @Override public RenovaiDialogBuilder setView(View view) {
        if (view instanceof ScrollView || view instanceof NestedScrollView) super.setView(view);
        else {
            ScrollView scroll = new ScrollView(getContext());
            scroll.setFillViewport(false);
            scroll.setClipToPadding(false);
            scroll.addView(view);
            super.setView(scroll);
        }
        return this;
    }

    @Override public AlertDialog create() {
        FrameLayout header = new FrameLayout(getContext());
        header.setClipChildren(false);
        TextView title = new TextView(getContext());
        title.setText(titulo);
        title.setTextSize(23);
        title.setTextColor(0xFF882B4E);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(dp(12), 0, dp(12), 0);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-1, -2);
        tp.leftMargin = dp(23);
        tp.rightMargin = dp(23);
        tp.topMargin = dp(44);
        tp.bottomMargin = dp(10);
        header.addView(title, tp);
        ImageButton close = new ImageButton(getContext());
        close.setTag("renovai.fechar");
        close.setImageResource(R.drawable.ic_popup_fechar);
        close.setContentDescription(getContext().getString(R.string.popup_fechar));
        close.setBackgroundResource(android.R.color.transparent);
        close.setPadding(dp(10), dp(10), dp(10), dp(10));
        close.setVisibility(cancelavel ? View.VISIBLE : View.GONE);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP | Gravity.END);
        cp.topMargin = dp(3);
        cp.rightMargin = dp(3);
        header.addView(close, cp);
        super.setCustomTitle(header);
        AlertDialog dialog = super.create();
        close.setOnClickListener(v -> dialog.cancel());
        dialog.setOnShowListener(ignored -> aplicar(dialog));
        return dialog;
    }

    private void aplicar(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(new Fundo(dp(27)));
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams p = window.getAttributes();
        p.dimAmount = 0.44f;
        p.width = Math.min(dp(340), getContext().getResources().getDisplayMetrics().widthPixels - dp(36));
        p.height = WindowManager.LayoutParams.WRAP_CONTENT;
        p.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        window.setAttributes(p);
        window.setElevation(dp(18));
        TextView message = dialog.findViewById(android.R.id.message);
        if (message != null) {
            message.setGravity(Gravity.CENTER);
            message.setTextSize(13);
            message.setTextColor(0xFF66645F);
            message.setLineSpacing(dp(3), 1f);
        }
        Button first = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (first == null || !(first.getParent() instanceof ViewGroup)) return;
        ViewGroup original = (ViewGroup) first.getParent();
        if (!(original.getParent() instanceof ViewGroup)) return;
        ViewGroup parent = (ViewGroup) original.getParent();
        int index = parent.indexOfChild(original);
        LinearLayout actions = new LinearLayout(getContext());
        actions.setOrientation(LinearLayout.VERTICAL);
        actions.setPadding(dp(23), dp(12), dp(23), dp(22));
        for (int id : new int[] {AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEUTRAL, AlertDialog.BUTTON_NEGATIVE}) {
            Button b = dialog.getButton(id);
            if (b == null || b.getVisibility() != View.VISIBLE) continue;
            ((ViewGroup) b.getParent()).removeView(b);
            b.setMinHeight(dp(48));
            b.setMinimumHeight(dp(48));
            b.setSingleLine(false);
            b.setMaxLines(4);
            b.setTextSize(13);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
            bp.topMargin = id == AlertDialog.BUTTON_POSITIVE ? 0 : dp(8);
            actions.addView(b, bp);
        }
        ViewGroup.LayoutParams params = original.getLayoutParams();
        // AlertDialogLayout reconhece o painel pelo ID e reserva sua altura antes do formulário.
        actions.setId(original.getId());
        actions.measure(View.MeasureSpec.makeMeasureSpec(p.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        actions.setMinimumHeight(actions.getMeasuredHeight());
        parent.removeView(original);
        parent.addView(actions, index, params);
    }

    private int dp(float v) { return Math.round(v * getContext().getResources().getDisplayMetrics().density); }

    public static void emAndamento(AlertDialog dialog, boolean busy) {
        dialog.setCancelable(!busy);
        dialog.setCanceledOnTouchOutside(!busy);
        if (dialog.getWindow() != null) {
            View close = dialog.getWindow().getDecorView().findViewWithTag("renovai.fechar");
            if (close != null) close.setEnabled(!busy);
        }
        for (int id : new int[] {AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL}) {
            Button button = dialog.getButton(id);
            if (button != null) button.setEnabled(!busy);
        }
    }

    /** Contornos das folhas reproduzidos do SVG da referência; sem imagens externas. */
    private static class Fundo extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float radius;
        Fundo(float radius) { this.radius = radius; }
        @Override public void draw(Canvas canvas) {
            RectF area = new RectF(getBounds());
            Path clip = new Path();
            clip.addRoundRect(area, radius, radius, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(clip);
            paint.setColor(0xFFFAF9F6);
            canvas.drawRect(area, paint);
            float s = area.width() / 340f;
            canvas.translate(area.left, area.top);
            canvas.scale(s, s);
            float h = area.height() / s;
            paint.setColor(0xFFEAF0E5);
            canvas.drawCircle(370, -25, 90, paint);
            paint.setColor(0xFFF4E7E9);
            canvas.drawCircle(-25, h + 20, 70, paint);
            folha(canvas, -8, 18, 55, -28, 0x3D518F5D);
            folha(canvas, 304, 75, 43, 35, 0x617BB583);
            folha(canvas, 12, h - 86, 36, 22, 0x33518F5D);
            folha(canvas, 280, h - 68, 58, -24, 0x38518F5D);
            canvas.restore();
        }
        private void folha(Canvas canvas, float x, float y, float size, float angle, int color) {
            canvas.save();
            canvas.translate(x, y);
            canvas.scale(size / 60, size / 60);
            canvas.rotate(angle, 30, 30);
            Path leaf = new Path();
            leaf.moveTo(8, 49); leaf.cubicTo(8, 18, 26, 4, 53, 7);
            leaf.cubicTo(51, 34, 34, 53, 8, 49); leaf.close();
            paint.setColor(color); paint.setStyle(Paint.Style.FILL);
            canvas.drawPath(leaf, paint);
            paint.setStrokeWidth(2); paint.setColor(0x40315C3C);
            canvas.drawLine(9, 50, 43, 15, paint);
            canvas.restore();
        }
        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
