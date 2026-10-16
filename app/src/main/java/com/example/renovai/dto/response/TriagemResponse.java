package com.example.renovai.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Espelha Responses.TriagemResponse (backend).
 *
 * Importante: cada Triagem representa UM material dentro de uma coleta (não a
 * coleta inteira). Uma coleta com N materiais gera N registros de Triagem, todos
 * com o mesmo coletaId — é por isso que "Completar Triagem" (tela 2.3) busca por
 * GET /triagens/por-coleta/{coletaId} e mostra uma linha por material.
 */
public class TriagemResponse {
    private String triagemId;
    private String equipeId;
    private String equipeNome;
    private String coletaId;
    private String materialId;
    private String materialCategoria;
    private String statusAtual;
    private BigDecimal quantidadeKg;
    private BigDecimal quantidadeRejeitoKg;
    private String dataTriagem;
    private String imagemUrl;
    private List<String> cooperadosNomes;

    public String getTriagemId() { return triagemId; }
    public String getEquipeId() { return equipeId; }
    public String getEquipeNome() { return equipeNome; }
    public String getColetaId() { return coletaId; }
    public String getMaterialId() { return materialId; }
    public String getMaterialCategoria() { return materialCategoria; }
    public String getStatusAtual() { return statusAtual; }
    public BigDecimal getQuantidadeKg() { return quantidadeKg; }
    public BigDecimal getQuantidadeRejeitoKg() { return quantidadeRejeitoKg; }
    public String getDataTriagem() { return dataTriagem; }
    public String getImagemUrl() { return imagemUrl; }
    public List<String> getCooperadosNomes() { return cooperadosNomes; }
}
