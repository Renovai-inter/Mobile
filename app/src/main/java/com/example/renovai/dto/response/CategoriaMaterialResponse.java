package com.example.renovai.dto.response;

/** Espelha Responses.CategoriaMaterialResponse. Categorias de material (Plástico, Papel, Metal, Vidro...). */
public class CategoriaMaterialResponse {
    private String categoriaId;
    private String categoriaPaiId;
    private String categoriaPaiNome;
    private String nomeCategoria;

    public String getCategoriaId() { return categoriaId; }
    public String getCategoriaPaiId() { return categoriaPaiId; }
    public String getCategoriaPaiNome() { return categoriaPaiNome; }
    public String getNomeCategoria() { return nomeCategoria; }
}
