package com.example.renovai;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.util.HashMap;
import java.util.Map;

/**
 * Upload de fotos para o Cloudinary (unsigned upload preset).
 *
 * <p>Fluxo: foto do aparelho → Cloudinary → secure_url → enviada no campo imagemUrl que a API já
 * aceita (ex.: POST /coletas). A API não precisa mudar.
 *
 * <p>cloud name e upload preset vêm do local.properties (ver app/build.gradle.kts):
 *
 * <pre>
 * cloudinary.cloudName=SEU_CLOUD_NAME
 * cloudinary.uploadPreset=SEU_UPLOAD_PRESET
 * </pre>
 */
public final class CloudinaryUploader {

    private static final String TAG = "CloudinaryUploader";
    private static boolean iniciado;

    public interface Callback {
        void onSucesso(String url);

        void onErro(String mensagem);
    }

    private CloudinaryUploader() {}

    /** Chamado uma única vez, em RenovaiApplication.onCreate(). */
    public static void init(Context context) {
        if (!estaConfigurado()) {
            Log.w(TAG, "Cloudinary não configurado: preencha cloudinary.* no local.properties");
            return;
        }
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
            config.put("secure", true);
            MediaManager.init(context, config);
            iniciado = true;
        } catch (IllegalStateException jaIniciado) {
            iniciado = true;
        }
    }

    public static boolean estaConfigurado() {
        return valido(BuildConfig.CLOUDINARY_CLOUD_NAME)
                && valido(BuildConfig.CLOUDINARY_UPLOAD_PRESET);
    }

    /** Vazio ou ainda com o valor de exemplo ("SEU_...") conta como não configurado. */
    private static boolean valido(String v) {
        return v != null && !v.isEmpty() && !v.startsWith("SEU_");
    }

    /**
     * Envia uma foto e devolve a URL https pública dela.
     *
     * @param pasta subpasta dentro de "renovai/" (ex.: "coletas/{cooperativaId}")
     */
    public static void enviarFoto(Uri foto, String pasta, Callback cb) {
        if (!iniciado) {
            cb.onErro("Envio de fotos não configurado (Cloudinary).");
            return;
        }
        MediaManager.get()
                .upload(foto)
                .unsigned(BuildConfig.CLOUDINARY_UPLOAD_PRESET)
                .option("folder", "renovai/" + pasta)
                .option("resource_type", "image")
                .callback(
                        new UploadCallback() {
                            @Override
                            public void onStart(String requestId) {}

                            @Override
                            public void onProgress(String requestId, long bytes, long totalBytes) {}

                            @Override
                            public void onSuccess(String requestId, Map resultData) {
                                Object url = resultData.get("secure_url");
                                if (url != null) cb.onSucesso(url.toString());
                                else cb.onErro("O Cloudinary não devolveu a URL da foto.");
                            }

                            @Override
                            public void onError(String requestId, ErrorInfo error) {
                                Log.w(TAG, "Upload falhou: " + error.getDescription());
                                cb.onErro("Não foi possível enviar a foto: " + error.getDescription());
                            }

                            @Override
                            public void onReschedule(String requestId, ErrorInfo error) {
                                // Sem internet o SDK reagenda o envio. Como a coleta precisa da URL
                                // agora, cancelamos e avisamos — a pessoa tenta de novo.
                                MediaManager.get().cancelRequest(requestId);
                                cb.onErro("Sem conexão para enviar a foto. Tente novamente.");
                            }
                        })
                .dispatch();
    }
}
