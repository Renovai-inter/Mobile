package com.example.renovai.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.dto.response.GestorResponses;

import java.util.ArrayList;
import java.util.List;

/** Preenche o cartão de rota (item_motorista_rota) usado nas telas Home e Rotas da semana. */
final class MotoristaRotaCard {

    private MotoristaRotaCard() {}

    private static List<GestorResponses.RotaEnd> ordenadas(GestorResponses.Rota r) {
        List<GestorResponses.RotaEnd> l =
                r.enderecos == null ? new ArrayList<>() : new ArrayList<>(r.enderecos);
        l.sort((a, b) -> (a.ordem == null ? 0 : a.ordem) - (b.ordem == null ? 0 : b.ordem));
        return l;
    }

    static void bind(
            Context c,
            View v,
            GestorResponses.Rota r,
            boolean iniciada,
            boolean concluida,
            Runnable aoAbrir) {
        List<GestorResponses.RotaEnd> ends = ordenadas(r);
        ((TextView) v.findViewById(R.id.txtRotaMNome)).setText(r.nome == null ? "Rota" : r.nome);
        ((TextView) v.findViewById(R.id.txtRotaMOrigem))
                .setText(ends.isEmpty() ? "—" : GestorRotaTexto.curto(ends.get(0)));
        ((TextView) v.findViewById(R.id.txtRotaMDestino))
                .setText(ends.size() < 2 ? "—" : GestorRotaTexto.curto(ends.get(ends.size() - 1)));

        TextView badge = v.findViewById(R.id.txtRotaMBadge);
        TextView sub = v.findViewById(R.id.txtRotaMSub);
        ProgressBar barra = v.findViewById(R.id.barRotaM);
        TextView progresso = v.findViewById(R.id.txtRotaMProgresso);

        GestorUi.badge(badge, concluida ? GestorUi.OUTRO : GestorUi.EM_NEGOCIACAO);
        badge.setText(concluida ? "Inativa" : "Disponível");
        badge.setCompoundDrawables(null, null, null, null);
        sub.setText("Rota da cooperativa");
        barra.setVisibility(View.GONE);
        progresso.setVisibility(View.GONE);

        GestorUi.borda(
                v.findViewById(R.id.cardRotaM),
                concluida ? Color.parseColor("#CCCCCC") : Color.parseColor("#6FAE76"));
        ((TextView) v.findViewById(R.id.txtRotaMResumo))
                .setText(ends.size() + (ends.size() == 1 ? " parada" : " paradas"));

        View.OnClickListener abrir = x -> aoAbrir.run();
        v.setOnClickListener(abrir);
        v.findViewById(R.id.txtRotaMDetalhes).setOnClickListener(abrir);
    }

    static void abrirDetalhe(android.app.Activity a, String rotaId) {
        Intent i = new Intent(a, MotoristaRotaDetalheActivity.class);
        i.putExtra(MotoristaRotaDetalheActivity.EXTRA_ROTA_ID, rotaId);
        a.startActivity(i);
    }
}
