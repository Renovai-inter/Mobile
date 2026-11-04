package com.example.renovai;

/** Validação de apresentação; a API permanece responsável pelas regras cadastrais. */
public final class PerfilEmpresaValidacao {
    private PerfilEmpresaValidacao() { }
    public static String erro(String campo, String valor) {
        String v = valor == null ? "" : valor.trim();
        if (v.isEmpty() && ("nomeEmpresa".equals(campo) || "email".equals(campo)
                || "cnpj".equals(campo) || "endereco".equals(campo)
                || "telefone".equals(campo))) return "Preencha este campo obrigatório.";
        int limite = "telefone".equals(campo) ? 20 : "cidade".equals(campo) ? 100
                : "descricao".equals(campo) ? Integer.MAX_VALUE : 255;
        if (v.length() > limite) return "Use até " + limite + " caracteres.";
        if ("email".equals(campo) && !v.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            return "Informe um e-mail válido, como nome@empresa.com.";
        if ("cnpj".equals(campo) && !v.replaceAll("\\D", "").matches("[0-9]{14}"))
            return "Informe os 14 números do CNPJ.";
        if ("telefone".equals(campo) && !v.isEmpty()
                && (!v.matches("[+()0-9 .-]+") || v.replaceAll("\\D", "").length() < 10
                || v.replaceAll("\\D", "").length() > 15))
            return "Informe o telefone com DDD.";
        return null;
    }
}
