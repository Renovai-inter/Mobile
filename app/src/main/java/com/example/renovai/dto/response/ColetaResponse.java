package com.example.renovai.dto.response;

import java.math.BigDecimal;

/** Espelha Responses.ColetaResponse. */
public class ColetaResponse {
    private String coletaId;
    private String cooperadoId;
    private String cooperadoNome;
    private String statusAtual;
    private String origem;
    private BigDecimal quantidadeKg;
    private String dataColeta;
    private String tipoColeta; // "EXTERNA" | "ENTREGA"
    private String imagemUrl;
    private String rotaId;

    public String getColetaId() { return coletaId; }
    public String getCooperadoId() { return cooperadoId; }
    public String getCooperadoNome() { return cooperadoNome; }
    public String getStatusAtual() { return statusAtual; }
    public String getOrigem() { return origem; }
    public BigDecimal getQuantidadeKg() { return quantidadeKg; }
    public String getDataColeta() { return dataColeta; }
    public String getTipoColeta() { return tipoColeta; }
    public String getImagemUrl() { return imagemUrl; }
    public String getRotaId() { return rotaId; }
}
