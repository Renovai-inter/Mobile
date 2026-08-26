package com.example.renovai;

import com.example.renovai.dto.request.LoginRequest;
import com.example.renovai.dto.response.CooperativaResponse;
import com.example.renovai.dto.response.LoginResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * NÃO é uma classe de produção — é um exemplo comentado de como usar as
 * peças acima dentro de uma Activity/Fragment/Controller. Apague ou adapte
 * conforme a estrutura MVC de vocês.
 */
public class ExemploDeUso {

    // 1) LOGIN — precisa ser feito antes de qualquer outra chamada,
    // porque as demais rotas exigem o header Authorization (ver SecurityConfig).
    public void fazerLogin(String email, String senha) {
        AuthApiService authService = ApiClient.createService(AuthApiService.class);

        LoginRequest request = new LoginRequest(email, senha);

        authService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse body = response.body();

                    // Salva o token — o AuthInterceptor vai usá-lo
                    // automaticamente em todas as próximas chamadas.
                    SessionManager.salvarSessao(body.getToken(), body.getEmail(), body.getRole());

                    // Aqui você navegaria pra tela principal, por exemplo.
                    buscarCooperativas();
                } else if (response.code() == 401) {
                    // Email ou senha incorretos (GlobalExceptionHandler
                    // do backend retorna 401 nesse caso)
                } else {
                    // Outro erro — response.errorBody() tem os detalhes
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                // Sem internet, API fora do ar, timeout, etc.
            }
        });
    }

    // 2) BUSCAR DADOS — qualquer chamada feita DEPOIS do login já sai
    // com o header "Authorization: Bearer <token>" automaticamente,
    // porque o AuthInterceptor foi registrado no OkHttpClient do ApiClient.
    public void buscarCooperativas() {
        CooperativaApiService cooperativaService =
                ApiClient.createService(CooperativaApiService.class);

        cooperativaService.listar().enqueue(new Callback<List<CooperativaResponse>>() {
            @Override
            public void onResponse(Call<List<CooperativaResponse>> call,
                                    Response<List<CooperativaResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<CooperativaResponse> cooperativas = response.body();
                    // Atualize sua RecyclerView/lista aqui.
                }
            }

            @Override
            public void onFailure(Call<List<CooperativaResponse>> call, Throwable t) {
                // trate o erro
            }
        });
    }

    // 3) LOGOUT — só precisa limpar o token local. O backend não guarda
    // sessão (é stateless via JWT), então não existe uma rota que
    // "invalida" o token no servidor — ele expira sozinho (JWT_EXPIRATION).
    public void logout() {
        SessionManager.logout();
    }
}
