package com.example.renovai.dto.request;


/** Corpos de requisição da área do Gestor (espelham os records de Requests da API). */
public class GestorRequests {

    public static class Mensagem {
        public String negociacaoId, remetenteId, mensagem, tipoMensagem;

        public Mensagem(
                String negociacaoId, String remetenteId, String mensagem, String tipoMensagem) {
            this.negociacaoId = negociacaoId;
            this.remetenteId = remetenteId;
            this.mensagem = mensagem;
            this.tipoMensagem = tipoMensagem;
        }
    }

    public static class Contraproposta {
        public String negociacaoId, observacao;
        public Double valorTotal;

        public Contraproposta(String negociacaoId, Double valorTotal, String observacao) {
            this.negociacaoId = negociacaoId;
            this.valorTotal = valorTotal;
            this.observacao = observacao;
        }
    }

    public static class Fechar {
        public Double valorFinal;
        public String observacao;

        public Fechar(Double valorFinal, String observacao) {
            this.valorFinal = valorFinal;
            this.observacao = observacao;
        }
    }

    public static class Recusar {
        public String justificativa;

        public Recusar(String justificativa) {
            this.justificativa = justificativa;
        }
    }

    public static class Negociacao {
        public String pedidoId, cooperativaId, empresaId, statusId;
        public Double valorTotal;

        public Negociacao(
                String pedidoId,
                String cooperativaId,
                String empresaId,
                String statusId,
                Double valorTotal) {
            this.pedidoId = pedidoId;
            this.cooperativaId = cooperativaId;
            this.empresaId = empresaId;
            this.statusId = statusId;
            this.valorTotal = valorTotal;
        }
    }

    public static class NegItem {
        public String negociacaoId, materialId;
        public Double quantidadeKg, precoUnitario;

        public NegItem(
                String negociacaoId, String materialId, Double quantidadeKg, Double precoUnitario) {
            this.negociacaoId = negociacaoId;
            this.materialId = materialId;
            this.quantidadeKg = quantidadeKg;
            this.precoUnitario = precoUnitario;
        }
    }

    /** POST /rateios/executar-geral e /executar-proporcional (datas ISO yyyy-MM-ddTHH:mm:ss). */
    public static class RateioPeriodo {
        public static RateioPeriodo paraMes(
                String gestorId, String cooperativaId, java.time.YearMonth mes) {
            return new RateioPeriodo(
                    gestorId,
                    cooperativaId,
                    mes.atDay(1)
                            .atStartOfDay()
                            .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    mes.atEndOfMonth()
                            .atTime(23, 59, 59)
                            .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        public String gestorId, cooperativaId, dataInicio, dataFim;

        public RateioPeriodo(
                String gestorId, String cooperativaId, String dataInicio, String dataFim) {
            this.gestorId = gestorId;
            this.cooperativaId = cooperativaId;
            this.dataInicio = dataInicio;
            this.dataFim = dataFim;
        }
    }

    public static class Financeiro {
        public String cooperativaId, dataInicio, dataFim;

        public Financeiro(String cooperativaId, String dataInicio, String dataFim) {
            this.cooperativaId = cooperativaId;
            this.dataInicio = dataInicio;
            this.dataFim = dataFim;
        }
    }

    public static class Despesa {
        public String cooperativaId, nome, tipoDespesa;
        public Boolean estaAtiva = true;

        public Despesa(String cooperativaId, String nome, String tipoDespesa) {
            this.cooperativaId = cooperativaId;
            this.nome = nome;
            this.tipoDespesa = tipoDespesa;
        }
    }

    public static class Lancamento {
        public String despesaId, mesReferencia;
        public Double valor;

        public Lancamento(String despesaId, Double valor, String mesReferencia) {
            this.despesaId = despesaId;
            this.valor = valor;
            this.mesReferencia = mesReferencia;
        }
    }

    public static class Rota {
        public String cooperativaId, nome;
        public Boolean estaAtiva;

        public Rota(String cooperativaId, String nome, Boolean estaAtiva) {
            this.cooperativaId = cooperativaId;
            this.nome = nome;
            this.estaAtiva = estaAtiva;
        }
    }

    public static class Endereco {
        public String cep, logradouro, numero, complemento, bairro, cidade;
    }

    public static class RotaEnd {
        public String rotaId, enderecoId, nomeLocal, tipoLocal;
        public Integer ordem;

        public RotaEnd(
                String rotaId,
                String enderecoId,
                String nomeLocal,
                String tipoLocal,
                Integer ordem) {
            this.rotaId = rotaId;
            this.enderecoId = enderecoId;
            this.nomeLocal = nomeLocal;
            this.tipoLocal = tipoLocal;
            this.ordem = ordem;
        }
    }

    /** POST /equipes e PUT /equipes/{id} (Requests.EquipeRequest). */
    public static class Equipe {
        public String gestorId, nome;
        public Boolean estaAtiva;

        public Equipe(String gestorId, String nome, Boolean estaAtiva) {
            this.gestorId = gestorId;
            this.nome = nome;
            this.estaAtiva = estaAtiva;
        }
    }

    /** POST /equipes-cooperados (Requests.EquipeCooperadoRequest). */
    public static class EquipeCooperado {
        public String equipeId, cooperadoId;

        public EquipeCooperado(String equipeId, String cooperadoId) {
            this.equipeId = equipeId;
            this.cooperadoId = cooperadoId;
        }
    }

    public static class Aceitar {
        public String pedidoCooperativaId, statusAceitoId;

        public Aceitar(String pedidoCooperativaId, String statusAceitoId) {
            this.pedidoCooperativaId = pedidoCooperativaId;
            this.statusAceitoId = statusAceitoId;
        }
    }
}
