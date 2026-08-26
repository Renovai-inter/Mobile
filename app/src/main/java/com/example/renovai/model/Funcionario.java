package com.example.renovai.model;

import com.example.renovai.model.enums.StatusFuncionario;
import java.util.Date;

public class Funcionario {

    private String funcionarioId;
    private String usuarioId;
    private String cooperativaId;
    private String cargoId;
    private Date dataAdmissao;
    private Date dataDesligamento;
    private StatusFuncionario statusFuncionario = StatusFuncionario.ATIVO;

    // Colunas GENERATED ALWAYS no banco: nunca enviar no INSERT/UPDATE,
    // apenas ler o valor retornado pelo servidor.
    private boolean estaAtivo;
    private boolean estaAfastado;

    public Funcionario() {
    }

    public Funcionario(String funcionarioId, String usuarioId, String cooperativaId, String cargoId,
                        Date dataAdmissao, Date dataDesligamento, StatusFuncionario statusFuncionario) {
        this.funcionarioId = funcionarioId;
        this.usuarioId = usuarioId;
        this.cooperativaId = cooperativaId;
        this.cargoId = cargoId;
        this.dataAdmissao = dataAdmissao;
        this.dataDesligamento = dataDesligamento;
        this.statusFuncionario = statusFuncionario;
    }

    public String getFuncionarioId() {
        return funcionarioId;
    }

    public void setFuncionarioId(String funcionarioId) {
        this.funcionarioId = funcionarioId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getCooperativaId() {
        return cooperativaId;
    }

    public void setCooperativaId(String cooperativaId) {
        this.cooperativaId = cooperativaId;
    }

    public String getCargoId() {
        return cargoId;
    }

    public void setCargoId(String cargoId) {
        this.cargoId = cargoId;
    }

    public Date getDataAdmissao() {
        return dataAdmissao;
    }

    public void setDataAdmissao(Date dataAdmissao) {
        this.dataAdmissao = dataAdmissao;
    }

    public Date getDataDesligamento() {
        return dataDesligamento;
    }

    public void setDataDesligamento(Date dataDesligamento) {
        this.dataDesligamento = dataDesligamento;
    }

    public StatusFuncionario getStatusFuncionario() {
        return statusFuncionario;
    }

    public void setStatusFuncionario(StatusFuncionario statusFuncionario) {
        this.statusFuncionario = statusFuncionario;
    }

    public boolean isEstaAtivo() {
        return estaAtivo;
    }

    public void setEstaAtivo(boolean estaAtivo) {
        this.estaAtivo = estaAtivo;
    }

    public boolean isEstaAfastado() {
        return estaAfastado;
    }

    public void setEstaAfastado(boolean estaAfastado) {
        this.estaAfastado = estaAfastado;
    }

    @Override
    public String toString() {
        return "Funcionario{" +
                "funcionarioId='" + funcionarioId + '\'' +
                ", usuarioId='" + usuarioId + '\'' +
                ", cooperativaId='" + cooperativaId + '\'' +
                ", statusFuncionario=" + statusFuncionario +
                '}';
    }
}
