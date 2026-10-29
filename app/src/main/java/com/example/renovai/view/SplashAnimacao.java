package com.example.renovai.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.renovai.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Animação da splash (prévia: artifact "Splash do Renovaí"). Total de 3,5 s:
 *
 * <pre>
 *    0 ms  brilho cresce; cabeça do guaxinim pula; nariz faz "boop"   (avd_logo_splash)
 *  300 ms  rabo gira e os 6 gomos formam a seta de reciclagem         (avd_logo_splash)
 *  500 ms  folhinhas sobem no fundo (até 2,9 s)
 *  750 ms  letras de "Renovaí" sobem; a folha pousa no "í"            (avd_nome_splash)
 * 1100 ms  onda lima se espalha
 * 1450 ms  piscada (e outra em 2,58 s)                                (avd_logo_splash)
 * 1900 ms  slogan sobe e aparece
 * 3180 ms  tudo sobe e some
 * 3500 ms  fim → aoTerminar
 * </pre>
 *
 * Com as animações do sistema desligadas (acessibilidade / "Escala de animação" = 0), mostra a
 * tela pronta e termina em 0,8 s.
 */
final class SplashAnimacao {

    static final long DURACAO_TOTAL = 3500;
    private static final long DURACAO_SEM_ANIMACAO = 800;
    private static final int[] CORES_FOLHA = {0xFFD4E699, 0xFF79B581, 0xD9FAFAF8};

    private final View raiz;
    private final FrameLayout folhas;
    private final ImageView halo, anel, logo, nome, slogan;
    private final LinearLayout palco;
    private final Runnable aoTerminar;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Animator> animadores = new ArrayList<>();
    private final Random random = new Random();
    private final Interpolator overshoot, desacelera, onda;
    private boolean terminou;

    SplashAnimacao(View raiz, Runnable aoTerminar) {
        this.raiz = raiz;
        this.aoTerminar = aoTerminar;
        folhas = raiz.findViewById(R.id.splashFolhas);
        halo = raiz.findViewById(R.id.splashHalo);
        anel = raiz.findViewById(R.id.splashAnel);
        palco = raiz.findViewById(R.id.splashPalco);
        logo = raiz.findViewById(R.id.logo);
        nome = raiz.findViewById(R.id.renovai_estilizado);
        slogan = raiz.findViewById(R.id.transforme_texto);
        overshoot = AnimationUtils.loadInterpolator(raiz.getContext(), R.interpolator.splash_overshoot);
        desacelera = AnimationUtils.loadInterpolator(raiz.getContext(), R.interpolator.splash_desacelera);
        onda = AnimationUtils.loadInterpolator(raiz.getContext(), R.interpolator.splash_onda);
    }

    /** Espera a tela ter tamanho, ajusta as proporções e começa. */
    void iniciar() {
        raiz.post(() -> {
            if (terminou) return;
            dimensionar(raiz.getWidth(), raiz.getHeight());
            palco.post(this::tocar);
        });
    }

    /** Para tudo (a Activity foi destruída antes do fim). */
    void cancelar() {
        terminou = true;
        handler.removeCallbacksAndMessages(null);
        List<Animator> todos = new ArrayList<>(animadores);
        animadores.clear();
        for (Animator a : todos) a.cancel();
    }

    // ── layout proporcional à largura (igual à prévia) ──

    private void dimensionar(int w, int h) {
        tamanho(logo, Math.round(w * 0.31f), 107f / 100f, 0);
        // o viewport do nome tem folga em cima para a folha cair; sobe 7% para encostar no mascote
        tamanho(nome, Math.round(w * 0.84f), 108f / 270f, -Math.round(w * 0.07f));
        tamanho(slogan, Math.round(w * 0.68f), 13f / 225f, Math.round(w * 0.03f));
        palco.setPadding(0, 0, 0, Math.round(h * 0.12f));
        int ladoHalo = Math.round(w * 0.92f), ladoAnel = Math.round(w * 0.46f);
        tamanhoQuadrado(halo, ladoHalo);
        tamanhoQuadrado(anel, ladoAnel);
    }

    private static void tamanho(View v, int largura, float proporcao, int margemTopo) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) v.getLayoutParams();
        lp.width = largura;
        lp.height = Math.round(largura * proporcao);
        lp.topMargin = margemTopo;
        v.setLayoutParams(lp);
    }

    private static void tamanhoQuadrado(View v, int lado) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        lp.width = lado;
        lp.height = lado;
        v.setLayoutParams(lp);
    }

    /** Halo e onda ficam centralizados no mascote. */
    private void centralizarNoMascote() {
        float cx = palco.getLeft() + logo.getLeft() + logo.getWidth() / 2f;
        float cy = palco.getTop() + logo.getTop() + logo.getHeight() / 2f;
        for (View v : new View[] {halo, anel}) {
            v.setX(cx - v.getWidth() / 2f);
            v.setY(cy - v.getHeight() / 2f);
        }
    }

    // ── linha do tempo ──

    private void tocar() {
        if (terminou) return;
        centralizarNoMascote();
        iniciarVetor(logo);
        iniciarVetor(nome);

        if (!ValueAnimator.areAnimatorsEnabled()) {
            halo.setAlpha(1f);
            slogan.setAlpha(1f);
            depois(DURACAO_SEM_ANIMACAO, this::terminar);
            return;
        }

        float dp = raiz.getResources().getDisplayMetrics().density;

        // brilho: cresce com "pulo" e depois pulsa devagar
        halo.setScaleX(0.2f);
        halo.setScaleY(0.2f);
        Animator haloEntra = junto(800, 0, overshoot,
                ObjectAnimator.ofFloat(halo, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(halo, View.SCALE_X, 0.2f, 1f),
                ObjectAnimator.ofFloat(halo, View.SCALE_Y, 0.2f, 1f));
        iniciar(haloEntra);
        depois(800, () -> iniciar(repetir(1300, ObjectAnimator.ofPropertyValuesHolder(halo,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.08f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.08f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.75f)))));

        // onda lima
        ObjectAnimator ondaAnim = ObjectAnimator.ofPropertyValuesHolder(anel,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.7f, 2.1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.7f, 2.1f),
                PropertyValuesHolder.ofKeyframe(View.ALPHA,
                        Keyframe.ofFloat(0f, 0f), Keyframe.ofFloat(0.15f, 0.85f), Keyframe.ofFloat(1f, 0f)));
        ondaAnim.setDuration(900);
        ondaAnim.setStartDelay(1100);
        ondaAnim.setInterpolator(onda);
        iniciar(ondaAnim);

        // mascote flutua
        depois(1500, () -> iniciar(repetir(1100,
                ObjectAnimator.ofFloat(logo, View.TRANSLATION_Y, 0f, -5 * dp))));

        // slogan
        slogan.setTranslationY(14 * dp);
        iniciar(junto(520, 1900, desacelera,
                ObjectAnimator.ofFloat(slogan, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(slogan, View.TRANSLATION_Y, 14 * dp, 0f)));

        // folhinhas
        for (long t = 500; t < 2900; t += 230) depois(t, this::soltarFolha);

        depois(3180, this::sair);
        depois(DURACAO_TOTAL, this::terminar);
    }

    private void sair() {
        // para os loops (brilho e flutuação) antes da saída
        for (Animator a : new ArrayList<>(animadores)) {
            if (a instanceof ValueAnimator && ((ValueAnimator) a).getRepeatCount() == ValueAnimator.INFINITE) {
                a.cancel();
                animadores.remove(a);
            }
        }
        float sobe = -18 * raiz.getResources().getDisplayMetrics().density;
        Interpolator acelera = new AccelerateInterpolator();
        List<Animator> saida = new ArrayList<>();
        for (View v : new View[] {logo, nome, slogan}) {
            saida.add(ObjectAnimator.ofFloat(v, View.ALPHA, v.getAlpha(), 0f));
            saida.add(ObjectAnimator.ofFloat(v, View.TRANSLATION_Y, v.getTranslationY(), v.getTranslationY() + sobe));
        }
        saida.add(ObjectAnimator.ofFloat(folhas, View.ALPHA, 1f, 0f));
        saida.add(ObjectAnimator.ofFloat(halo, View.ALPHA, halo.getAlpha(), 0f));
        saida.add(ObjectAnimator.ofFloat(halo, View.SCALE_X, halo.getScaleX(), 1.8f));
        saida.add(ObjectAnimator.ofFloat(halo, View.SCALE_Y, halo.getScaleY(), 1.8f));
        iniciar(junto(320, 0, acelera, saida.toArray(new Animator[0])));
    }

    private void soltarFolha() {
        if (terminou) return;
        int w = folhas.getWidth(), h = folhas.getHeight();
        if (w == 0 || h == 0) return;
        float u = w / 320f; // a prévia usa um celular de ~320 px de largura
        int tam = Math.round((10 + random.nextFloat() * 16) * u);
        float x = random.nextFloat() * (w - tam);
        float bal = (18 + random.nextFloat() * 30) * u * (random.nextBoolean() ? 1 : -1);
        float yFinal = h * (0.15f + random.nextFloat() * 0.35f);
        float giro0 = random.nextFloat() * 360;
        float giro = (120 + random.nextFloat() * 240) * (random.nextBoolean() ? 1 : -1);
        float opac = 0.35f + random.nextFloat() * 0.45f;
        float y0 = h + tam;

        ImageView folha = new ImageView(raiz.getContext());
        Drawable d = raiz.getContext().getDrawable(R.drawable.splash_folha);
        folha.setImageDrawable(d);
        folha.setImageTintList(ColorStateList.valueOf(CORES_FOLHA[random.nextInt(CORES_FOLHA.length)]));
        folha.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        folhas.addView(folha, new FrameLayout.LayoutParams(tam, tam));

        ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(folha,
                PropertyValuesHolder.ofKeyframe(View.TRANSLATION_X,
                        Keyframe.ofFloat(0f, x), Keyframe.ofFloat(0.3f, x + bal),
                        Keyframe.ofFloat(0.7f, x - bal * 0.6f), Keyframe.ofFloat(1f, x + bal * 0.3f)),
                PropertyValuesHolder.ofKeyframe(View.TRANSLATION_Y,
                        Keyframe.ofFloat(0f, y0), Keyframe.ofFloat(0.3f, y0 - (y0 - yFinal) * 0.38f),
                        Keyframe.ofFloat(0.7f, y0 - (y0 - yFinal) * 0.8f), Keyframe.ofFloat(1f, yFinal)),
                PropertyValuesHolder.ofKeyframe(View.ROTATION,
                        Keyframe.ofFloat(0f, giro0), Keyframe.ofFloat(0.3f, giro0 + giro * 0.3f),
                        Keyframe.ofFloat(0.7f, giro0 + giro * 0.65f), Keyframe.ofFloat(1f, giro0 + giro)),
                PropertyValuesHolder.ofKeyframe(View.ALPHA,
                        Keyframe.ofFloat(0f, 0f), Keyframe.ofFloat(0.3f, opac),
                        Keyframe.ofFloat(0.7f, opac), Keyframe.ofFloat(1f, 0f)));
        a.setDuration(2600 + random.nextInt(1600));
        a.setInterpolator(new LinearInterpolator());
        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                folhas.removeView(folha);
                animadores.remove(animation);
            }
        });
        iniciar(a);
    }

    private void terminar() {
        if (terminou) return;
        terminou = true;
        aoTerminar.run();
    }

    // ── utilitários ──

    private static void iniciarVetor(ImageView v) {
        Drawable d = v.getDrawable();
        if (d instanceof Animatable) ((Animatable) d).start();
    }

    private void depois(long ms, Runnable r) {
        handler.postDelayed(() -> {
            if (!terminou) r.run();
        }, ms);
    }

    private void iniciar(Animator a) {
        animadores.add(a);
        a.start();
    }

    private static Animator junto(long duracao, long atraso, Interpolator interp, Animator... partes) {
        AnimatorSet s = new AnimatorSet();
        s.playTogether(partes);
        s.setDuration(duracao);
        s.setStartDelay(atraso);
        s.setInterpolator(interp);
        return s;
    }

    private static ObjectAnimator repetir(long duracao, ObjectAnimator a) {
        a.setDuration(duracao);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        return a;
    }
}
