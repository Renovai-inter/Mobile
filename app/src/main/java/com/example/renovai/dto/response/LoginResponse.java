package com.example.renovai.dto.response;

/**
 * Espelha Responses.LoginResponse. Retorno de POST /auth/login.
 *
 * ATUALIZAÇÃO (área Cooperado): campo "usuarioId" adicionado — a API já devolvia
 * esse valor, mas o DTO não capturava. É essencial para a área de Cooperado:
 * é o único jeito de descobrir qual Funcionario está logado (ver FuncionarioResolver).
 *
 * Atenção — comportamento observado em AuthService.login() (backend):
 *  - Login de Funcionario (cooperado/motorista/gestor de cooperativa): usuarioId
 *    = Usuario.usuarioId de verdade.
 *  - Login de Perfil (Empresa ou admin de Cooperativa): o MESMO campo vem
 *    preenchido com perfil.getPerfilId() (a API reaproveita o campo). Por isso,
 *    em AuthController, só tentamos resolver o Funcionario quando o "role" não é
 *    GESTOR_EMPRESA nem ADMIN_COOPERATIVA.
 */
public class LoginResponse {
    private String token;
    private String tipo;
    private String email;
    private String role;
    private String usuarioId;

    public String getToken() { return token; }
    public String getTipo() { return tipo; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getUsuarioId() { return usuarioId; }
}
