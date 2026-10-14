package com.example.renovai;

import android.app.Activity;
import android.os.CancellationSignal;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

/**
 * "Entrar com Google" via Credential Manager. Abre a janela de contas Google do aparelho e devolve
 * o ID token, que o app manda para a API (POST /auth/google) — ver AuthController.loginGoogle.
 *
 * <p>O server client id é o "ID do cliente da Web" do projeto Firebase. O plugin google-services
 * gera o recurso default_web_client_id a partir do app/google-services.json; sem esse arquivo o
 * botão avisa que o login com Google não está configurado.
 */
public final class GoogleLogin {

    private static final String TAG = "GoogleLogin";

    public interface Callback {
        void onIdToken(String idToken);

        /** Erro para mostrar ao usuário. Não é chamado quando a pessoa só fecha a janela. */
        void onErro(String mensagem);

        /** A pessoa fechou a janela de contas sem escolher nenhuma. */
        void onCancelado();
    }

    private GoogleLogin() {}

    /** ID do cliente da Web gerado pelo google-services, ou null se o app foi compilado sem ele. */
    public static String serverClientId(Activity activity) {
        int id =
                activity.getResources()
                        .getIdentifier("default_web_client_id", "string", activity.getPackageName());
        return id == 0 ? null : activity.getString(id);
    }

    public static void entrar(Activity activity, Callback callback) {
        String clientId = serverClientId(activity);
        if (clientId == null || clientId.isEmpty()) {
            callback.onErro("O login com Google não está configurado neste app.");
            return;
        }

        GetSignInWithGoogleOption opcao = new GetSignInWithGoogleOption.Builder(clientId).build();
        GetCredentialRequest pedido =
                new GetCredentialRequest.Builder().addCredentialOption(opcao).build();

        CredentialManager.create(activity)
                .getCredentialAsync(
                        activity,
                        pedido,
                        new CancellationSignal(),
                        ContextCompat.getMainExecutor(activity),
                        new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                            @Override
                            public void onResult(GetCredentialResponse resposta) {
                                Credential credencial = resposta.getCredential();
                                if (credencial instanceof CustomCredential
                                        && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                                .equals(credencial.getType())) {
                                    try {
                                        GoogleIdTokenCredential google =
                                                GoogleIdTokenCredential.createFrom(credencial.getData());
                                        callback.onIdToken(google.getIdToken());
                                    } catch (Exception e) {
                                        Log.w(TAG, "Credencial do Google inválida", e);
                                        callback.onErro("Não foi possível ler a conta Google.");
                                    }
                                } else {
                                    callback.onErro("Tipo de credencial inesperado.");
                                }
                            }

                            @Override
                            public void onError(GetCredentialException e) {
                                if (e instanceof GetCredentialCancellationException) {
                                    callback.onCancelado();
                                    return;
                                }
                                Log.w(TAG, "Falha no login com Google", e);
                                if (e instanceof NoCredentialException) {
                                    callback.onErro(
                                            "Nenhuma conta Google neste aparelho. Adicione uma conta nas configurações do Android.");
                                } else {
                                    callback.onErro("Não foi possível entrar com o Google.");
                                }
                            }
                        });
    }
}
