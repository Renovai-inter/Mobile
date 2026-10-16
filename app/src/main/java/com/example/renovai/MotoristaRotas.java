package com.example.renovai;

import com.example.renovai.dto.response.GestorResponses;

import java.util.ArrayList;
import java.util.List;

/** O cadastro SQL informa disponibilidade, não o andamento de uma viagem. */
public final class MotoristaRotas {
    private MotoristaRotas() {}

    public static class Grupo {
        public final List<GestorResponses.Rota> disponiveis = new ArrayList<>();
        public final List<GestorResponses.Rota> inativas = new ArrayList<>();
    }

    public static Grupo classificar(List<GestorResponses.Rota> rotas) {
        Grupo g = new Grupo();
        for (GestorResponses.Rota r : rotas) {
            if (Boolean.TRUE.equals(r.estaAtiva)) g.disponiveis.add(r);
            else if (Boolean.FALSE.equals(r.estaAtiva)) g.inativas.add(r);
        }
        return g;
    }
}
