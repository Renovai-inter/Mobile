package com.example.renovai.controller;

/**
 * Erro de cadastro que precisa de explicação, mostrado num pop-up. Erros de digitação (campo vazio,
 * CPF com dígito errado...) não viram FalhaCadastro: continuam como Toast.
 */
public final class FalhaCadastro {

    public enum Tipo {
        /** O e-mail já tem conta: o caminho é entrar pelo login. */
        CONTA_EXISTENTE,
        /** CPF, CNPJ ou outro dado já pertence a outra conta (422 "já cadastrado" ou 409). */
        DADOS_EM_USO,
        /** Erro 5xx: problema do servidor, nada foi salvo. */
        SERVIDOR,
        /** Não chegou resposta (sem internet, ou o servidor demorou para acordar). */
        CONEXAO,
        OUTRO
    }

    public final Tipo tipo;
    public final String titulo;
    public final String mensagem;
    /** O que a pessoa pode fazer; pode ser null. */
    public final String dica;

    FalhaCadastro(Tipo tipo, String titulo, String mensagem, String dica) {
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.dica = dica;
    }

    static FalhaCadastro conexao() {
        return new FalhaCadastro(
                Tipo.CONEXAO,
                "Sem conexão com o servidor",
                "Não conseguimos falar com o servidor do Renovai, então o cadastro não foi enviado.",
                "Confira sua internet. Se o app ficou um tempo sem uso, o servidor pode levar até 1"
                        + " minuto para acordar: é só tentar de novo.");
    }
}
