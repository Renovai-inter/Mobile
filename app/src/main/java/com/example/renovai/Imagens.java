package com.example.renovai;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import java.lang.ref.WeakReference;

/** Memória → disco (original e tamanhos derivados) → URL HTTPS. */
public final class Imagens {
    private Imagens() {}

    public static void carregar(ImageView view, String url, boolean miniatura) {
        carregar(view, url, miniatura, TipoImagem.OUTRA);
    }

    public static void carregar(ImageView view, String url, boolean miniatura, TipoImagem tipo) {
        Object binding = new Object();
        view.setTag(R.id.tag_imagem_binding, binding);
        carregar(view, url, miniatura, binding, descricao(tipo), tipo);
    }

    public static void coleta(ImageView view, String coletaId, String url, boolean miniatura) {
        Object binding = new Object();
        view.setTag(R.id.tag_imagem_binding, binding);
        carregar(view, url, miniatura, binding, R.string.foto_coleta, TipoImagem.COLETA);
        if (ImagemUrl.normalizar(url) != null) {
            FotoColetaRepository.get(view.getContext()).guardar(coletaId, url);
        } else if (coletaId != null && !coletaId.trim().isEmpty()) {
            WeakReference<ImageView> target = new WeakReference<>(view);
            FotoColetaRepository.get(view.getContext()).resolver(coletaId, (resolved, falhou) -> {
                ImageView image = target.get();
                if (image == null || image.getTag(R.id.tag_imagem_binding) != binding
                        || !vivo(image.getContext())) return;
                carregar(image, resolved, miniatura, binding, R.string.foto_coleta, TipoImagem.COLETA);
                if (falhou) image.setContentDescription(image.getContext().getString(R.string.foto_indisponivel));
            });
        }
    }

    private static int descricao(TipoImagem tipo) {
        switch (tipo) {
            case COLETA: return R.string.foto_coleta;
            case TRIAGEM: return R.string.foto_triagem;
            case PERFIL: return R.string.foto_perfil;
            default: return R.string.imagem;
        }
    }

    private static void carregar(ImageView view, String url, boolean miniatura, Object binding, int descricao, TipoImagem tipo) {
        if (!vivo(view.getContext())) return;
        Glide.with(view).clear(view);
        view.setImageResource(R.drawable.pedido_foto_placeholder);
        String model = ImagemUrl.normalizar(url);
        view.setContentDescription(view.getContext().getString(
                model == null ? (tipo == TipoImagem.COLETA ? R.string.coleta_sem_foto
                        : tipo == TipoImagem.TRIAGEM ? R.string.triagem_sem_foto : R.string.sem_imagem)
                        : R.string.carregando_foto));
        if (model == null) return;
        RequestBuilder<Drawable> request = Glide.with(view).load(model)
                .signature(new com.bumptech.glide.signature.ObjectKey(tipo.chaveCache()))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.pedido_foto_placeholder)
                .error(R.drawable.pedido_foto_placeholder)
                .listener(new RequestListener<Drawable>() {
                    @Override public boolean onLoadFailed(GlideException e, Object model,
                            Target<Drawable> target, boolean first) {
                        if (view.getTag(R.id.tag_imagem_binding) == binding)
                            view.setContentDescription(view.getContext().getString(R.string.foto_indisponivel));
                        return false;
                    }
                    @Override public boolean onResourceReady(Drawable resource, Object model,
                            Target<Drawable> target, DataSource source, boolean first) {
                        if (view.getTag(R.id.tag_imagem_binding) == binding)
                            view.setContentDescription(view.getContext().getString(descricao));
                        return false;
                    }
                });
        (miniatura ? request.centerCrop() : request.fitCenter()).into(view);
    }

    public static void limpar(ImageView view) {
        view.setTag(R.id.tag_imagem_binding, null);
        if (vivo(view.getContext())) Glide.with(view).clear(view);
        view.setImageResource(R.drawable.pedido_foto_placeholder);
    }

    public static void preparar(Context context, String url, TipoImagem tipo) {
        String model = ImagemUrl.normalizar(url);
        if (model != null) Glide.with(context.getApplicationContext()).downloadOnly()
                .load(model).signature(new com.bumptech.glide.signature.ObjectKey(tipo.chaveCache()))
                .diskCacheStrategy(DiskCacheStrategy.DATA).preload();
    }

    private static boolean vivo(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity)
                return !((Activity) context).isFinishing() && !((Activity) context).isDestroyed();
            Context base = ((ContextWrapper) context).getBaseContext();
            if (base == context) break;
            context = base;
        }
        return true;
    }
}
