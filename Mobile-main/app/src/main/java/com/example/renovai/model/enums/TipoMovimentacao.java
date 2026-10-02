package com.example.renovai.model.enums;

/**
 * Espelha a coluna GENERATED ALWAYS AS de movimentacoes_estoques.
 * ENTRADA quando quantidade_kg > 0, SAIDA quando quantidade_kg < 0.
 */
public enum TipoMovimentacao {
    ENTRADA,
    SAIDA
}
