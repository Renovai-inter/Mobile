package com.example.renovai;

import com.example.renovai.dto.response.EmpresaResponses;
import com.example.renovai.dto.response.GestorResponses;
import com.example.renovai.dto.response.PedidoResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Camada de dados da área da Empresa — mesmo padrão cache-primeiro-depois-atualiza do Gestor. */
public final class EmpresaData {

    private EmpresaData() {}

    // Tipos das listagens da Empresa — permitem que o GestorCache salve e restaure do Firestore
    // (cache offline). Ver GestorCache.registrarTipo.
    static {
        GestorCache.registrarTipo("empresa:meuPerfil", EmpresaResponses.MeuPerfil.class);
        GestorCache.registrarTipo("empresa:dashboard", EmpresaResponses.Dashboard.class);
        GestorCache.registrarTipo("empresa:pedidos",
                new com.google.gson.reflect.TypeToken<List<PedidoResponse>>() {}.getType());
        GestorCache.registrarTipo("empresa:negociacoes",
                new com.google.gson.reflect.TypeToken<List<GestorResponses.Negociacao>>() {}.getType());
        GestorCache.registrarTipo("empresa:favoritos",
                new com.google.gson.reflect.TypeToken<List<EmpresaResponses.Favorito>>() {}.getType());
        GestorCache.registrarTipo("empresa:categorias",
                new com.google.gson.reflect.TypeToken<
                        List<com.example.renovai.dto.response.CategoriaMaterialResponse>>() {}.getType());
        GestorCache.registrarTipo("empresa:interesses",
                new com.google.gson.reflect.TypeToken<List<EmpresaResponses.MaterialInteresse>>() {}.getType());
        GestorCache.registrarTipo("empresa:perfilId", String.class);
        GestorCache.registrarTipo(GestorCache.STATUS,
                new com.google.gson.reflect.TypeToken<List<GestorResponses.Status>>() {}.getType());
    }

    public static String erro(retrofit2.Response<?> r) {
        try {
            if (r.errorBody() != null) {
                com.google.gson.JsonObject obj =
                        com.google.gson.JsonParser.parseString(r.errorBody().string())
                                .getAsJsonObject();
                for (String k : new String[] {"mensagem", "message"})
                    if (obj.has(k) && obj.get(k).isJsonPrimitive()) return obj.get(k).getAsString();
            }
        } catch (Exception ignored) {
        }
        return "Não foi possível concluir a operação (erro " + r.code() + ").";
    }

    public static EmpresaApiService api() {
        return ApiClient.createService(EmpresaApiService.class);
    }

    public static String empresaId() {
        return SessionManager.getEmpresaId();
    }

    private static <T> void buscar(
            String chave,
            boolean forcar,
            java.util.function.Supplier<Call<T>> fabrica,
            GestorData.Ouvinte<T> o) {
        // Primeiro traz para a memória a cópia salva no Firestore (cache offline), depois segue.
        GestorCache.aposRestaurar(() -> buscarAgora(chave, forcar, fabrica, o));
    }

    private static <T> void buscarAgora(
            String chave,
            boolean forcar,
            java.util.function.Supplier<Call<T>> fabrica,
            GestorData.Ouvinte<T> o) {
        final String token = SessionManager.getToken();
        final T cache = GestorCache.obter(chave);
        if (cache != null) o.aoReceber(cache, true);
        if (cache != null && !forcar && GestorCache.valido(chave)) return;

        fabrica.get()
                .enqueue(
                        new Callback<T>() {
                            @Override
                            public void onResponse(Call<T> call, Response<T> r) {
                                if (!java.util.Objects.equals(token, SessionManager.getToken()))
                                    return;
                                if (r.isSuccessful() && r.body() != null) {
                                    GestorCache.guardar(chave, r.body());
                                    o.aoReceber(r.body(), false);
                                } else if (cache == null || forcar) {
                                    o.aoErro(erro(r));
                                }
                            }

                            @Override
                            public void onFailure(Call<T> call, Throwable t) {
                                if (!java.util.Objects.equals(token, SessionManager.getToken()))
                                    return;
                                if (cache == null || forcar)
                                    o.aoErro("Sem conexão com o servidor.");
                            }
                        });
    }

    public static void meuPerfil(boolean forcar, GestorData.Ouvinte<EmpresaResponses.MeuPerfil> o) {
        buscar("empresa:meuPerfil", forcar, () -> api().meuPerfil(), o);
    }

    public static void dashboard(boolean forcar, GestorData.Ouvinte<EmpresaResponses.Dashboard> o) {
        if (empresaId() == null) {
            o.aoErro("Não foi possível identificar a empresa. Faça login novamente.");
            return;
        }
        buscar("empresa:dashboard", forcar, () -> api().dashboard(), o);
    }

    public static void pedidos(boolean forcar, GestorData.Ouvinte<List<PedidoResponse>> o) {
        if (empresaId() == null) {
            o.aoErro("Não foi possível identificar a empresa. Faça login novamente.");
            return;
        }
        buscar("empresa:pedidos", forcar, () -> api().pedidosPorEmpresa(), o);
    }

    public static void negociacoes(
            boolean forcar, GestorData.Ouvinte<List<GestorResponses.Negociacao>> o) {
        if (empresaId() == null) {
            o.aoErro("Não foi possível identificar a empresa. Faça login novamente.");
            return;
        }
        buscar("empresa:negociacoes", forcar, () -> api().negociacoesPorEmpresa(), o);
    }

    public static void favoritos(
            boolean forcar, GestorData.Ouvinte<List<EmpresaResponses.Favorito>> o) {
        if (empresaId() == null) {
            o.aoErro("Não foi possível identificar a empresa. Faça login novamente.");
            return;
        }
        buscar("empresa:favoritos", forcar, () -> api().favoritos(), o);
    }

    public static void status(GestorData.Ouvinte<List<GestorResponses.Status>> o) {
        buscar(GestorCache.STATUS, false, () -> api().status(), o);
    }

    public static void categorias(
            GestorData.Ouvinte<List<com.example.renovai.dto.response.CategoriaMaterialResponse>>
                    o) {
        buscar("empresa:categorias", false, () -> api().categorias(), o);
    }

    public static void materiaisInteresse(
            boolean forcar, GestorData.Ouvinte<List<EmpresaResponses.MaterialInteresse>> o) {
        if (empresaId() == null) {
            o.aoErro("Não foi possível identificar a empresa.");
            return;
        }
        buscar("empresa:interesses", forcar, () -> api().materiaisInteresse(), o);
    }

    /**
     * perfilId da própria empresa logada — necessário para enviar mensagens e avaliações como
     * remetente/avaliador.
     */
    public static void meuPerfilId(GestorData.Ouvinte<String> o) {
        GestorCache.aposRestaurar(() -> meuPerfilIdAgora(o));
    }

    private static void meuPerfilIdAgora(GestorData.Ouvinte<String> o) {
        String cache = GestorCache.obter("empresa:perfilId");
        if (cache != null) {
            o.aoReceber(cache, true);
            return;
        }
        meuPerfil(
                false,
                new GestorData.Ouvinte<EmpresaResponses.MeuPerfil>() {
                    @Override
                    public void aoReceber(EmpresaResponses.MeuPerfil p, boolean doCache) {
                        if (p.perfilId != null) {
                            GestorCache.guardar("empresa:perfilId", p.perfilId);
                            o.aoReceber(p.perfilId, doCache);
                        } else if (!doCache)
                            o.aoErro("Não foi possível identificar o perfil da empresa.");
                    }

                    @Override
                    public void aoErro(String m) {
                        o.aoErro(m);
                    }
                });
    }

    public static void invalidar(String... chaves) {
        GestorCache.invalidar(chaves);
    }

    public static void limparTudo() {
        for (String k :
                new String[] {
                    "empresa:meuPerfil",
                    "empresa:dashboard",
                    "empresa:pedidos",
                    "empresa:negociacoes",
                    "empresa:favoritos",
                    "empresa:categorias",
                    "empresa:interesses",
                    "empresa:perfilId"
                }) {
            GestorCache.invalidar(k);
        }
    }

    /** Pré-carrega as listagens principais no login (mesma ideia do GestorData.precarregar). */
    public static void precarregar(Runnable aoTerminar) {
        if (empresaId() == null) {
            aoTerminar.run();
            return;
        }
        final AtomicBoolean feito = new AtomicBoolean(false);
        final Runnable concluir =
                () -> {
                    if (feito.compareAndSet(false, true)) aoTerminar.run();
                };
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(concluir, 15000);

        final AtomicInteger pend = new AtomicInteger(4);
        final Runnable um =
                () -> {
                    if (pend.decrementAndGet() == 0) concluir.run();
                };
        meuPerfil(false, ouvinteUnico(um));
        dashboard(false, ouvinteUnico(um));
        pedidos(false, ouvinteUnico(um));
        favoritos(false, ouvinteUnico(um));
    }

    private static <T> GestorData.Ouvinte<T> ouvinteUnico(Runnable aoPrimeiro) {
        final AtomicBoolean ja = new AtomicBoolean(false);
        return new GestorData.Ouvinte<T>() {
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
}
