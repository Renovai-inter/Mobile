package com.example.renovai.dto.response;

import java.util.List;

/**
 * DTOs da área do Gestor. Espelham (com nomes simplificados) os records de
 * com.renovai.api.dto.response.Responses. Campos públicos porque o Gson preenche direto por
 * reflexão e são só leitura na UI.
 */
public class GestorResponses {

    public static class NegItem {
        public String negociacaoItemId, negociacaoId, materialId, materialCategoria;
        public Double quantidadeKg, precoUnitario;
    }

    public static class Negociacao {
        public String negociacaoId,
                pedidoId,
                cooperativaId,
                cooperativaNome,
                empresaId,
                empresaNome;
        public String statusAtual, dataInicio, dataFechamento, observacao;
        public Double valorTotal;
        public List<NegItem> itens;
    }

    public static class Mensagem {
        public String mensagemId,
                negociacaoId,
                remetenteId,
                remetenteNome,
                mensagem,
                tipoMensagem,
                dataEnvio;
    }

    public static class Status {
        public String statusId, referencia, statusAtual;
    }

    public static class Pedido {
        public String pedidoId,
                empresaId,
                empresaNome,
                dataPedido,
                dataConclusao,
                statusAtual,
                observacao;
        public Double valorTotal;
    }

    public static class PedidoCoop {
        public String pedidoCooperativaId, pedidoId, cooperativaId, cooperativaNome, statusAtual;
    }

    public static class Item {
        public String itemId, pedidoId, materialId, materialCategoria;
        public Double quantidadeKg, precoUnitario;
    }

    public static class Estoque {
        public String estoqueId,
                cooperativaId,
                cooperativaNome,
                materialId,
                materialCategoria,
                dataAtualizacao;
        public Double quantidadeKg;
    }

    public static class ResultadoInd {
        public String funcionarioId, funcionarioNome, cargo;
        public Boolean isGestor;
        public Double valorRateio, percentualParticipacao;
        public Integer quantidadeColetas, quantidadeTriagens;
    }

    public static class RateioDetalhe {
        public String rateioId, gestorNome, cooperativaNome, tipoRateio, dataRateio;
        public Double valorTotalDistribuido;
        public List<ResultadoInd> funcionarios;
    }

    public static class RateioRealizado {
        public String rateioId, tipoRateio;
        public Double valorTotalDistribuido;
        public Long quantidadePessoas;
    }

    public static class TotalAcumulado {
        public Double totalAcumulado;
    }

    public static class Despesa {
        public String despesaId, cooperativaId, nome, tipoDespesa;
        public Boolean estaAtiva;
    }

    public static class Lancamento {
        public String lancamentoId, despesaId, despesaNome, tipoDespesa, mesReferencia;
        public Double valor;
    }

    public static class TotalDespesas {
        public String mesReferencia;
        public Double totalFixas, totalVariaveis, totalGeral;
    }

    /** Montado no app a partir de GET /funcionarios/por-cooperativa + pré-cadastros incompletos. */
    public static class FuncionarioDetalhe {
        public String funcionarioId, usuarioId, nome, cargoId, cargo;
        public String cooperativaId, cooperativaNome, statusFuncionario, dataAdmissao;
        public Boolean estaAtivo, pendente;

        public boolean inativo() {
            return (statusFuncionario != null && statusFuncionario.toUpperCase().contains("INATIV"))
                    || Boolean.FALSE.equals(estaAtivo);
        }
    }

    public static class PreCadastro {
        public String funcionarioId,
                usuarioId,
                usuarioNome,
                cpf,
                cooperativaId,
                cooperativaNome,
                cargoAtribuido;
        public Boolean temEmailCompleto;
        public String dataAdmissao;
    }

    public static class Cargo {
        public String cargoId, cargo;
    }

    public static class Usuario {
        public String usuarioId, nome, cpf, dataNascimento, imagemUrl;
    }

    public static class RotaEnd {
        public String rotaEnderecoId,
                rotaId,
                enderecoId,
                nomeLocal,
                tipoLocal,
                logradouro,
                numero,
                bairro,
                cidade;
        public Integer ordem;
    }

    public static class Rota {
        public String rotaId, cooperativaId, cooperativaNome, nome;
        public Boolean estaAtiva;
        public List<RotaEnd> enderecos;
    }

    public static class Endereco {
        public String enderecoId, cep, logradouro, numero, complemento, bairro, cidade, tipo;
    }

    /** Responses.EquipeResponse (só o que o app usa). */
    public static class Equipe {
        public String equipeId, cooperativaId, gestorId, nome;
        public Boolean estaAtiva;
    }

    public static class Procedure {
        public Boolean sucesso;
        public String mensagem;
    }
}
