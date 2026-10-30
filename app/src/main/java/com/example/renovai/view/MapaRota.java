package com.example.renovai.view;

import android.graphics.Bitmap;

import java.util.List;

/**
 * O mapa da tela de navegação do motorista ({@link MapaGoogle}, Google Maps). Mantém o SDK fora da
 * Activity, que só lida com paradas e posições. Pontos são sempre {lat, lng}.
 */
interface MapaRota {

    interface AoTocar {
        /** Toque no pino da parada {@code indice} da rota. */
        void parada(int indice);
    }

    final class Pino {
        final double[] ponto;
        final int indice;
        final Bitmap icone;
        final boolean destaque;

        Pino(double[] ponto, int indice, Bitmap icone, boolean destaque) {
            this.ponto = ponto;
            this.indice = indice;
            this.icone = icone;
            this.destaque = destaque;
        }
    }

    /** Troca pinos e linhas: {@code feito} (verde, contínua) e {@code resto} (cinza, tracejada). */
    void desenhar(List<Pino> pinos, List<double[]> feito, List<double[]> resto);

    /** Mostra todos os pontos na tela. */
    void enquadrar(List<double[]> pontos);

    void centralizar(double[] ponto);

    /** Posição do motorista (o Google desenha o ponto azul sozinho; usado por outros mapas). */
    void posicao(double[] ponto);

    /** A permissão de localização foi concedida. */
    void localizacaoPermitida();

    void retomar();

    void pausar();

    void destruir();
}
