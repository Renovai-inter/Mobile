package com.example.renovai.dto.response;

/**
 * Espelha FuncionarioService.FuncionarioResponse (backend, com.renovai.api.service).
 * Vem de GET /funcionarios e variantes. É a "ponte" entre o usuarioId (que a gente
 * tem a partir do login) e o funcionarioId (cooperadoId) usado em /coletas,
 * /triagens e /rateios. Ver FuncionarioResolver.
 */
public class FuncionarioResponse {
    private String funcionarioId;
    private String usuarioId;
    private String usuarioNome;
    private String cargoId;
    private String cargo;
    private String cooperativaId;
    private String cooperativaNome;
    private Boolean estaAtivo;
    private String statusFuncionario;

    public String getFuncionarioId() { return funcionarioId; }
    public String getUsuarioId() { return usuarioId; }
    public String getUsuarioNome() { return usuarioNome; }
    public String getCargoId() { return cargoId; }
    public String getCargo() { return cargo; }
    public String getCooperativaId() { return cooperativaId; }
    public String getCooperativaNome() { return cooperativaNome; }
    public Boolean getEstaAtivo() { return estaAtivo; }
    public String getStatusFuncionario() { return statusFuncionario; }
}
