package com.example.renovai.model;

public class EmpresaMaterialInteresse {

    private String empresaMaterialId;
    private String empresaId;
    private String categoriaId;

    public EmpresaMaterialInteresse() {
    }

    public EmpresaMaterialInteresse(String empresaMaterialId, String empresaId, String categoriaId) {
        this.empresaMaterialId = empresaMaterialId;
        this.empresaId = empresaId;
        this.categoriaId = categoriaId;
    }

    public String getEmpresaMaterialId() {
        return empresaMaterialId;
    }

    public void setEmpresaMaterialId(String empresaMaterialId) {
        this.empresaMaterialId = empresaMaterialId;
    }

    public String getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(String empresaId) {
        this.empresaId = empresaId;
    }

    public String getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(String categoriaId) {
        this.categoriaId = categoriaId;
    }

    @Override
    public String toString() {
        return "EmpresaMaterialInteresse{" +
                "empresaMaterialId='" + empresaMaterialId + '\'' +
                ", empresaId='" + empresaId + '\'' +
                ", categoriaId='" + categoriaId + '\'' +
                '}';
    }
}
