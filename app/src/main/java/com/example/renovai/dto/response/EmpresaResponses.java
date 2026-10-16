package com.example.renovai.dto.response;

import java.util.List;

/**
 * DTOs específicos da área da Empresa. Espelham com.renovai.api.dto.response.Responses / o patch
 * EmpresaContaController.
 */
public class EmpresaResponses {

    public static class Dashboard {
        public Long totalPedidosEnviados, totalPedidosAceitos, totalCooperativasFavoritadas;
        public Double valorTotalNegociado;
    }

    /** Vem de GET/PATCH /empresas-conta/meu-perfil (patch do backend). */
    public static class MeuPerfil {
        public String perfilId,
                empresaId,
                nomeEmpresa,
                descricao,
                imagemUrl,
                email,
                cnpj,
                endereco,
                telefone,
                cidade,
                materialId,
                categoriaId,
                categoriaNome;
    }

    public static class CooperativaPerfilPublico {
        public String cooperativaId,
                nome,
                descricao,
                imagemUrl,
                contatoPreferencial,
                horarioFuncionamento,
                cidade,
                endereco,
                email,
                telefone;

        public String contato() {
            return telefone != null && !telefone.isBlank() ? telefone : email;
        }

        public Double mediaAvaliacoes;
        public Long totalAvaliacoes;
        public List<GestorResponses.Estoque> materiaisDisponiveis;
    }

    public static class DistribuicaoEstrelas {
        public long estrelas0, estrelas1, estrelas2, estrelas3, estrelas4, estrelas5;
    }

    public static class Avaliacao {
        public String avaliacaoId,
                avaliadorId,
                avaliadorNome,
                avaliadoId,
                pedidoId,
                comentario,
                dataAvaliacao;
        public Integer nota;
    }

    public static class Favorito {
        public String favoritoId,
                empresaId,
                cooperativaId,
                cooperativaNome,
                cooperativaImagem,
                dataCriacao;
    }

    public static class MaterialInteresse {
        public String empresaMaterialId, empresaId, categoriaId, categoriaNome;
    }
}
