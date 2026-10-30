package com.example.renovai.controller;

import java.util.HashSet;
import java.util.Set;

/**
 * Decide quando o motorista chegou à próxima parada, para o app concluí-la sozinho.
 *
 * <p>Conta como chegada estar a até {@link #RAIO_METROS} da parada em {@link #LEITURAS} posições
 * seguidas com precisão de até {@link #PRECISAO_MAX_METROS} — uma leitura só (ou um GPS ruim) não
 * conclui nada. Se o motorista desfizer a conclusão automática, aquela parada só volta a ser
 * concluída sozinha depois que ele se afastar {@link #SAIDA_METROS} (senão ela seria concluída de
 * novo na leitura seguinte).
 */
public final class ChegadaParada {

    public static final double RAIO_METROS = 80;
    public static final double SAIDA_METROS = 150;
    public static final float PRECISAO_MAX_METROS = 60;
    public static final int LEITURAS = 2;

    private int alvo = -1, leituras;
    private final Set<Integer> desfeitas = new HashSet<>();

    /**
     * Nova posição do motorista.
     *
     * @param parada índice da próxima parada (negativo: nenhuma)
     * @param distancia metros até ela
     * @param precisao precisão da posição em metros (0 = desconhecida)
     * @return true quando a parada deve ser concluída agora (só uma vez por chegada)
     */
    public boolean posicao(int parada, double distancia, float precisao) {
        if (parada < 0) {
            alvo = -1;
            leituras = 0;
            return false;
        }
        if (parada != alvo) {
            alvo = parada;
            leituras = 0;
        }
        if (precisao <= 0 || precisao > PRECISAO_MAX_METROS) return false; // não conta nem zera
        if (desfeitas.contains(parada)) {
            if (distancia > SAIDA_METROS) desfeitas.remove(parada);
            return false;
        }
        if (distancia > RAIO_METROS) {
            leituras = 0;
            return false;
        }
        if (++leituras < LEITURAS) return false;
        leituras = 0;
        return true;
    }

    /** O motorista desfez a conclusão automática desta parada. */
    public void desfeita(int parada) {
        desfeitas.add(parada);
        leituras = 0;
    }
}
