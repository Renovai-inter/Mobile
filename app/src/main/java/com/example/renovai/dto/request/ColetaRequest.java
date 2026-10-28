package com.example.renovai.dto.request;

import java.math.BigDecimal;
import java.util.List;

/**
 * Espelha Requests.ColetaRequest (backend) — corpo de POST /coletas (tela 2.2).
 *
 * <p>Com materiais, a API cria uma triagem por material (precisaTriagem = true) ou já lança no
 * estoque (false); para isso exige equipeId e que a soma dos pesos seja o peso da coleta. Sem
 * materiais, a coleta fica esperando triagem. Descrição e data prevista da triagem não existem no
 * banco da API: ficam no Firestore (ver ColetaDetalhes).
 *
 * <p>Campos null não vão no JSON (Gson), então os opcionais podem ficar vazios.
 */
public class ColetaRequest {
    private final String cooperadoId;
    private final String statusId;
    private final BigDecimal quantidadeKg;
    private final String imagemUrl;
    private final String tipoColeta;
    private final String rotaId;
    private String equipeId;
    private Boolean precisaTriagem;
    private List<Material> materiais;

    /** Um material da coleta e o peso dele. */
    public static class Material {
        private final String materialId;
        private final BigDecimal quantidadeKg;

        public Material(String materialId, BigDecimal quantidadeKg) {
            this.materialId = materialId;
            this.quantidadeKg = quantidadeKg;
        }

        public String getMaterialId() { return materialId; }
        public BigDecimal getQuantidadeKg() { return quantidadeKg; }
    }

    public ColetaRequest(String cooperadoId, String statusId, BigDecimal quantidadeKg,
                          String imagemUrl, String tipoColeta, String rotaId) {
        this.cooperadoId = cooperadoId;
        this.statusId = statusId;
        this.quantidadeKg = quantidadeKg;
        this.imagemUrl = imagemUrl;
        this.tipoColeta = tipoColeta;
        this.rotaId = rotaId;
    }

    public ColetaRequest comTriagem(Boolean precisaTriagem) {
        this.precisaTriagem = precisaTriagem;
        return this;
    }

    public ColetaRequest comMateriais(String equipeId, List<Material> materiais) {
        this.equipeId = equipeId;
        this.materiais = materiais == null || materiais.isEmpty() ? null : materiais;
        return this;
    }

    public String getCooperadoId() { return cooperadoId; }
    public String getStatusId() { return statusId; }
    public BigDecimal getQuantidadeKg() { return quantidadeKg; }
    public String getImagemUrl() { return imagemUrl; }
    public String getTipoColeta() { return tipoColeta; }
    public String getRotaId() { return rotaId; }
    public String getEquipeId() { return equipeId; }
    public Boolean getPrecisaTriagem() { return precisaTriagem; }
    public List<Material> getMateriais() { return materiais; }
}
