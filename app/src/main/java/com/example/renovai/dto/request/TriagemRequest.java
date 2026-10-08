package com.example.renovai.dto.request;

import java.math.BigDecimal;

/**
 * Espelha Requests.TriagemRequest (backend) — corpo de PUT /triagens/{id}, usado
 * pela tela 2.3 para "salvar o progresso" da separação de um material.
 *
 * statusId é opcional (backend só atualiza o status se vier != null — ver
 * TriagemService.atualizar()). Como o app não tem hoje uma tela para escolher o
 * statusId manualmente (e os valores reais de referencia=TRIAGEM não são
 * conhecidos sem consultar o banco), ele é sempre enviado como null ao salvar
 * progresso — o status intermediário simplesmente não muda, o que é seguro.
 * Quem realmente fecha o status para "CONCLUIDA" é o endpoint de concluir
 * (ver ConcluirTriagemRequest / TriagemApiService.concluir).
 */
public class TriagemRequest {
    private final String equipeId;
    private final String coletaId;
    private final String materialId;
    private final String statusId;
    private final BigDecimal quantidadeKg;
    private final BigDecimal quantidadeRejeitoKg;
    private final String imagemUrl;

    public TriagemRequest(String equipeId, String coletaId, String materialId, String statusId,
                           BigDecimal quantidadeKg, BigDecimal quantidadeRejeitoKg, String imagemUrl) {
        this.equipeId = equipeId;
        this.coletaId = coletaId;
        this.materialId = materialId;
        this.statusId = statusId;
        this.quantidadeKg = quantidadeKg;
        this.quantidadeRejeitoKg = quantidadeRejeitoKg;
        this.imagemUrl = imagemUrl;
    }

    public String getEquipeId() { return equipeId; }
    public String getColetaId() { return coletaId; }
    public String getMaterialId() { return materialId; }
    public String getStatusId() { return statusId; }
    public BigDecimal getQuantidadeKg() { return quantidadeKg; }
    public BigDecimal getQuantidadeRejeitoKg() { return quantidadeRejeitoKg; }
    public String getImagemUrl() { return imagemUrl; }
}
