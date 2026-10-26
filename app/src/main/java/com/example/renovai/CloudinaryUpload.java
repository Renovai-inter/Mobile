package com.example.renovai;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import okio.BufferedSink;

/** Upload unsigned: o segredo da conta nunca faz parte do APK. */
public final class CloudinaryUpload {
    private CloudinaryUpload() {}
    public interface Retorno { void sucesso(String url); void erro(String mensagem); }
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS).build();
    public static void enviar(Context context, Uri uri, TipoImagem categoria, Retorno retorno) {
        Handler main = new Handler(Looper.getMainLooper());
        if (!estaConfigurado()) {
            main.post(() -> retorno.erro("Envio de fotos não configurado (Cloudinary)."));
            return;
        }
        if (uri == null) {
            main.post(() -> retorno.erro("Selecione uma imagem antes de enviar."));
            return;
        }
        Context app = context.getApplicationContext();
        String mime = app.getContentResolver().getType(uri);
        if (mime != null && !mime.startsWith("image/")) {
            main.post(() -> retorno.erro("Selecione um arquivo de imagem."));
            return;
        }
        final MediaType tipo = MediaType.parse(mime == null ? "application/octet-stream" : mime);
        RequestBody arquivo = new RequestBody() {
            @Override public MediaType contentType() { return tipo; }
            @Override public void writeTo(BufferedSink sink) throws IOException {
                try (InputStream in = app.getContentResolver().openInputStream(uri)) {
                    if (in == null) throw new IOException("Imagem indisponível");
                    byte[] buffer = new byte[8192]; int n;
                    while ((n = in.read(buffer)) != -1) sink.write(buffer, 0, n);
                } catch (SecurityException e) {
                    throw new IOException("Selecione novamente a imagem para permitir a leitura.", e);
                }
            }
        };
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", "imagem", arquivo)
                .addFormDataPart("upload_preset", BuildConfig.CLOUDINARY_UPLOAD_PRESET)
                .addFormDataPart("folder", categoria.pastaCloudinary()).build();
        Request request = new Request.Builder().url("https://api.cloudinary.com/v1_1/"
                + Uri.encode(BuildConfig.CLOUDINARY_CLOUD_NAME) + "/image/upload").post(body).build();
        HTTP.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                main.post(() -> retorno.erro("Não foi possível enviar a foto. Verifique a conexão e tente novamente."));
            }
            @Override public void onResponse(Call call, Response response) {
                try (Response r = response) {
                    if (!r.isSuccessful()) {
                        String mensagem;
                        switch (r.code()) {
                            case 400: mensagem = "Cloudinary recusou a imagem. Verifique se o preset existe e permite uploads unsigned."; break;
                            case 401: case 403: mensagem = "Cloudinary não autorizou o upload. Verifique o cloud name e o preset unsigned."; break;
                            case 413: mensagem = "A imagem excede o tamanho permitido pelo Cloudinary."; break;
                            case 429: mensagem = "Limite de uploads atingido. Tente novamente mais tarde."; break;
                            default: mensagem = "Cloudinary indisponível (HTTP " + r.code() + "). Tente novamente.";
                        }
                        main.post(() -> retorno.erro(mensagem));
                        return;
                    }
                    if (r.body() == null) throw new IOException("Resposta vazia");
                    JsonObject json = JsonParser.parseString(r.body().string()).getAsJsonObject();
                    String url = json.get("secure_url").getAsString();
                    Uri destino = Uri.parse(url);
                    if (!"https".equals(destino.getScheme()) || !"res.cloudinary.com".equals(destino.getHost()))
                        throw new IOException("URL inválida");
                    main.post(() -> {
                        Imagens.preparar(app, url, categoria);
                        retorno.sucesso(url);
                    });
                } catch (Exception e) {
                    main.post(() -> retorno.erro("Cloudinary não confirmou o envio da foto. Tente novamente."));
                }
            }
        });
    }
    /** cloudinary.cloudName e cloudinary.uploadPreset no local.properties (ver app/build.gradle.kts). */
    public static boolean estaConfigurado() {
        return valido(BuildConfig.CLOUDINARY_CLOUD_NAME) && valido(BuildConfig.CLOUDINARY_UPLOAD_PRESET);
    }

    /** Vazio ou ainda com o valor de exemplo ("SEU_...") conta como não configurado. */
    private static boolean valido(String v) {
        return v != null && !v.isEmpty() && !v.startsWith("SEU_");
    }
}
