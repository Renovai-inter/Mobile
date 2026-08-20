package com.example.renovai.model;

import java.util.Date;

public class Pedido {

    private String pedidoId;
    private String empresaId;
    private String observacao;
    private Date dataPedido;
    private Date dataConclusao;

    public Pedido() {
    }

    public Pedido(String pedidoId, String empresaId, String observacao, Date dataPedido, Date dataConclusao) {
        this.pedidoId = pedidoId;
        this.empresaId = empresaId;
        this.observacao = observacao;
        this.dataPedido = dataPedido;
        this.dataConclusao = dataConclusao;
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(String pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(String empresaId) {
        this.empresaId = empresaId;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public Date getDataPedido() {
        return dataPedido;
    }

    public void setDataPedido(Date dataPedido) {
        this.dataPedido = dataPedido;
    }

    public Date getDataConclusao() {
        return dataConclusao;
    }

    public void setDataConclusao(Date dataConclusao) {
        this.dataConclusao = dataConclusao;
    }

    @Override
    public String toString() {
        return "Pedido{" +
                "pedidoId='" + pedidoId + '\'' +
                ", empresaId='" + empresaId + '\'' +
                ", dataPedido=" + dataPedido +
                '}';
    }
}
