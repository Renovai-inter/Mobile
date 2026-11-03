package com.example.renovai.dto.response;

import java.math.BigDecimal;

/** Espelha Responses.MaterialResponse. */
public class MaterialResponse {
    private String materialId;
    private String categoriaId;
    private String categoriaNome;
    private BigDecimal precoSugerido;
    private Boolean estaDisponivel;
    private String cooperativaId;
    private String imagemUrl;

    public String getMaterialId() { return materialId; }
    public String getCategoriaId() { return categoriaId; }
    public String getCategoriaNome() { return categoriaNome; }
    public BigDecimal getPrecoSugerido() { return precoSugerido; }
    public Boolean getEstaDisponivel() { return estaDisponivel; }
    public String getCooperativaId() { return cooperativaId; }
    public String getImagemUrl() { return imagemUrl; }

    /** Usa a categoria específica e seus ancestrais, sem inventar subprodutos. */
    public String nomeExibicao(java.util.List<CategoriaMaterialResponse> categorias) {
        java.util.List<String> nomes = new java.util.ArrayList<>();
        java.util.Set<String> visitadas = new java.util.HashSet<>();
        java.util.Map<String, CategoriaMaterialResponse> porId = new java.util.HashMap<>();
        for (CategoriaMaterialResponse c : categorias) porId.put(c.getCategoriaId(), c);
        String id = categoriaId;
        while (id != null && visitadas.add(id)) {
            CategoriaMaterialResponse atual = porId.get(id);
            if (atual == null) break;
            if (atual.getNomeCategoria() != null && !atual.getNomeCategoria().isBlank())
                nomes.add(atual.getNomeCategoria());
            if (atual.getCategoriaPaiId() != null
                    && !porId.containsKey(atual.getCategoriaPaiId())) {
                if (atual.getCategoriaPaiNome() != null && !atual.getCategoriaPaiNome().isBlank())
                    nomes.add(atual.getCategoriaPaiNome());
                break;
            }
            id = atual.getCategoriaPaiId();
        }
        if (nomes.isEmpty())
            return categoriaNome == null || categoriaNome.isBlank() ? "Material" : categoriaNome;
        return String.join(" › ", nomes);
    }

}
