package com.example.renovai;

/** Categorias explícitas para upload, cache e apresentação. */
public enum TipoImagem {
    COLETA("coletas"), TRIAGEM("triagens"), PERFIL("perfis"),
    COOPERATIVA("cooperativas"), EMPRESA("empresas"), MATERIAL("materiais"),
    LOGO("logos"), OUTRA("outras");

    private final String categoria;
    TipoImagem(String categoria) { this.categoria = categoria; }
    public String pastaCloudinary() { return "Renovai/" + categoria; }
    public String chaveCache() { return "renovai-imagem-v2/" + categoria; }
}
