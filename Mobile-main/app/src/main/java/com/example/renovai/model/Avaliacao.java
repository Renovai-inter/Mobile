package com.example.renovai.model;

import java.util.Date;

public class Avaliacao {

    private String avaliacaoId;
    private String avaliadorId;
    private String avaliadoId;
    private String pedidoId;
    private Integer nota;
    private String comentario;
    private Date dataAvaliacao;
    private Date atualizadoEm;

    public Avaliacao() {
    }

    public Avaliacao(String avaliacaoId, String avaliadorId, String avaliadoId, String pedidoId,
                      Integer nota, String comentario, Date dataAvaliacao, Date atualizadoEm) {
        this.avaliacaoId = avaliacaoId;
        this.avaliadorId = avaliadorId;
        this.avaliadoId = avaliadoId;
        this.pedidoId = pedidoId;
        this.nota = nota;
        this.comentario = comentario;
        this.dataAvaliacao = dataAvaliacao;
        this.atualizadoEm = atualizadoEm;
    }

    public String getAvaliacaoId() {
        return avaliacaoId;
    }

    public void setAvaliacaoId(String avaliacaoId) {
        this.avaliacaoId = avaliacaoId;
    }

    public String getAvaliadorId() {
        return avaliadorId;
    }

    public void setAvaliadorId(String avaliadorId) {
        this.avaliadorId = avaliadorId;
    }

    public String getAvaliadoId() {
        return avaliadoId;
    }

    public void setAvaliadoId(String avaliadoId) {
        this.avaliadoId = avaliadoId;
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(String pedidoId) {
        this.pedidoId = pedidoId;
    }

    public Integer getNota() {
        return nota;
    }

    public void setNota(Integer nota) {
        this.nota = nota;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public Date getDataAvaliacao() {
        return dataAvaliacao;
    }

    public void setDataAvaliacao(Date dataAvaliacao) {
        this.dataAvaliacao = dataAvaliacao;
    }

    public Date getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Date atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    @Override
    public String toString() {
        return "Avaliacao{" +
                "avaliacaoId='" + avaliacaoId + '\'' +
                ", avaliadorId='" + avaliadorId + '\'' +
                ", avaliadoId='" + avaliadoId + '\'' +
                ", nota=" + nota +
                '}';
    }
}
