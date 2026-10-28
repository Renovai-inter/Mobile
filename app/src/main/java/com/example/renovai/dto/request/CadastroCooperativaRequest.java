package com.example.renovai.dto.request;

/**
 * POST /auth/cadastro-cooperativa. A API exige CPF (000.000.000-00), CNPJ (00.000.000/0000-00) e
 * CEP (00000-000) já formatados.
 */
public class CadastroCooperativaRequest {

    private final String nomeResponsavel;
    private final String cpfResponsavel;
    private final String nome;
    private final String contatoPreferencial;
    private final String email;
    private final String cnpj;
    private final String senha;
    private final Endereco endereco;

    public CadastroCooperativaRequest(
            String nomeResponsavel,
            String cpfResponsavel,
            String nome,
            String contatoPreferencial,
            String email,
            String cnpj,
            String senha,
            Endereco endereco) {
        this.nomeResponsavel = nomeResponsavel;
        this.cpfResponsavel = cpfResponsavel;
        this.nome = nome;
        this.contatoPreferencial = contatoPreferencial;
        this.email = email;
        this.cnpj = cnpj;
        this.senha = senha;
        this.endereco = endereco;
    }

    public static class Endereco {
        private final String cep;
        private final String logradouro;
        private final String numero;
        private final String complemento;
        private final String bairro;
        private final String cidade;

        public Endereco(String cep, String logradouro, String numero, String complemento, String bairro, String cidade) {
            this.cep = cep;
            this.logradouro = logradouro;
            this.numero = numero;
            this.complemento = complemento;
            this.bairro = bairro;
            this.cidade = cidade;
        }
    }
}
