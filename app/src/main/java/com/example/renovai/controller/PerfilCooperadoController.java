package com.example.renovai.controller;

import com.example.renovai.CooperadoSession;
import com.example.renovai.FuncionarioResolver;
import com.example.renovai.SessionManager;

/**
 * Controller de Perfil para a área de Cooperado (tela 2.4).
 *
 * Na maioria das vezes NÃO precisa chamar a API: os dados (funcionarioId,
 * cooperativaNome, cargo, nome) já foram resolvidos e guardados em
 * CooperadoSession logo após o login (ver AuthController). Este controller só
 * lê esse cache e o e-mail salvo em SessionManager.
 *
 * Se, por algum motivo, o cache estiver vazio (ex.: uma falha de rede passageira
 * no momento do login), tenta resolver de novo aqui usando o usuarioId que foi
 * salvo à parte (ver CooperadoSession.getUsuarioId()). Se nem isso existir, não
 * há como identificar o cooperado sem um novo login — a API não expõe um jeito
 * de buscar o Funcionario pelo e-mail (ver FuncionarioResolver).
 */
public class PerfilCooperadoController {

    public static class PerfilCooperado {
        public final String nome;
        public final String email;
        public final String cargo;
        public final String cooperativaNome;

        public PerfilCooperado(String nome, String email, String cargo, String cooperativaNome) {
            this.nome = nome;
            this.email = email;
            this.cargo = cargo;
            this.cooperativaNome = cooperativaNome;
        }
    }

    public interface Callback {
        void onSuccess(PerfilCooperado perfil);
        void onErro(String mensagem);
    }

    public void carregarPerfil(Callback callback) {
        if (CooperadoSession.temCooperadoResolvido()) {
            entregar(callback);
            return;
        }

        String usuarioId = CooperadoSession.getUsuarioId();
        FuncionarioResolver.resolverPorUsuarioId(usuarioId, funcionario -> {
            if (funcionario == null) {
                callback.onErro("Não foi possível identificar o cooperado. Faça login novamente.");
                return;
            }
            CooperadoSession.salvar(
                    funcionario.getFuncionarioId(),
                    funcionario.getCooperativaId(),
                    funcionario.getCooperativaNome(),
                    funcionario.getCargo(),
                    funcionario.getUsuarioNome());
            entregar(callback);
        });
    }

    private void entregar(Callback callback) {
        callback.onSuccess(new PerfilCooperado(
                CooperadoSession.getUsuarioNome(),
                SessionManager.getEmail(),
                CooperadoSession.getCargo(),
                CooperadoSession.getCooperativaNome()));
    }
}
