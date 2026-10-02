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
}
