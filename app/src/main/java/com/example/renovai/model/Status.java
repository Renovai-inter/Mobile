package com.example.renovai.model;

import java.util.Date;

/**
 * Registro de status genérico, reaproveitado por diferentes entidades
 * (pedidos_cooperativas, negociacoes, eventos_operacionais) através do
 * campo "referencia".
 */
public class Status {

    private String statusId;
    private String referencia;
    private String statusAtual;
    private Date dataAtualizacao;

    public Status() {
    }

    public Status(String statusId, String referencia, String statusAtual, Date dataAtualizacao) {
        this.statusId = statusId;
        this.referencia = referencia;
        this.statusAtual = statusAtual;
        this.dataAtualizacao = dataAtualizacao;
    }

    public String getStatusId() {
        return statusId;
    }

    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getStatusAtual() {
        return statusAtual;
    }

    public void setStatusAtual(String statusAtual) {
        this.statusAtual = statusAtual;
    }

    public Date getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(Date dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    @Override
    public String toString() {
        return "Status{" +
                "statusId='" + statusId + '\'' +
                ", referencia='" + referencia + '\'' +
                ", statusAtual='" + statusAtual + '\'' +
                '}';
    }
}
