package com.example.renovai.model;

import com.renovai.model.enums.TipoMensagem;
import java.util.Date;

public class NegociacaoMensagem {

    private String mensagemId;
    private String negociacaoId;
    private String remetenteId;
    private String mensagem;
    private TipoMensagem tipoMensagem = TipoMensagem.TEXTO;
    private Date dataEnvio;

    public NegociacaoMensagem() {
    }

    public NegociacaoMensagem(String mensagemId, String negociacaoId, String remetenteId,
                               String mensagem, TipoMensagem tipoMensagem, Date dataEnvio) {
        this.mensagemId = mensagemId;
        this.negociacaoId = negociacaoId;
        this.remetenteId = remetenteId;
        this.mensagem = mensagem;
        this.tipoMensagem = tipoMensagem;
        this.dataEnvio = dataEnvio;
    }

    public String getMensagemId() {
        return mensagemId;
    }

    public void setMensagemId(String mensagemId) {
        this.mensagemId = mensagemId;
    }

    public String getNegociacaoId() {
        return negociacaoId;
    }

    public void setNegociacaoId(String negociacaoId) {
        this.negociacaoId = negociacaoId;
    }

    public String getRemetenteId() {
        return remetenteId;
    }

    public void setRemetenteId(String remetenteId) {
        this.remetenteId = remetenteId;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public TipoMensagem getTipoMensagem() {
        return tipoMensagem;
    }

    public void setTipoMensagem(TipoMensagem tipoMensagem) {
        this.tipoMensagem = tipoMensagem;
    }

    public Date getDataEnvio() {
        return dataEnvio;
    }

    public void setDataEnvio(Date dataEnvio) {
        this.dataEnvio = dataEnvio;
    }

    @Override
    public String toString() {
        return "NegociacaoMensagem{" +
                "mensagemId='" + mensagemId + '\'' +
                ", negociacaoId='" + negociacaoId + '\'' +
                ", tipoMensagem=" + tipoMensagem +
                '}';
    }
}
