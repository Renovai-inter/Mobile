package com.example.renovai.view;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.renovai.EmpresaData;
import com.example.renovai.GestorUi;
import com.example.renovai.Imagens;
import com.example.renovai.R;
import com.example.renovai.TipoImagem;
import com.example.renovai.dto.request.EmpresaRequests;
import com.example.renovai.dto.response.EmpresaResponses;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

/**
 * Preenche o cartão de cooperativa (item_empresa_cooperativa_busca) usado em Busca e Favoritas, e
 * cuida da estrela de favoritar (também usada no perfil público da cooperativa).
 */
final class EmpresaCooperativaCard {

    private EmpresaCooperativaCard() {}

    static void bind(
            Activity a,
            View v,
            String cooperativaId,
            String nome,
            String imagemUrl,
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
        foto(v.findViewById(R.id.imgCoopBusca), imagemUrl);
        estrela(v.findViewById(R.id.imgEstrelaFav), cooperativaId, favoritada, aoMudarFavorito);

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

    /**
     * Detalhes da cooperativa no cartão: descrição curta e uma linha para cada dado que existir
     * (cidade, horário, contato preferido e número de cooperados). Rótulos em negrito.
     */
    static void detalhes(
            View card,
            String descricao,
            String cidade,
            String horario,
            String contatoPreferencial,
            Integer cooperados) {
        TextView txtDescricao = card.findViewById(R.id.txtCoopDescricaoE);
        boolean temDescricao = descricao != null && !descricao.trim().isEmpty();
        txtDescricao.setVisibility(temDescricao ? View.VISIBLE : View.GONE);
        if (temDescricao) txtDescricao.setText(descricao.trim());

        android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
        linha(sb, "Cidade", cidade);
        linha(sb, "Horário", horario);
        linha(sb, "Contato", contato(contatoPreferencial));
        linha(sb, "Cooperados", cooperados == null || cooperados <= 0 ? null : String.valueOf(cooperados));
        ((TextView) card.findViewById(R.id.txtCoopDetalhesE))
                .setText(sb.length() == 0 ? "Detalhes ainda não informados." : sb);
    }

    private static void linha(android.text.SpannableStringBuilder sb, String rotulo, String valor) {
        if (valor == null || valor.trim().isEmpty()) return;
        if (sb.length() > 0) sb.append('\n');
        int ini = sb.length();
        sb.append(rotulo).append(": ");
        sb.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                ini, sb.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(valor.trim());
    }

    /** "WHATSAPP" → "WhatsApp" etc. (a API guarda o canal preferido, não o número). */
    private static String contato(String canal) {
        if (canal == null || canal.trim().isEmpty()) return null;
        String c = canal.trim().toUpperCase(java.util.Locale.ROOT);
        if (c.contains("WHATS")) return "WhatsApp";
        if (c.contains("MAIL")) return "E-mail";
        if (c.contains("TEL") || c.contains("LIGA")) return "Telefone";
        String t = canal.trim().toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    /** Foto da cooperativa (Cloudinary); sem foto fica o fundo padrão. */
    static void foto(ImageView img, String imagemUrl) {
        Imagens.carregar(img, imagemUrl, true, TipoImagem.COOPERATIVA);
    }

    static void abrirPerfilPublico(Activity a, String cooperativaId) {
        Intent i = new Intent(a, EmpresaPerfilPublicoActivity.class);
        i.putExtra(EmpresaPerfilPublicoActivity.EXTRA_COOPERATIVA_ID, cooperativaId);
        a.startActivity(i);
    }

    /** Desenha a estrela cheia (favoritada) ou só o contorno. */
    static void desenharEstrela(ImageView estrela, boolean favoritada) {
        estrela.setImageResource(favoritada ? R.drawable.ic_estrela_cheia : R.drawable.ic_estrela_vazia);
        estrela.setContentDescription(favoritada ? "Remover dos favoritos" : "Favoritar");
    }

    /**
     * Liga a estrela: cada toque preenche ou esvazia na hora e põe ou tira a cooperativa dos
     * favoritos na API. Se a API recusar, a estrela volta como estava. Toques enquanto a chamada
     * anterior não voltou são ignorados (evita favoritar duas vezes).
     */
    static void estrela(ImageView estrela, String cooperativaId, boolean favoritada, Runnable aoMudar) {
        boolean[] estado = {favoritada};
        boolean[] enviando = {false};
        desenharEstrela(estrela, favoritada);
        estrela.setOnClickListener(x -> {
            if (enviando[0] || cooperativaId == null || EmpresaData.empresaId() == null) return;
            boolean novo = !estado[0];
            estado[0] = novo;
            enviando[0] = true;
            desenharEstrela(estrela, novo);
            alternarFavorito(cooperativaId, novo, ok -> {
                enviando[0] = false;
                if (!estrela.isAttachedToWindow()) return;
                if (!ok) {
                    estado[0] = !novo;
                    desenharEstrela(estrela, !novo);
                    return;
                }
                Toast.makeText(estrela.getContext(),
                        novo ? "Adicionada aos favoritos." : "Removida dos favoritos.",
                        Toast.LENGTH_SHORT).show();
                if (aoMudar != null) aoMudar.run();
            }, estrela);
        });
    }

    private interface Resultado {
        void fim(boolean ok);
    }

    private static void alternarFavorito(String cooperativaId, boolean favoritar, Resultado r, View v) {
        Call<?> call = favoritar
                ? EmpresaData.api().favoritar(new EmpresaRequests.Favorito(cooperativaId))
                : EmpresaData.api().desfavoritar(cooperativaId);
        enfileirar(call, r, v);
    }

    private static <T> void enfileirar(Call<T> call, Resultado r, View v) {
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> c, Response<T> resp) {
                if (resp.isSuccessful()) {
                    EmpresaData.invalidar("empresa:favoritos", "empresa:dashboard");
                    r.fim(true);
                } else {
                    if (v.isAttachedToWindow())
                        Toast.makeText(v.getContext(), EmpresaData.erro(resp), Toast.LENGTH_LONG).show();
                    r.fim(false);
                }
            }

            @Override
            public void onFailure(Call<T> c, Throwable t) {
                if (v.isAttachedToWindow())
                    Toast.makeText(v.getContext(), "Não foi possível alterar o favorito.",
                            Toast.LENGTH_LONG).show();
                r.fim(false);
            }
        });
    }

    static boolean estaFavoritada(List<EmpresaResponses.Favorito> favoritos, String cooperativaId) {
        if (favoritos == null || cooperativaId == null) return false;
        for (EmpresaResponses.Favorito f : favoritos)
            if (cooperativaId.equalsIgnoreCase(f.cooperativaId)) return true;
        return false;
    }
}
