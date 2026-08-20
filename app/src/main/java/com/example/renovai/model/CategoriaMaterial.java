package com.example.renovai.model;

public class CategoriaMaterial {

    private String categoriaId;
    private String categoriaPaiId;
    private String nomeCategoria;

    public CategoriaMaterial() {
    }

    public CategoriaMaterial(String categoriaId, String categoriaPaiId, String nomeCategoria) {
        this.categoriaId = categoriaId;
        this.categoriaPaiId = categoriaPaiId;
        this.nomeCategoria = nomeCategoria;
    }

    public String getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(String categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getCategoriaPaiId() {
        return categoriaPaiId;
    }

    public void setCategoriaPaiId(String categoriaPaiId) {
        this.categoriaPaiId = categoriaPaiId;
    }

    public String getNomeCategoria() {
        return nomeCategoria;
    }

    public void setNomeCategoria(String nomeCategoria) {
        this.nomeCategoria = nomeCategoria;
    }

    @Override
    public String toString() {
        return "CategoriaMaterial{" +
                "categoriaId='" + categoriaId + '\'' +
                ", categoriaPaiId='" + categoriaPaiId + '\'' +
                ", nomeCategoria='" + nomeCategoria + '\'' +
                '}';
    }
}
