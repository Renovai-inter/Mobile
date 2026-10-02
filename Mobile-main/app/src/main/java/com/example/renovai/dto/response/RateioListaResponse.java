package com.example.renovai.dto.response;

import java.math.BigDecimal;

/** Espelha Responses.RateioListaResponse (backend). Vem de GET /rateios/por-cooperativa/{id}. */
public class RateioListaResponse {
    private String rateioId;
    private String gestorNome;
    private String cooperativaNome;
    private String tipoRateio;
    private String dataRateio;
    private Long quantidadePessoas;
    private BigDecimal valorTotalDistribuido;

    public String getRateioId() { return rateioId; }
    public String getGestorNome() { return gestorNome; }
    public String getCooperativaNome() { return cooperativaNome; }
    public String getTipoRateio() { return tipoRateio; }
    public String getDataRateio() { return dataRateio; }
    public Long getQuantidadePessoas() { return quantidadePessoas; }
    public BigDecimal getValorTotalDistribuido() { return valorTotalDistribuido; }
}
