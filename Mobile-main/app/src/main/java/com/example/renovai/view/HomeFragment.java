package com.example.renovai.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.renovai.ApiClient;
import com.example.renovai.CooperativaApiService;
import com.example.renovai.FavoritoApiService;
import com.example.renovai.PedidoApiService;
import com.example.renovai.PerfilResolver;
import com.example.renovai.R;
import com.example.renovai.SessionManager;
import com.example.renovai.adapter.CooperativaCardAdapter;
import com.example.renovai.adapter.PedidoAdapter;
import com.example.renovai.dto.response.CooperativaPerfilPublicoResponse;
import com.example.renovai.dto.response.CooperativaResponse;
import com.example.renovai.dto.response.FavoritoResponse;
import com.example.renovai.dto.response.ItemResponse;
import com.example.renovai.dto.response.PedidoCooperativaResponse;
import com.example.renovai.dto.response.PedidoResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Aba "Home" do menu da Empresa. Todos os dados vêm de endpoints que já existiam
 * na API — nada foi criado nem alterado no backend.
 *
 * Como a API não devolve o empresaId no login nem o nome da cooperativa dentro de
 * cada pedido, essas duas informações são resolvidas aqui mesmo, no app:
 *  - empresaId: GET /perfis, cruzando pelo email da sessão (ver PerfilResolver).
 *  - cooperativa de cada pedido: GET /cooperativas + GET /pedidos/por-cooperativa/{id}
 *    para cada uma, montando um mapa pedidoId -> nome da cooperativa.
 */
public class HomeFragment extends Fragment {

    private static final int MAX_PEDIDOS_RECENTES = 5;
    private static final int MAX_COOPERATIVAS_RECENTES = 3;

    private TextView txtTotalPedidos, txtEmNegociacao, txtFinalizados;
    private TextView txtSemPedidos, txtSemCooperativas;
    private RecyclerView recyclerPedidos, recyclerCooperativas;
    private PedidoAdapter pedidoAdapter;
    private CooperativaCardAdapter cooperativaAdapter;

    // Estado da junção pedidos + mapa de cooperativas (as duas chamadas rodam em
    // paralelo; só processamos quando as duas tiverem voltado).
    private List<PedidoResponse> pedidosCarregados;
    private Map<String, String> mapaCooperativaPorPedido;
    private boolean pedidosProntos = false;
    private boolean mapaProntos = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtTotalPedidos = view.findViewById(R.id.txtTotalPedidos);
        txtEmNegociacao = view.findViewById(R.id.txtEmNegociacao);
        txtFinalizados = view.findViewById(R.id.txtFinalizados);
        txtSemPedidos = view.findViewById(R.id.txtSemPedidos);
        txtSemCooperativas = view.findViewById(R.id.txtSemCooperativas);

        recyclerPedidos = view.findViewById(R.id.recyclerPedidos);
        recyclerPedidos.setLayoutManager(new LinearLayoutManager(requireContext()));
        pedidoAdapter = new PedidoAdapter();
        recyclerPedidos.setAdapter(pedidoAdapter);

        recyclerCooperativas = view.findViewById(R.id.recyclerCooperativas);
        cooperativaAdapter = new CooperativaCardAdapter();
        recyclerCooperativas.setAdapter(cooperativaAdapter);

        String empresaId = SessionManager.getEmpresaId();
        if (empresaId != null && !empresaId.trim().isEmpty()) {
            iniciarCarregamento(empresaId);
            return;
        }

        // Fallback: sessão antiga, ou empresaId ainda não resolvido no login.
        // Tenta resolver de novo agora, pelo email salvo.
        PerfilResolver.resolverEmpresaId(SessionManager.getEmail(), resolvido -> {
            if (!isAdded()) return;
            if (resolvido == null) {
                Toast.makeText(requireContext(),
                        "Não foi possível identificar a empresa desta conta.", Toast.LENGTH_LONG).show();
                return;
            }
            SessionManager.salvarEmpresaId(resolvido);
            iniciarCarregamento(resolvido);
        });
    }

    private void iniciarCarregamento(String empresaId) {
        carregarPedidos(empresaId);
        carregarMapaCooperativasPorPedido();
        carregarCooperativasRecentes(empresaId);
    }

    // ================= PEDIDOS =================

    private void carregarPedidos(String empresaId) {
        PedidoApiService service = ApiClient.createService(PedidoApiService.class);
        service.listarPorEmpresa(empresaId).enqueue(new Callback<List<PedidoResponse>>() {
            @Override
            public void onResponse(Call<List<PedidoResponse>> call, Response<List<PedidoResponse>> response) {
                if (!isAdded()) return;
                pedidosCarregados = response.isSuccessful() && response.body() != null
                        ? response.body() : new ArrayList<>();
                pedidosProntos = true;
                tentarProcessarPedidos();
            }

            @Override
            public void onFailure(Call<List<PedidoResponse>> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Falha de conexão ao carregar pedidos.", Toast.LENGTH_SHORT).show();
                pedidosCarregados = new ArrayList<>();
                pedidosProntos = true;
                tentarProcessarPedidos();
            }
        });
    }

    /**
     * Monta pedidoId -> nome da cooperativa cruzando GET /cooperativas (lista todas)
     * com GET /pedidos/por-cooperativa/{id} de cada uma. Custa N+1 chamadas, mas não
     * exige nada novo na API.
     */
    private void carregarMapaCooperativasPorPedido() {
        mapaCooperativaPorPedido = new HashMap<>();

        CooperativaApiService coopService = ApiClient.createService(CooperativaApiService.class);
        coopService.listar().enqueue(new Callback<List<CooperativaResponse>>() {
            @Override
            public void onResponse(Call<List<CooperativaResponse>> call, Response<List<CooperativaResponse>> response) {
                if (!isAdded()) return;
                List<CooperativaResponse> cooperativas =
                        response.isSuccessful() && response.body() != null ? response.body() : new ArrayList<>();

                if (cooperativas.isEmpty()) {
                    mapaProntos = true;
                    tentarProcessarPedidos();
                    return;
                }

                PedidoApiService pedidoService = ApiClient.createService(PedidoApiService.class);
                AtomicInteger pendentes = new AtomicInteger(cooperativas.size());

                for (CooperativaResponse coop : cooperativas) {
                    pedidoService.listarPorCooperativa(coop.getCooperativaId())
                            .enqueue(new Callback<List<PedidoCooperativaResponse>>() {
                                @Override
                                public void onResponse(Call<List<PedidoCooperativaResponse>> call,
                                                       Response<List<PedidoCooperativaResponse>> response) {
                                    if (response.isSuccessful() && response.body() != null) {
                                        for (PedidoCooperativaResponse vinculo : response.body()) {
                                            mapaCooperativaPorPedido.put(vinculo.getPedidoId(), vinculo.getCooperativaNome());
                                        }
                                    }
                                    finalizarSeCompleto();
                                }

                                @Override
                                public void onFailure(Call<List<PedidoCooperativaResponse>> call, Throwable t) {
                                    finalizarSeCompleto();
                                }

                                private void finalizarSeCompleto() {
                                    if (pendentes.decrementAndGet() == 0 && isAdded()) {
                                        mapaProntos = true;
                                        tentarProcessarPedidos();
                                    }
                                }
                            });
                }
            }

            @Override
            public void onFailure(Call<List<CooperativaResponse>> call, Throwable t) {
                if (!isAdded()) return;
                mapaProntos = true; // segue sem os nomes de cooperativa, não trava a tela
                tentarProcessarPedidos();
            }
        });
    }

    private void tentarProcessarPedidos() {
        if (!pedidosProntos || !mapaProntos) return; // espera as duas chamadas
        processarPedidos(pedidosCarregados);
    }

    private void processarPedidos(List<PedidoResponse> pedidos) {
        int total = pedidos.size();
        int finalizados = 0;

        for (PedidoResponse p : pedidos) {
            PedidoAdapter.StatusPedido status =
                    PedidoAdapter.classificarStatus(p.getStatusAtual(), p.getDataConclusao() != null);
            if (status == PedidoAdapter.StatusPedido.CONCLUIDO || status == PedidoAdapter.StatusPedido.CANCELADO) {
                finalizados++;
            }
        }
        int emNegociacao = total - finalizados;

        txtTotalPedidos.setText(String.valueOf(total));
        txtEmNegociacao.setText(emNegociacao + " Em negociação");
        txtFinalizados.setText(finalizados + " Finalizados");

        List<PedidoResponse> ordenados = new ArrayList<>(pedidos);
        ordenados.sort((a, b) -> {
            String da = a.getDataPedido(), db = b.getDataPedido();
            if (da == null || db == null) return 0;
            return db.compareTo(da);
        });

        if (ordenados.isEmpty()) {
            txtSemPedidos.setVisibility(View.VISIBLE);
            recyclerPedidos.setVisibility(View.GONE);
            return;
        }
        txtSemPedidos.setVisibility(View.GONE);
        recyclerPedidos.setVisibility(View.VISIBLE);

        List<PedidoResponse> recentes = ordenados.subList(0, Math.min(MAX_PEDIDOS_RECENTES, ordenados.size()));
        carregarItensDosPedidos(recentes);
    }

    private void carregarItensDosPedidos(List<PedidoResponse> recentes) {
        PedidoApiService service = ApiClient.createService(PedidoApiService.class);
        PedidoAdapter.Item[] slots = new PedidoAdapter.Item[recentes.size()];
        AtomicInteger pendentes = new AtomicInteger(recentes.size());

        for (int i = 0; i < recentes.size(); i++) {
            final int idx = i;
            PedidoResponse pedido = recentes.get(i);
            PedidoAdapter.StatusPedido status =
                    PedidoAdapter.classificarStatus(pedido.getStatusAtual(), pedido.getDataConclusao() != null);

            service.listarItens(pedido.getPedidoId()).enqueue(new Callback<List<ItemResponse>>() {
                @Override
                public void onResponse(Call<List<ItemResponse>> call, Response<List<ItemResponse>> response) {
                    slots[idx] = montarItemUi(pedido, status, response.isSuccessful() ? response.body() : null);
                    finalizarSeCompleto();
                }

                @Override
                public void onFailure(Call<List<ItemResponse>> call, Throwable t) {
                    slots[idx] = montarItemUi(pedido, status, null);
                    finalizarSeCompleto();
                }

                private void finalizarSeCompleto() {
                    if (pendentes.decrementAndGet() == 0 && isAdded()) {
                        pedidoAdapter.submitList(Arrays.asList(slots));
                    }
                }
            });
        }
    }

    private PedidoAdapter.Item montarItemUi(PedidoResponse pedido, PedidoAdapter.StatusPedido status,
                                            @Nullable List<ItemResponse> itens) {
        String materialTitulo = "";
        double totalKg = 0;
        if (itens != null && !itens.isEmpty()) {
            String categoria = itens.get(0).getMaterialCategoria();
            materialTitulo = categoria != null ? categoria : "";
            for (ItemResponse it : itens) {
                if (it.getQuantidadeKg() != null) totalKg += it.getQuantidadeKg();
            }
        }
        String cooperativa = mapaCooperativaPorPedido != null
                ? mapaCooperativaPorPedido.get(pedido.getPedidoId()) : null;
        if (cooperativa == null) cooperativa = "Cooperativa";
        String subtitulo = cooperativa + " - " + formatarKg(totalKg) + "kg";
        return new PedidoAdapter.Item(pedido, materialTitulo, subtitulo, status);
    }

    private String formatarKg(double kg) {
        if (kg == Math.floor(kg)) return String.valueOf((long) kg);
        return String.format(Locale.getDefault(), "%.1f", kg);
    }

    // ================= COOPERATIVAS RECENTES =================

    private void carregarCooperativasRecentes(String empresaId) {
        FavoritoApiService service = ApiClient.createService(FavoritoApiService.class);
        service.listarPorEmpresa(empresaId).enqueue(new Callback<List<FavoritoResponse>>() {
            @Override
            public void onResponse(Call<List<FavoritoResponse>> call, Response<List<FavoritoResponse>> response) {
                if (!isAdded()) return;
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    txtSemCooperativas.setVisibility(View.VISIBLE);
                    recyclerCooperativas.setVisibility(View.GONE);
                    return;
                }
                txtSemCooperativas.setVisibility(View.GONE);
                recyclerCooperativas.setVisibility(View.VISIBLE);

                List<FavoritoResponse> favoritos = new ArrayList<>(response.body());
                favoritos.sort((a, b) -> {
                    String da = a.getDataCriacao(), db = b.getDataCriacao();
                    if (da == null || db == null) return 0;
                    return db.compareTo(da);
                });
                List<FavoritoResponse> recentes =
                        favoritos.subList(0, Math.min(MAX_COOPERATIVAS_RECENTES, favoritos.size()));
                carregarAvaliacoesCooperativas(recentes);
            }

            @Override
            public void onFailure(Call<List<FavoritoResponse>> call, Throwable t) {
                if (!isAdded()) return;
                txtSemCooperativas.setVisibility(View.VISIBLE);
                recyclerCooperativas.setVisibility(View.GONE);
            }
        });
    }

    private void carregarAvaliacoesCooperativas(List<FavoritoResponse> recentes) {
        FavoritoApiService service = ApiClient.createService(FavoritoApiService.class);
        CooperativaCardAdapter.Item[] slots = new CooperativaCardAdapter.Item[recentes.size()];
        AtomicInteger pendentes = new AtomicInteger(recentes.size());

        for (int i = 0; i < recentes.size(); i++) {
            final int idx = i;
            FavoritoResponse fav = recentes.get(i);

            service.perfilPublico(fav.getCooperativaId()).enqueue(new Callback<CooperativaPerfilPublicoResponse>() {
                @Override
                public void onResponse(Call<CooperativaPerfilPublicoResponse> call,
                                       Response<CooperativaPerfilPublicoResponse> response) {
                    CooperativaPerfilPublicoResponse body = response.isSuccessful() ? response.body() : null;
                    slots[idx] = new CooperativaCardAdapter.Item(
                            fav.getCooperativaNome(),
                            fav.getCooperativaImagem(),
                            body != null ? body.getMediaAvaliacoes() : null,
                            body != null ? body.getTotalAvaliacoes() : null
                    );
                    finalizarSeCompleto();
                }

                @Override
                public void onFailure(Call<CooperativaPerfilPublicoResponse> call, Throwable t) {
                    slots[idx] = new CooperativaCardAdapter.Item(
                            fav.getCooperativaNome(), fav.getCooperativaImagem(), null, null);
                    finalizarSeCompleto();
                }

                private void finalizarSeCompleto() {
                    if (pendentes.decrementAndGet() == 0 && isAdded()) {
                        cooperativaAdapter.submitList(Arrays.asList(slots));
                    }
                }
            });
        }
    }
}
