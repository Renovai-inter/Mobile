package com.example.renovai.model;

import java.util.Date;

/**
 * Classe base abstrata que espelha a tabela "eventos_operacionais" do banco
 * (usada via INHERITS pelo PostgreSQL). Coleta e Triagem estendem esta
 * classe, assim como coletas e triagens herdam de eventos_operacionais no
 * schema SQL.
 */
public abstract class EventoOperacional {

    private String eventoId;
    private Date dataEvento;
    private String statusId;

    protected EventoOperacional() {
    }

    protected EventoOperacional(String eventoId, Date dataEvento, String statusId) {
        this.eventoId = eventoId;
        this.dataEvento = dataEvento;
        this.statusId = statusId;
    }

    public String getEventoId() {
        return eventoId;
    }

    public void setEventoId(String eventoId) {
        this.eventoId = eventoId;
    }

    public Date getDataEvento() {
        return dataEvento;
    }

    public void setDataEvento(Date dataEvento) {
        this.dataEvento = dataEvento;
    }

    public String getStatusId() {
        return statusId;
    }

    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }
}
