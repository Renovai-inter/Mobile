package com.example.renovai.model;

public class PedidoCooperativa {

    private String pedidoCooperativaId;
    private String pedidoId;
    private String cooperativaId;
    private String statusId;

    public PedidoCooperativa() {
    }

    public PedidoCooperativa(String pedidoCooperativaId, String pedidoId, String cooperativaId, String statusId) {
        this.pedidoCooperativaId = pedidoCooperativaId;
        this.pedidoId = pedidoId;
        this.cooperativaId = cooperativaId;
        this.statusId = statusId;
    }

    public String getPedidoCooperativaId() {
        return pedidoCooperativaId;
    }

    public void setPedidoCooperativaId(String pedidoCooperativaId) {
        this.pedidoCooperativaId = pedidoCooperativaId;
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(String pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getCooperativaId() {
        return cooperativaId;
    }

    public void setCooperativaId(String cooperativaId) {
        this.cooperativaId = cooperativaId;
    }

    public String getStatusId() {
        return statusId;
    }

    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    @Override
    public String toString() {
        return "PedidoCooperativa{" +
                "pedidoCooperativaId='" + pedidoCooperativaId + '\'' +
                ", pedidoId='" + pedidoId + '\'' +
                ", cooperativaId='" + cooperativaId + '\'' +
                ", statusId='" + statusId + '\'' +
                '}';
    }
}
