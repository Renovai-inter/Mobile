package com.example.renovai.dto.response;

import java.math.BigDecimal;

/**
 * Espelha Responses.RateioFuncionarioResponse (backend). Vem de
 * GET /rateios/{id}/distribuicao — é a distribuição individual de UM rateio entre
 * todos os funcionários da cooperativa. A tela 2.5 (Rateios Recebidos) filtra essa
 * lista pelo funcionarioId do cooperado logado (ver RateiController / IMPLEMENTACAO.md
 * sobre por que não existe um GET /rateios/por-cooperado direto na API).
 */
public class RateioFuncionarioResponse {
    private String rateioFuncionarioId;
    private String rateioId;
    private String dataRateio;
    private String tipoRateio;
    private String funcionarioId;
    private String funcionarioNome;
    private BigDecimal valorRateio;
    private String cooperativaNome;

    public String getRateioFuncionarioId() { return rateioFuncionarioId; }
    public String getRateioId() { return rateioId; }
    public String getDataRateio() { return dataRateio; }
    public String getTipoRateio() { return tipoRateio; }
    public String getFuncionarioId() { return funcionarioId; }
    public String getFuncionarioNome() { return funcionarioNome; }
    public BigDecimal getValorRateio() { return valorRateio; }
    public String getCooperativaNome() { return cooperativaNome; }
}
