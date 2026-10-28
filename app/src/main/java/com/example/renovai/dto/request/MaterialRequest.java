package com.example.renovai.dto.request;

import java.math.BigDecimal;

/**
 * Espelha Requests.MaterialRequest (backend) — corpo de POST /materiais. imagemUrl (foto no
 * Cloudinary) só é gravada pela versão da API que aceita o campo; as anteriores o ignoram.
 */
public class MaterialRequest {
    private final String categoriaId;
    private final String cooperativaId;
    private final BigDecimal precoSugerido;
    private final Boolean estaDisponivel;
    private final String imagemUrl;

    public MaterialRequest(String categoriaId, String cooperativaId, BigDecimal precoSugerido,
                           Boolean estaDisponivel, String imagemUrl) {
        this.categoriaId = categoriaId;
        this.cooperativaId = cooperativaId;
        this.precoSugerido = precoSugerido;
        this.estaDisponivel = estaDisponivel;
        this.imagemUrl = imagemUrl;
    }

    public String getCategoriaId() { return categoriaId; }
    public String getCooperativaId() { return cooperativaId; }
    public BigDecimal getPrecoSugerido() { return precoSugerido; }
    public Boolean getEstaDisponivel() { return estaDisponivel; }
    public String getImagemUrl() { return imagemUrl; }
}
