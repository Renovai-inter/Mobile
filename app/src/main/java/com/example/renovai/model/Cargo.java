package com.example.renovai.model;

public class Cargo {

    private String cargoId;
    private String cargo;

    public Cargo() {
    }

    public Cargo(String cargoId, String cargo) {
        this.cargoId = cargoId;
        this.cargo = cargo;
    }

    public String getCargoId() {
        return cargoId;
    }

    public void setCargoId(String cargoId) {
        this.cargoId = cargoId;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    @Override
    public String toString() {
        return "Cargo{" +
                "cargoId='" + cargoId + '\'' +
                ", cargo='" + cargo + '\'' +
                '}';
    }
}
