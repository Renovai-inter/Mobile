package com.example.renovai.model;

/**
 * Um Telefone pertence exclusivamente a um Perfil OU a um Usuario
 * (nunca aos dois), conforme a constraint chk_telefones_dono do banco.
 */
public class Telefone {

    private String telefoneId;
    private String perfilId;
    private String usuarioId;
    private String ddd;
    private String telefone;

    public Telefone() {
    }

    public Telefone(String telefoneId, String perfilId, String usuarioId, String ddd, String telefone) {
        this.telefoneId = telefoneId;
        this.perfilId = perfilId;
        this.usuarioId = usuarioId;
        this.ddd = ddd;
        this.telefone = telefone;
    }

    public String getTelefoneId() {
        return telefoneId;
    }

    public void setTelefoneId(String telefoneId) {
        this.telefoneId = telefoneId;
    }

    public String getPerfilId() {
        return perfilId;
    }

    public void setPerfilId(String perfilId) {
        this.perfilId = perfilId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getDdd() {
        return ddd;
    }

    public void setDdd(String ddd) {
        this.ddd = ddd;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    @Override
    public String toString() {
        return "Telefone{" +
                "telefoneId='" + telefoneId + '\'' +
                ", ddd='" + ddd + '\'' +
                ", telefone='" + telefone + '\'' +
                '}';
    }
}
