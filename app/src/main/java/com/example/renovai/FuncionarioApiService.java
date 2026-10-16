package com.example.renovai;

import com.example.renovai.dto.response.FuncionarioResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Consome /funcionarios/** (com.renovai.api.controller.FuncionarioController).
 *
 * ATUALIZAÇÃO: o backend passou a expor GET /funcionarios/por-usuario/{usuarioId}
 * (endpoint novo, criado por causa do problema abaixo) — FuncionarioResolver usa
 * esse endpoint em vez de listarTodos() + filtro no app. listarTodos() continua
 * aqui só por completude, mas tem um problema conhecido: se QUALQUER funcionário
 * no banco tiver uma relação quebrada (usuario/cargo/cooperativa nula), a rota
 * inteira responde 500 — foi exatamente isso que quebrou o login em produção.
 * O endpoint novo não sofre disso, por isso é o preferido.
 */
public interface FuncionarioApiService {

    @GET("funcionarios")
    Call<List<FuncionarioResponse>> listarTodos();

    @GET("funcionarios/por-usuario/{usuarioId}")
    Call<FuncionarioResponse> buscarPorUsuarioId(@Path("usuarioId") String usuarioId);
}
