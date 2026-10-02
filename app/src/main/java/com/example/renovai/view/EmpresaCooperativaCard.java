package com.example.renovai.view;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.renovai.EmpresaData;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.dto.request.EmpresaRequests;
import com.example.renovai.dto.response.EmpresaResponses;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

/**
 * Preenche o cartão de cooperativa (item_empresa_cooperativa_busca) usado em Busca, Favoritas e
 * Home.
 */
final class EmpresaCooperativaCard {

    private EmpresaCooperativaCard() {}

    static void bind(
            Activity a,
            View v,
            String cooperativaId,
            String nome,
            Double nota,
            boolean favoritada,
            Runnable aoMudarFavorito) {
        ((TextView) v.findViewById(R.id.txtNomeCoopBusca))
                .setText(nome == null ? "Cooperativa" : nome);
        ((TextView) v.findViewById(R.id.txtNotaCoop))
                .setText(
                        nota == null
                                ? "Sem avaliações"
                                : "Nota: " + String.format(GestorUi.BR, "%.1f", nota) + "/5");
        ImageView estrela = v.findViewById(R.id.imgEstrelaFav);
        estrela.setImageAlpha(favoritada ? 255 : 90);
        estrela.setOnClickListener(
                x -> alternarFavorito(a, cooperativaId, nome, favoritada, aoMudarFavorito));

        View.OnClickListener abrir = x -> abrirPerfilPublico(a, cooperativaId);
        v.setOnClickListener(abrir);
        v.findViewById(R.id.btnDetalhesCoop).setOnClickListener(abrir);
        v.findViewById(R.id.btnFazerPedidoCoop)
                .setOnClickListener(
                        x -> {
                            Intent i = new Intent(a, EmpresaEnviarPedidoActivity.class);
                            i.putExtra(
                                    EmpresaEnviarPedidoActivity.EXTRA_COOPERATIVA_ID,
                                    cooperativaId);
                            i.putExtra(EmpresaEnviarPedidoActivity.EXTRA_COOPERATIVA_NOME, nome);
                            a.startActivity(i);
                        });
    }

    static void abrirPerfilPublico(Activity a, String cooperativaId) {
        Intent i = new Intent(a, EmpresaPerfilPublicoActivity.class);
        i.putExtra(EmpresaPerfilPublicoActivity.EXTRA_COOPERATIVA_ID, cooperativaId);
        a.startActivity(i);
    }

    private static void alternarFavorito(
            Activity a,
            String cooperativaId,
            String nome,
            boolean eraFavoritada,
            Runnable aoMudar) {
        String empresaId = EmpresaData.empresaId();
        if (empresaId == null) return;
        if (eraFavoritada) {
            EmpresaData.api()
                    .desfavoritar(cooperativaId)
                    .enqueue(
                            new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> c, Response<Void> r) {
                                    if (a.isFinishing() || a.isDestroyed()) return;
                                    if (!r.isSuccessful()) {
                                        android.widget.Toast.makeText(
                                                        a,
                                                        EmpresaData.erro(r),
                                                        android.widget.Toast.LENGTH_LONG)
                                                .show();
                                        return;
                                    }
                                    EmpresaData.invalidar("empresa:favoritos", "empresa:dashboard");
                                    if (aoMudar != null) aoMudar.run();
                                }

                                @Override
                                public void onFailure(Call<Void> c, Throwable t) {
                                    android.widget.Toast.makeText(
                                                    a,
                                                    "Não foi possível alterar o favorito.",
                                                    android.widget.Toast.LENGTH_LONG)
                                            .show();
                                }
                            });
        } else {
            EmpresaData.api()
                    .favoritar(new EmpresaRequests.Favorito(cooperativaId))
                    .enqueue(
                            new Callback<EmpresaResponses.Favorito>() {
                                @Override
                                public void onResponse(
                                        Call<EmpresaResponses.Favorito> c,
                                        Response<EmpresaResponses.Favorito> r) {
                                    if (a.isFinishing() || a.isDestroyed()) return;
                                    if (!r.isSuccessful()) {
                                        android.widget.Toast.makeText(
                                                        a,
                                                        EmpresaData.erro(r),
                                                        android.widget.Toast.LENGTH_LONG)
                                                .show();
                                        return;
                                    }
                                    EmpresaData.invalidar("empresa:favoritos", "empresa:dashboard");
                                    if (aoMudar != null) aoMudar.run();
                                }

                                @Override
                                public void onFailure(
                                        Call<EmpresaResponses.Favorito> c, Throwable t) {
                                    android.widget.Toast.makeText(
                                                    a,
                                                    "Não foi possível alterar o favorito.",
                                                    android.widget.Toast.LENGTH_LONG)
                                            .show();
                                }
                            });
        }
    }

    static boolean estaFavoritada(List<EmpresaResponses.Favorito> favoritos, String cooperativaId) {
        if (favoritos == null) return false;
        for (EmpresaResponses.Favorito f : favoritos)
            if (cooperativaId.equalsIgnoreCase(f.cooperativaId)) return true;
        return false;
    }
}
