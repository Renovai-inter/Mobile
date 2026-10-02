package com.example.renovai.model;

import com.example.renovai.model.enums.TipoColeta;
import java.math.BigDecimal;
import java.util.Date;

public class Coleta extends EventoOperacional {

    private String cooperadoId;
    private String origem;
    private TipoColeta tipoColeta;
    private BigDecimal quantidadeKg;

    public Coleta() {
        super();
    }

    public Coleta(String eventoId, Date dataEvento, String statusId, String cooperadoId,
                  String origem, TipoColeta tipoColeta, BigDecimal quantidadeKg) {
        super(eventoId, dataEvento, statusId);
        this.cooperadoId = cooperadoId;
        this.origem = origem;
        this.tipoColeta = tipoColeta;
        this.quantidadeKg = quantidadeKg;
    }

    public String getCooperadoId() {
        return cooperadoId;
    }

    public void setCooperadoId(String cooperadoId) {
        this.cooperadoId = cooperadoId;
    }

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public TipoColeta getTipoColeta() {
        return tipoColeta;
    }

    public void setTipoColeta(TipoColeta tipoColeta) {
        this.tipoColeta = tipoColeta;
    }

    public BigDecimal getQuantidadeKg() {
        return quantidadeKg;
    }

    public void setQuantidadeKg(BigDecimal quantidadeKg) {
        this.quantidadeKg = quantidadeKg;
    }

    @Override
    public String toString() {
        return "Coleta{" +
                "eventoId='" + getEventoId() + '\'' +
                ", cooperadoId='" + cooperadoId + '\'' +
                ", origem='" + origem + '\'' +
                ", tipoColeta=" + tipoColeta +
                ", quantidadeKg=" + quantidadeKg +
                '}';
    }
}
