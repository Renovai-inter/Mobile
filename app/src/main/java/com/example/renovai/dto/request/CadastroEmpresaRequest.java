package com.example.renovai.dto.request;

public class CadastroEmpresaRequest {

    private final String cpf;
    private final String nome;
    private final String email;
    private final String telefone;
    private final String senha;
    private final String nomeEmpresa;
    private final String cnpj;
    private final String endereco;

    public CadastroEmpresaRequest(
            String nome,
            String cpf,
            String email,
            String telefone,
            String senha,
            String nomeEmpresa,
            String cnpj,
            String endereco) {
        this.nome = nome;
        this.cpf = cpf;
        this.email = email;
        this.telefone = telefone;
        this.senha = senha;
        this.nomeEmpresa = nomeEmpresa;
        this.cnpj = cnpj;
        this.endereco = endereco;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getSenha() {
        return senha;
    }

    public String getNomeEmpresa() {
        return nomeEmpresa;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getEndereco() {
        return endereco;
    }
}
