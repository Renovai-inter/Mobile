package com.example.renovai.model;

public class EquipeCooperado {

    private String equipeCooperadoId;
    private String equipeId;
    private String cooperadoId;

    public EquipeCooperado() {
    }

    public EquipeCooperado(String equipeCooperadoId, String equipeId, String cooperadoId) {
        this.equipeCooperadoId = equipeCooperadoId;
        this.equipeId = equipeId;
        this.cooperadoId = cooperadoId;
    }

    public String getEquipeCooperadoId() {
        return equipeCooperadoId;
    }

    public void setEquipeCooperadoId(String equipeCooperadoId) {
        this.equipeCooperadoId = equipeCooperadoId;
    }

    public String getEquipeId() {
        return equipeId;
    }

    public void setEquipeId(String equipeId) {
        this.equipeId = equipeId;
    }

    public String getCooperadoId() {
        return cooperadoId;
    }

    public void setCooperadoId(String cooperadoId) {
        this.cooperadoId = cooperadoId;
    }

    @Override
    public String toString() {
        return "EquipeCooperado{" +
                "equipeCooperadoId='" + equipeCooperadoId + '\'' +
                ", equipeId='" + equipeId + '\'' +
                ", cooperadoId='" + cooperadoId + '\'' +
                '}';
    }
}
