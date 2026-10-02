package com.example.renovai.dto.request;

import java.math.BigDecimal;

/**
 * Espelha Requests.ColetaRequest (backend) — corpo de POST /coletas (tela 2.2).
 *
 * ATENÇÃO — limitação real da API (documentada em IMPLEMENTACAO.md): este DTO só
 * tem os campos abaixo. O wireframe da tela 2.2 também pede "materiais coletados",
 * "necessita triagem?" e "data prevista da triagem", mas o backend NÃO tem esses
 * campos em ColetaRequest hoje — a tela mostra esses campos (fidelidade ao
 * wireframe), mas eles não são enviados para a API porque não existe onde
 * persisti-los ainda.
 *
 * statusId é opcional (backend aceita null — ver ColetaService.criar()).
 */
public class ColetaRequest {
    private final String cooperadoId;
    private final String statusId;
    private final BigDecimal quantidadeKg;
    private final String imagemUrl;
    private final String tipoColeta;
    private final String rotaId;

    public ColetaRequest(String cooperadoId, String statusId, BigDecimal quantidadeKg,
                          String imagemUrl, String tipoColeta, String rotaId) {
        this.cooperadoId = cooperadoId;
        this.statusId = statusId;
        this.quantidadeKg = quantidadeKg;
        this.imagemUrl = imagemUrl;
        this.tipoColeta = tipoColeta;
        this.rotaId = rotaId;
    }

    public String getCooperadoId() { return cooperadoId; }
    public String getStatusId() { return statusId; }
    public BigDecimal getQuantidadeKg() { return quantidadeKg; }
    public String getImagemUrl() { return imagemUrl; }
    public String getTipoColeta() { return tipoColeta; }
    public String getRotaId() { return rotaId; }
}
