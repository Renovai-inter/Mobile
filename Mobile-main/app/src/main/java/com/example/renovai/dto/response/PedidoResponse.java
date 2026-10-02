package com.example.renovai.dto.response;

public class PedidoResponse {
    private String pedidoId;
    private String empresaId;
    private String empresaNome;
    private String dataPedido;
    private String dataConclusao;
    private String statusAtual;
    private Double valorTotal;
    private String observacao;

    public String getPedidoId() { return pedidoId; }
    public String getEmpresaId() { return empresaId; }
    public String getEmpresaNome() { return empresaNome; }
    public String getDataPedido() { return dataPedido; }
    public String getDataConclusao() { return dataConclusao; }
    public String getStatusAtual() { return statusAtual; }
    public Double getValorTotal() { return valorTotal; }
    public String getObservacao() { return observacao; }
}
