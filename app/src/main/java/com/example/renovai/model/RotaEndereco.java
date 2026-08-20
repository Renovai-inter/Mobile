package com.example.renovai.model;

public class RotaEndereco {

    private String rotaEnderecoId;
    private String rotaId;
    private String enderecoId;
    private String nomeLocal;
    private String tipoLocal;
    private int ordem = 1;

    public RotaEndereco() {
    }

    public RotaEndereco(String rotaEnderecoId, String rotaId, String enderecoId,
                         String nomeLocal, String tipoLocal, int ordem) {
        this.rotaEnderecoId = rotaEnderecoId;
        this.rotaId = rotaId;
        this.enderecoId = enderecoId;
        this.nomeLocal = nomeLocal;
        this.tipoLocal = tipoLocal;
        this.ordem = ordem;
    }

    public String getRotaEnderecoId() {
        return rotaEnderecoId;
    }

    public void setRotaEnderecoId(String rotaEnderecoId) {
        this.rotaEnderecoId = rotaEnderecoId;
    }

    public String getRotaId() {
        return rotaId;
    }

    public void setRotaId(String rotaId) {
        this.rotaId = rotaId;
    }

    public String getEnderecoId() {
        return enderecoId;
    }

    public void setEnderecoId(String enderecoId) {
        this.enderecoId = enderecoId;
    }

    public String getNomeLocal() {
        return nomeLocal;
    }

    public void setNomeLocal(String nomeLocal) {
        this.nomeLocal = nomeLocal;
    }

    public String getTipoLocal() {
        return tipoLocal;
    }

    public void setTipoLocal(String tipoLocal) {
        this.tipoLocal = tipoLocal;
    }

    public int getOrdem() {
        return ordem;
    }

    public void setOrdem(int ordem) {
        this.ordem = ordem;
    }

    @Override
    public String toString() {
        return "RotaEndereco{" +
                "rotaEnderecoId='" + rotaEnderecoId + '\'' +
                ", rotaId='" + rotaId + '\'' +
                ", nomeLocal='" + nomeLocal + '\'' +
                ", ordem=" + ordem +
                '}';
    }
}
