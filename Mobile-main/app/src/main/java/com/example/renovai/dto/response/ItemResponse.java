package com.example.renovai.dto.response;

/** Espelha Responses.ItemResponse. Retorno de GET /pedidos/{id}/itens. */
public class ItemResponse {
    private String itemId;
    private String pedidoId;
    private String materialId;
    private String materialCategoria;
    private Double quantidadeKg;
    private Double precoUnitario;

    public String getItemId() { return itemId; }
    public String getPedidoId() { return pedidoId; }
    public String getMaterialId() { return materialId; }
    public String getMaterialCategoria() { return materialCategoria; }
    public Double getQuantidadeKg() { return quantidadeKg; }
    public Double getPrecoUnitario() { return precoUnitario; }
}
