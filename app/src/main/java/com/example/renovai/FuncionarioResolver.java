package com.example.renovai;

import com.example.renovai.dto.response.FuncionarioResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Descobre o Funcionario (cooperado) de quem logou, usando
 * GET /funcionarios/por-usuario/{usuarioId} — endpoint dedicado, criado no
 * backend especificamente pra isso (ver IMPLEMENTACAO.md / histórico do chat).
 *
 * ATUALIZAÇÃO: antes, isso era feito com GET /funcionarios (listarTodos) +
 * filtro no app, porque não existia um jeito direto de buscar por usuarioId.
 * Isso quebrou em produção: um único funcionário com relação quebrada no banco
 * (usuario/cargo/cooperativa nula) derrubava a lista inteira com 500, o que
 * impedia QUALQUER cooperado de logar, mesmo com os próprios dados corretos.
 * O endpoint novo busca só o registro necessário, então não sofre com isso.
 *
 * Só funciona depois de um login bem-sucedido (precisa do token já salvo,
 * porque o AuthInterceptor anexa ele em toda chamada). Se a conta logada não
 * for de um Funcionario (ex.: é uma conta de Empresa) ou o usuarioId não tiver
 * um Funcionario correspondente, a API responde 404 e o resultado vem null —
 * não é tratado como erro.
 */
public final class FuncionarioResolver {

    public interface Callback {
        void onResultado(FuncionarioResponse funcionario); // pode vir null
    }

    private FuncionarioResolver() {
    }

    public static void resolverPorUsuarioId(String usuarioId, Callback callback) {
        if (usuarioId == null || usuarioId.trim().isEmpty()) {
            callback.onResultado(null);
            return;
        }

        FuncionarioApiService service = ApiClient.createService(FuncionarioApiService.class);
        service.buscarPorUsuarioId(usuarioId).enqueue(new retrofit2.Callback<FuncionarioResponse>() {
            @Override
            public void onResponse(Call<FuncionarioResponse> call, Response<FuncionarioResponse> response) {
                // 404 (sem Funcionario pra esse usuarioId, ex.: conta de Empresa)
                // e qualquer outro erro caem no mesmo "null" — quem chama decide
                // o que fazer (ver PerfilCooperadoController / AuthController).
                callback.onResultado(response.isSuccessful() ? response.body() : null);
            }

            @Override
            public void onFailure(Call<FuncionarioResponse> call, Throwable t) {
                callback.onResultado(null);
            }
        });
    }
}
