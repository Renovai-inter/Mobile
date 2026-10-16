package com.example.renovai.dto.request;

/**
 * Corpos de requisição específicos da área da Empresa (espelham Requests.java / o patch
 * EmpresaContaController).
 */
public class EmpresaRequests {

    public static class Empresa {
        public String nome, descricao, materialId, imagemUrl;

        public Empresa(String nome, String descricao, String imagemUrl) {
            this.nome = nome;
            this.descricao = descricao;
            this.imagemUrl = imagemUrl;
        }
    }

    /** PATCH /empresas-conta/meu-perfil (patch do backend) — todos os campos são opcionais. */
    public static class AtualizarMeuPerfil {
        public String nomeEmpresa, email, cnpj, endereco, telefone, descricao, cidade;
    }

    public static class Avaliacao {
        public String pedidoId, cooperativaId, comentario;
        public Integer nota;

        public Avaliacao(
                String avaliadorId,
                String avaliadoId,
                String pedidoId,
                Integer nota,
                String comentario) {
            this.cooperativaId = avaliadoId;
            this.pedidoId = pedidoId;
            this.nota = nota;
            this.comentario = comentario;
        }
    }

    public static class Favorito {
        public String cooperativaId;

        public Favorito(String cooperativaId) {
            this.cooperativaId = cooperativaId;
        }
    }

    public static class SubstituirInteresses {
        public final java.util.List<String> categoriaIds;

        public SubstituirInteresses(java.util.List<String> categoriaIds) {
            this.categoriaIds = new java.util.ArrayList<>(categoriaIds);
        }
    }

    public static class MaterialInteresse {
        public String empresaId, categoriaId;

        public MaterialInteresse(String empresaId, String categoriaId) {
            this.empresaId = empresaId;
            this.categoriaId = categoriaId;
        }
    }

    public static class PedidoItem {
        public String materialId;
        public java.math.BigDecimal quantidadeKg, precoUnitario;

        public PedidoItem(String materialId, double peso, double preco) {
            this.materialId = materialId;
            this.quantidadeKg = java.math.BigDecimal.valueOf(peso);
            this.precoUnitario = java.math.BigDecimal.valueOf(preco);
        }
    }

    public static class EnviarPedido {
        public String cooperativaId, chaveSolicitacao;
        public java.util.List<PedidoItem> itens = new java.util.ArrayList<>();
    }

    public static class Pedido {
        public String empresaId, observacao;

        public Pedido(String empresaId, String observacao) {
            this.empresaId = empresaId;
            this.observacao = observacao;
        }
    }

    public static class Item {
        public String pedidoId, materialId;
        public Double quantidadeKg, precoUnitario;

        public Item(String pedidoId, String materialId, Double quantidadeKg, Double precoUnitario) {
            this.pedidoId = pedidoId;
            this.materialId = materialId;
            this.quantidadeKg = quantidadeKg;
            this.precoUnitario = precoUnitario;
        }
    }

    public static class PedidoCooperativa {
        public String pedidoId, cooperativaId, statusId;

        public PedidoCooperativa(String pedidoId, String cooperativaId, String statusId) {
            this.pedidoId = pedidoId;
            this.cooperativaId = cooperativaId;
            this.statusId = statusId;
        }
    }
}
