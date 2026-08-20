package com.example.renovai.model;

import com.renovai.model.enums.TipoMovimentacao;
import java.math.BigDecimal;
import java.util.Date;

public class MovimentacaoEstoque {

    private String movimentacaoId;
    private String estoqueId;
    private String triagemId;
    private String itemId;
    private BigDecimal quantidadeKg;

    // Coluna GENERATED ALWAYS no banco (derivada do sinal de quantidadeKg):
    // nunca enviar no INSERT/UPDATE, apenas ler o valor retornado pelo servidor.
    private TipoMovimentacao tipoMovimentacao;

    private Date dataMovimentacao;

    public MovimentacaoEstoque() {
    }

    public MovimentacaoEstoque(String movimentacaoId, String estoqueId, String triagemId, String itemId,
                                BigDecimal quantidadeKg, Date dataMovimentacao) {
        this.movimentacaoId = movimentacaoId;
        this.estoqueId = estoqueId;
        this.triagemId = triagemId;
        this.itemId = itemId;
        this.quantidadeKg = quantidadeKg;
        this.dataMovimentacao = dataMovimentacao;
    }

    public String getMovimentacaoId() {
        return movimentacaoId;
    }

    public void setMovimentacaoId(String movimentacaoId) {
        this.movimentacaoId = movimentacaoId;
    }

    public String getEstoqueId() {
        return estoqueId;
    }

    public void setEstoqueId(String estoqueId) {
        this.estoqueId = estoqueId;
    }

    public String getTriagemId() {
        return triagemId;
    }

    public void setTriagemId(String triagemId) {
        this.triagemId = triagemId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public BigDecimal getQuantidadeKg() {
        return quantidadeKg;
    }

    public void setQuantidadeKg(BigDecimal quantidadeKg) {
        this.quantidadeKg = quantidadeKg;
    }

    public TipoMovimentacao getTipoMovimentacao() {
        return tipoMovimentacao;
    }

    public void setTipoMovimentacao(TipoMovimentacao tipoMovimentacao) {
        this.tipoMovimentacao = tipoMovimentacao;
    }

    public Date getDataMovimentacao() {
        return dataMovimentacao;
    }

    public void setDataMovimentacao(Date dataMovimentacao) {
        this.dataMovimentacao = dataMovimentacao;
    }

    @Override
    public String toString() {
        return "MovimentacaoEstoque{" +
                "movimentacaoId='" + movimentacaoId + '\'' +
                ", estoqueId='" + estoqueId + '\'' +
                ", quantidadeKg=" + quantidadeKg +
                ", tipoMovimentacao=" + tipoMovimentacao +
                '}';
    }
}
