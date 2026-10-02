package com.example.renovai.model;

import java.math.BigDecimal;

public class PedidoItem {

    private String itemId;
    private String pedidoId;
    private String materialId;
    private BigDecimal quantidadeKg;
    private BigDecimal precoUnitario;

    public PedidoItem() {
    }

    public PedidoItem(String itemId, String pedidoId, String materialId,
                       BigDecimal quantidadeKg, BigDecimal precoUnitario) {
        this.itemId = itemId;
        this.pedidoId = pedidoId;
        this.materialId = materialId;
        this.quantidadeKg = quantidadeKg;
        this.precoUnitario = precoUnitario;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(String pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public BigDecimal getQuantidadeKg() {
        return quantidadeKg;
    }

    public void setQuantidadeKg(BigDecimal quantidadeKg) {
        this.quantidadeKg = quantidadeKg;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    @Override
    public String toString() {
        return "PedidoItem{" +
                "itemId='" + itemId + '\'' +
                ", pedidoId='" + pedidoId + '\'' +
                ", materialId='" + materialId + '\'' +
                ", quantidadeKg=" + quantidadeKg +
                ", precoUnitario=" + precoUnitario +
                '}';
    }
}
