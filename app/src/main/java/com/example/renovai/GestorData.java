package com.example.renovai;

import android.os.Handler;
import android.os.Looper;

import com.example.renovai.dto.request.GestorRequests;
import com.example.renovai.dto.response.ColetaResponse;
import com.example.renovai.dto.response.FuncionarioResponse;
import com.example.renovai.dto.response.GestorResponses;
import com.example.renovai.dto.response.GestorResponses.FuncionarioDetalhe;
import com.example.renovai.dto.response.GestorResponses.Negociacao;
import com.example.renovai.dto.response.PerfilResponse;
import com.example.renovai.dto.response.RateioListaResponse;
import com.example.renovai.dto.response.TriagemResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Camada de dados da área do Gestor: cada método entrega PRIMEIRO o que já está no GestorCache
 * (doCache = true), se houver, e DEPOIS o resultado novo da API (doCache = false). A tela só
 * precisa renderizar de forma idempotente a cada chamada.
 */
public final class GestorData {

    public interface Ouvinte<T> {
        void aoReceber(T dados, boolean doCache);

        default void aoErro(String mensagem) {}
    }

    private GestorData() {}

    public static GestorApiService api() {
        return ApiClient.createService(GestorApiService.class);
    }

    public static String coop() {
        return CooperadoSession.getCooperativaId();
    }

    public static String funcionarioId() {
        return CooperadoSession.getFuncionarioId();
    }

    private static <T> void buscar(
            String chave, boolean forcar, Supplier<Call<T>> fabrica, Ouvinte<T> o) {
        if (coop() == null) {
            o.aoErro("Não foi possível identificar a cooperativa do gestor. Faça login novamente.");
            return;
        }
        final T cache = GestorCache.obter(chave);
        final long sessao = GestorCache.sessao();
        if (cache != null) o.aoReceber(cache, true);
        if (cache != null && !forcar && GestorCache.valido(chave)) return;

        fabrica.get()
                .enqueue(
                        new Callback<T>() {
                            @Override
                            public void onResponse(Call<T> call, Response<T> r) {
                                if (sessao != GestorCache.sessao()) return;
                                if (r.isSuccessful() && r.body() != null) {
                                    GestorCache.guardar(chave, r.body());
                                    o.aoReceber(r.body(), false);
                                } else if (cache == null) {
                                    o.aoErro("Erro " + r.code() + " ao carregar os dados.");
                                }
                            }

                            @Override
                            public void onFailure(Call<T> call, Throwable t) {
                                if (sessao != GestorCache.sessao()) return;
                                if (cache == null) o.aoErro("Sem conexão com o servidor.");
                            }
                        });
    }

    public static void coletas(boolean forcar, Ouvinte<List<ColetaResponse>> o) {
        buscar(GestorCache.COLETAS, forcar, () -> api().coletas(coop()), o);
    }

    public static void triagens(boolean forcar, Ouvinte<List<TriagemResponse>> o) {
        buscar(GestorCache.TRIAGENS, forcar, () -> api().triagens(coop()), o);
    }

    public static void estoque(boolean forcar, Ouvinte<List<GestorResponses.Estoque>> o) {
        buscar(GestorCache.ESTOQUE, forcar, () -> api().estoque(coop()), o);
    }

    public static void negociacoes(boolean forcar, Ouvinte<List<Negociacao>> o) {
        buscar(GestorCache.NEGOCIACOES, forcar, () -> api().negociacoes(coop()), o);
    }

    public static void pedidosCoop(boolean forcar, Ouvinte<List<GestorResponses.PedidoCoop>> o) {
        buscar(GestorCache.PEDIDOS_COOP, forcar, () -> api().pedidosCoop(coop()), o);
    }

    public static void rateios(boolean forcar, Ouvinte<List<RateioListaResponse>> o) {
        buscar(GestorCache.RATEIOS, forcar, () -> api().rateios(coop()), o);
    }

    public static void rotas(boolean forcar, Ouvinte<List<GestorResponses.Rota>> o) {
        buscar(GestorCache.ROTAS, forcar, () -> api().rotas(coop()), o);
    }

    public static void status(Ouvinte<List<GestorResponses.Status>> o) {
        buscar(GestorCache.STATUS, false, () -> api().status(), o);
    }

    public static void cargos(Ouvinte<List<GestorResponses.Cargo>> o) {
        buscar(GestorCache.CARGOS, false, () -> api().cargos(), o);
    }

    /**
     * Funcionários da cooperativa (tela 4.9): GET /funcionarios/por-cooperativa/{id} + GET
     * /funcionarios/pre-cadastro/incompletos/por-cooperativa/{id} (para marcar quem ainda não
     * completou o cadastro).
     */
    public static void funcionarios(boolean forcar, Ouvinte<List<FuncionarioDetalhe>> o) {
        if (coop() == null) {
            o.aoErro("Não foi possível identificar a cooperativa do gestor. Faça login novamente.");
            return;
        }
        final List<FuncionarioDetalhe> cache = GestorCache.obter(GestorCache.FUNCIONARIOS);
        if (cache != null) o.aoReceber(cache, true);
        if (cache != null && !forcar && GestorCache.valido(GestorCache.FUNCIONARIOS)) return;
        carregarFuncionarios(cache, o);
    }

    private static void carregarFuncionarios(
            List<FuncionarioDetalhe> cache, Ouvinte<List<FuncionarioDetalhe>> o) {
        final long sessao = GestorCache.sessao();
        api().funcionariosBase(coop())
                .enqueue(
                        new Callback<List<FuncionarioResponse>>() {
                            @Override
                            public void onResponse(
                                    Call<List<FuncionarioResponse>> call,
                                    Response<List<FuncionarioResponse>> r) {
                                if (sessao != GestorCache.sessao()) return;
                                if (!r.isSuccessful() || r.body() == null) {
                                    if (cache == null)
                                        o.aoErro("Erro " + r.code() + " ao carregar funcionários.");
                                    return;
                                }
                                final List<FuncionarioResponse> base = r.body();
                                api().preCadastros(coop())
                                        .enqueue(
                                                new Callback<List<GestorResponses.PreCadastro>>() {
                                                    @Override
                                                    public void onResponse(
                                                            Call<List<GestorResponses.PreCadastro>>
                                                                    c2,
                                                            Response<
                                                                            List<
                                                                                    GestorResponses
                                                                                            .PreCadastro>>
                                                                    r2) {
                                                        if (sessao != GestorCache.sessao()) return;
                                                        if (!r2.isSuccessful()
                                                                || r2.body() == null) {
                                                            o.aoErro(
                                                                    "Não foi possível confirmar os"
                                                                        + " pré-cadastros dos"
                                                                        + " funcionários.");
                                                            return;
                                                        }
                                                        Set<String> pendentes = new HashSet<>();
                                                        if (r2.isSuccessful()
                                                                && r2.body() != null) {
                                                            for (GestorResponses.PreCadastro p :
                                                                    r2.body())
                                                                pendentes.add(p.funcionarioId);
                                                        }
                                                        entregar(base, pendentes, o);
                                                    }

                                                    @Override
                                                    public void onFailure(
                                                            Call<List<GestorResponses.PreCadastro>>
                                                                    c2,
                                                            Throwable t) {
                                                        if (sessao != GestorCache.sessao()) return;
                                                        o.aoErro(
                                                                "Não foi possível confirmar os"
                                                                    + " pré-cadastros dos"
                                                                    + " funcionários.");
                                                    }
                                                });
                            }

                            @Override
                            public void onFailure(
                                    Call<List<FuncionarioResponse>> call, Throwable t) {
                                if (sessao != GestorCache.sessao()) return;
                                if (cache == null) o.aoErro("Sem conexão com o servidor.");
                            }
                        });
    }

    private static void entregar(
            List<FuncionarioResponse> base,
            Set<String> pendentes,
            Ouvinte<List<FuncionarioDetalhe>> o) {
        List<FuncionarioDetalhe> lista = new ArrayList<>();
        for (FuncionarioResponse f : base) {
            FuncionarioDetalhe d = new FuncionarioDetalhe();
            d.funcionarioId = f.getFuncionarioId();
            d.usuarioId = f.getUsuarioId();
            d.nome = f.getUsuarioNome();
            d.cargoId = f.getCargoId();
            d.cargo = f.getCargo();
            d.cooperativaId = f.getCooperativaId();
            d.cooperativaNome = f.getCooperativaNome();
            d.statusFuncionario = f.getStatusFuncionario();
            d.estaAtivo = f.getEstaAtivo();
            d.pendente = pendentes.contains(f.getFuncionarioId());
            lista.add(d);
        }
        GestorCache.guardar(GestorCache.FUNCIONARIOS, lista);
        o.aoReceber(lista, false);
    }

    /**
     * perfilId da conta da cooperativa: é o "remetente" das mensagens do chat (NegociacaoController
     * exige um Perfil).
     */
    public static void perfilDaCooperativa(Ouvinte<String> o) {
        String cache = GestorCache.obter(GestorCache.PERFIL_COOP);
        if (cache != null) {
            o.aoReceber(cache, true);
            return;
        }
        final long sessao = GestorCache.sessao();
        api().perfis()
                .enqueue(
                        new Callback<List<PerfilResponse>>() {
                            @Override
                            public void onResponse(
                                    Call<List<PerfilResponse>> call,
                                    Response<List<PerfilResponse>> r) {
                                if (sessao != GestorCache.sessao()) return;
                                if (r.isSuccessful() && r.body() != null) {
                                    for (PerfilResponse p : r.body()) {
                                        if (coop() != null
                                                && coop().equalsIgnoreCase(p.getCooperativaId())) {
                                            GestorCache.guardar(
                                                    GestorCache.PERFIL_COOP, p.getPerfilId());
                                            o.aoReceber(p.getPerfilId(), false);
                                            return;
                                        }
                                    }
                                }
                                o.aoErro(
                                        "Não foi possível identificar a conta da cooperativa para"
                                            + " enviar mensagens.");
                            }

                            @Override
                            public void onFailure(Call<List<PerfilResponse>> call, Throwable t) {
                                if (sessao != GestorCache.sessao()) return;
                                o.aoErro("Sem conexão com o servidor.");
                            }
                        });
    }

    /**
     * Devolve [lucroTotal, despesasTotais] do mês. Tenta a função do banco; se falhar, soma as
     * negociações concluídas.
     */
    public static void financeiroMes(YearMonth mes, Ouvinte<double[]> o) {
        final String ini =
                mes.atDay(1)
                        .atStartOfDay()
                        .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        final String fim =
                mes.atEndOfMonth()
                        .atTime(23, 59, 59)
                        .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        final double[] valores = new double[2];
        final AtomicInteger pend = new AtomicInteger(2);
        final AtomicBoolean falhou = new AtomicBoolean();
        final Runnable terminar =
                () -> {
                    if (pend.decrementAndGet() == 0) {
                        if (falhou.get())
                            o.aoErro(
                                    "Não foi possível confirmar os valores financeiros do"
                                        + " período.");
                        else o.aoReceber(valores, false);
                    }
                };
        api().totalAcumulado(new GestorRequests.Financeiro(coop(), ini, fim))
                .enqueue(
                        new Callback<GestorResponses.TotalAcumulado>() {
                            public void onResponse(
                                    Call<GestorResponses.TotalAcumulado> c,
                                    Response<GestorResponses.TotalAcumulado> r) {
                                if (r.isSuccessful()
                                        && r.body() != null
                                        && r.body().totalAcumulado != null) {
                                    valores[0] = r.body().totalAcumulado;
                                    terminar.run();
                                } else fallback();
                            }

                            public void onFailure(
                                    Call<GestorResponses.TotalAcumulado> c, Throwable t) {
                                fallback();
                            }

                            private void fallback() {
                                api().negociacoes(coop())
                                        .enqueue(
                                                new Callback<List<Negociacao>>() {
                                                    public void onResponse(
                                                            Call<List<Negociacao>> c,
                                                            Response<List<Negociacao>> r) {
                                                        if (r.isSuccessful() && r.body() != null)
                                                            valores[0] =
                                                                    GestorUi.somaConcluidas(
                                                                            r.body(), mes);
                                                        else falhou.set(true);
                                                        terminar.run();
                                                    }

                                                    public void onFailure(
                                                            Call<List<Negociacao>> c, Throwable t) {
                                                        falhou.set(true);
                                                        terminar.run();
                                                    }
                                                });
                            }
                        });
        api().totalDespesas(coop(), mes.atDay(1).toString())
                .enqueue(
                        new Callback<GestorResponses.TotalDespesas>() {
                            public void onResponse(
                                    Call<GestorResponses.TotalDespesas> c,
                                    Response<GestorResponses.TotalDespesas> r) {
                                if (r.isSuccessful()
                                        && r.body() != null
                                        && r.body().totalGeral != null)
                                    valores[1] = r.body().totalGeral;
                                else falhou.set(true);
                                terminar.run();
                            }

                            public void onFailure(
                                    Call<GestorResponses.TotalDespesas> c, Throwable t) {
                                falhou.set(true);
                                terminar.run();
                            }
                        });
    }

    /**
     * Pré-carrega as listagens durante o login. Chama aoTerminar uma única vez (ou após 15 s, o que
     * vier primeiro).
     */
    public static void precarregar(Runnable aoTerminar) {
        if (coop() == null) {
            aoTerminar.run();
            return;
        }
        final AtomicBoolean feito = new AtomicBoolean(false);
        final Runnable concluir =
                () -> {
                    if (feito.compareAndSet(false, true)) aoTerminar.run();
                };
        new Handler(Looper.getMainLooper()).postDelayed(concluir, 15000);

        final int total = 8;
        final AtomicInteger pend = new AtomicInteger(total);
        final Runnable um =
                () -> {
                    if (pend.decrementAndGet() == 0) concluir.run();
                };

        coletas(false, ouvinteUnico(um));
        triagens(false, ouvinteUnico(um));
        negociacoes(false, ouvinteUnico(um));
        pedidosCoop(false, ouvinteUnico(um));
        estoque(false, ouvinteUnico(um));
        rateios(false, ouvinteUnico(um));
        rotas(false, ouvinteUnico(um));
        funcionarios(false, ouvinteUnico(um));
    }

    private static <T> Ouvinte<T> ouvinteUnico(Runnable aoPrimeiro) {
        final AtomicBoolean ja = new AtomicBoolean(false);
        return new Ouvinte<T>() {
            @Override
            public void aoReceber(T dados, boolean doCache) {
                if (ja.compareAndSet(false, true)) aoPrimeiro.run();
            }

            @Override
            public void aoErro(String mensagem) {
                if (ja.compareAndSet(false, true)) aoPrimeiro.run();
            }
        };
    }

    /**
     * Depois de gravar algo, marca as listagens afetadas como velhas (a próxima abertura rebusca).
     */
    public static void invalidar(String... chaves) {
        GestorCache.invalidar(chaves);
    }

    public static void limparTudo() {
        GestorCache.limpar();
    }

    public static LocalDate hoje() {
        return LocalDate.now();
    }
}
