package com.example.renovai.dto.response;

/**
 * Espelha Responses.RotaResponse (backend) — versão enxuta, só com o que a tela
 * 2.2 (seletor "Rota de origem" para coleta Externa) precisa. O record real
 * também tem "enderecos" (List&lt;RotaEnderecoResponse&gt;), omitido aqui de
 * propósito por não ser usado nesta tela.
 */
public class RotaResponse {
    private String rotaId;
    private String cooperativaId;
    private String cooperativaNome;
    private String nome;
    private Boolean estaAtiva;

    public String getRotaId() { return rotaId; }
    public String getCooperativaId() { return cooperativaId; }
    public String getCooperativaNome() { return cooperativaNome; }
    public String getNome() { return nome; }
    public Boolean getEstaAtiva() { return estaAtiva; }
}
