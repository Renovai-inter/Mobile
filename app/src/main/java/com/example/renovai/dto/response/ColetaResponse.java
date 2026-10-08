package com.example.renovai.dto.response;

import java.math.BigDecimal;

/**
 * Espelha Responses.ColetaResponse (backend).
 *
 * CORREÇÃO (área Cooperado): removido o campo "origem", que não existe no record
 * real do backend (com.renovai.api.dto.response.Responses.ColetaResponse). O campo
 * nunca era usado em nenhum outro lugar do app — conferido com grep antes de mexer.
 */
public class ColetaResponse {
    private String coletaId;
    private String cooperadoId;
    private String cooperadoNome;
    private String statusAtual;
    private BigDecimal quantidadeKg;
    private String dataColeta;
    private String tipoColeta; // "INTERNO" | "EXTERNO"
    private String imagemUrl;
    private String rotaId;

    public String getColetaId() { return coletaId; }
    public String getCooperadoId() { return cooperadoId; }
    public String getCooperadoNome() { return cooperadoNome; }
    public String getStatusAtual() { return statusAtual; }
    public BigDecimal getQuantidadeKg() { return quantidadeKg; }
    public String getDataColeta() { return dataColeta; }
    public String getTipoColeta() { return tipoColeta; }
    public String getImagemUrl() { return imagemUrl; }
    public String getRotaId() { return rotaId; }
}
