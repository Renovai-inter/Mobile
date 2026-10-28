package com.example.renovai;

/**
 * Validação e formatação de CPF, CNPJ e CEP.
 *
 * <p>A validação é a mesma da API (EmpresaSchemaService.validarDocumento): 11/14 números, não
 * todos iguais, e os dois dígitos verificadores calculados pelo módulo 11 batendo com os dois
 * últimos números. Conferir aqui evita o "Documento inválido." genérico da API, que não diz se o
 * problema é o CPF ou o CNPJ.
 */
public final class Documento {

    private Documento() {}

    public static String numeros(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    public static boolean cpfValido(String cpf) {
        return valido(numeros(cpf), 11);
    }

    public static boolean cnpjValido(String cnpj) {
        return valido(numeros(cnpj), 14);
    }

    /** 000.000.000-00 (formato exigido pelo POST /auth/cadastro-cooperativa). */
    public static String formatarCpf(String cpf) {
        String d = numeros(cpf);
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }

    /** 00.000.000/0000-00 (formato exigido pelo POST /auth/cadastro-cooperativa). */
    public static String formatarCnpj(String cnpj) {
        String d = numeros(cnpj);
        return d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8) + "/" + d.substring(8, 12) + "-" + d.substring(12);
    }

    /** 00000-000. */
    public static String formatarCep(String cep) {
        String d = numeros(cep);
        return d.substring(0, 5) + "-" + d.substring(5);
    }

    private static boolean valido(String d, int tamanho) {
        if (d.length() != tamanho || d.chars().distinct().count() == 1) return false;
        // CPF: pesos 10..2 e depois 11..2. CNPJ: 5,4,3,2,9..2 e depois 6,5,4,3,2,9..2.
        int[] pesos1 = tamanho == 11
                ? new int[] {10, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[] {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesos2 = tamanho == 11
                ? new int[] {11, 10, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[] {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        return digito(d, pesos1) == d.charAt(pesos1.length) - '0'
                && digito(d, pesos2) == d.charAt(pesos2.length) - '0';
    }

    private static int digito(String d, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) soma += (d.charAt(i) - '0') * pesos[i];
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
