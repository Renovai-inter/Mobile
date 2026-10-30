package com.example.renovai.controller;

import com.example.renovai.RotaViagemApiService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Andamento oficial da viagem de uma rota, na API-Redis. A tela de detalhe e o mapa do motorista
 * leem o estado daqui, então não existe um progresso só da tela.
 *
 * <p>As paradas são as coletas da rota (sem partida e retorno), na ordem da Spring API; o índice
 * 0..n é o da API-Redis. Marcar ou desmarcar uma coleta sem conexão não se perde: a alteração fica
 * guardada em {@link Pendencias} e é reenviada depois. O PUT da parada é idempotente (grava o
 * status final), então reenviar não registra a mesma coleta duas vezes.
 */
public class ViagemRota {

    /** Uma coleta da rota, como vai para a API-Redis. */
    public static final class Coleta {
        public final String endereco, enderecoId;
        public final Integer ordem;

        public Coleta(String endereco, String enderecoId, Integer ordem) {
            this.endereco = endereco;
            this.enderecoId = enderecoId;
            this.ordem = ordem;
        }
    }

    /** Status de coleta ainda não aceitos pela API (guardados no aparelho). */
    public interface Pendencias {
        Map<Integer, String> ler(String rotaId);

        void guardar(String rotaId, int indice, String status);

        void remover(String rotaId, int indice);
    }

    public interface Ouvinte {
        /** O estado mudou: a tela redesenha. */
        void mudou();

        /** Mensagem curta para o motorista (toast). */
        void aviso(String mensagem);

        /** A API aceitou a finalização. */
        default void finalizou() {}
    }

    private final RotaViagemApiService api;
    private final String rotaId, motoristaId;
    private final Pendencias pendencias;
    private final Ouvinte ouvinte;

    private List<Coleta> coletas = new ArrayList<>();
    private String status, inicio, motoristaDaViagem;
    private boolean carregou, semConexao, ocupado, reenviouParadas, enviandoPendencias;
    private final Map<Integer, RotaViagemApiService.Parada> paradas = new HashMap<>();
    private boolean encerrada;

    public ViagemRota(RotaViagemApiService api, String rotaId, String motoristaId,
                      Pendencias pendencias, Ouvinte ouvinte) {
        this.api = api;
        this.rotaId = rotaId;
        this.motoristaId = motoristaId;
        this.pendencias = pendencias;
        this.ouvinte = ouvinte;
    }

    /** Para de avisar a tela (onDestroy): respostas que chegarem depois são ignoradas. */
    public void encerrar() {
        encerrada = true;
    }

    public void setColetas(List<Coleta> coletas) {
        this.coletas = coletas == null ? new ArrayList<>() : new ArrayList<>(coletas);
    }

    // ───────────────────────────── estado ─────────────────────────────

    public boolean carregou() {
        return carregou;
    }

    /** A última consulta à API-Redis falhou (sem rede ou servidor fora). */
    public boolean semConexao() {
        return semConexao;
    }

    public boolean ocupado() {
        return ocupado;
    }

    public String status() {
        return status;
    }

    public String inicio() {
        return inicio;
    }

    public boolean emAndamento() {
        return RotaViagemApiService.INICIADA.equals(status);
    }

    /** Finalizada na API (aguardando ou já gravada no Postgres). */
    public boolean finalizada() {
        return RotaViagemApiService.FINALIZADA.equals(status)
                || RotaViagemApiService.SINCRONIZADA.equals(status);
    }

    public boolean outroMotorista() {
        return motoristaDaViagem != null
                && motoristaId != null
                && !motoristaId.equalsIgnoreCase(motoristaDaViagem);
    }

    /** Este motorista pode marcar coletas e finalizar. */
    public boolean podeMexer() {
        return emAndamento() && !outroMotorista() && !ocupado;
    }

    public int totalColetas() {
        return coletas.size();
    }

    /** Status da coleta: o pendente de envio vale sobre o da API (é o que o motorista marcou). */
    public String statusColeta(int indice) {
        String local = pendencias.ler(rotaId).get(indice);
        if (local != null) return local;
        RotaViagemApiService.Parada p = paradas.get(indice);
        return p == null || p.status == null ? RotaViagemApiService.PENDENTE : p.status;
    }

    public boolean concluida(int indice) {
        return RotaViagemApiService.CONCLUIDA.equals(statusColeta(indice));
    }

    public boolean aguardandoEnvio(int indice) {
        return pendencias.ler(rotaId).containsKey(indice);
    }

    public int pendentesDeEnvio() {
        return pendencias.ler(rotaId).size();
    }

    /** Hora (ISO) em que a coleta foi concluída, segundo a API. */
    public String conclusao(int indice) {
        RotaViagemApiService.Parada p = paradas.get(indice);
        return p == null ? null : p.conclusao;
    }

    public int concluidas() {
        int n = 0;
        for (int i = 0; i < coletas.size(); i++) if (concluida(i)) n++;
        return n;
    }

    /** Primeira coleta não concluída, na ordem da rota; -1 se todas estão feitas. */
    public int proxima() {
        for (int i = 0; i < coletas.size(); i++) if (!concluida(i)) return i;
        return -1;
    }

    // ───────────────────────────── API-Redis ─────────────────────────────

    /** Busca a sessão e as paradas; depois tenta mandar o que ficou pendente. */
    public void carregar() {
        api.sessao(rotaId).enqueue(new Callback<RotaViagemApiService.Sessao>() {
            @Override
            public void onResponse(Call<RotaViagemApiService.Sessao> c,
                                   Response<RotaViagemApiService.Sessao> r) {
                if (encerrada) return;
                semConexao = false;
                if (r.code() == 404 || (r.isSuccessful() && r.body() == null)) {
                    // não iniciada, ou finalizada e já apagada do Redis
                    status = null;
                    inicio = null;
                    motoristaDaViagem = null;
                    paradas.clear();
                    descartarPendencias();
                    carregou = true;
                    ouvinte.mudou();
                    return;
                }
                if (!r.isSuccessful()) {
                    semConexao = true;
                    carregou = true;
                    ouvinte.mudou();
                    return;
                }
                RotaViagemApiService.Sessao s = r.body();
                status = s.status;
                inicio = s.inicio;
                motoristaDaViagem = s.motoristaId;
                carregou = true;
                if (emAndamento()) carregarParadas();
                else if (finalizada()) {
                    // mostra o status já; as coletas feitas chegam em seguida (até a sincronização)
                    descartarPendencias();
                    ouvinte.mudou();
                    carregarParadas();
                } else {
                    ouvinte.mudou();
                }
            }

            @Override
            public void onFailure(Call<RotaViagemApiService.Sessao> c, Throwable t) {
                if (encerrada) return;
                semConexao = true;
                carregou = true;
                ouvinte.mudou();
            }
        });
    }

    private void carregarParadas() {
        api.paradas(rotaId).enqueue(new Callback<RotaViagemApiService.Paradas>() {
            @Override
            public void onResponse(Call<RotaViagemApiService.Paradas> c,
                                   Response<RotaViagemApiService.Paradas> r) {
                if (encerrada) return;
                if (!r.isSuccessful() || r.body() == null) {
                    semConexao = emAndamento(); // finalizada: só fica sem a contagem
                    ouvinte.mudou();
                    return;
                }
                List<RotaViagemApiService.Parada> lista = r.body().paradas;
                if ((lista == null || lista.isEmpty()) && emAndamento() && !outroMotorista()
                        && !reenviouParadas && !coletas.isEmpty()) {
                    // o envio das paradas falhou ao iniciar: manda de novo (1 vez)
                    reenviouParadas = true;
                    enviarParadas(null);
                    return;
                }
                paradas.clear();
                if (lista != null) {
                    for (RotaViagemApiService.Parada p : lista) {
                        try {
                            // o campo id, e não a posição na lista (ordem de texto acima de 10)
                            paradas.put(Integer.parseInt(p.id), p);
                        } catch (NumberFormatException | NullPointerException ignorado) {
                            // parada fora do formato da API: fica pendente
                        }
                    }
                }
                ouvinte.mudou();
                if (emAndamento()) enviarPendencias();
            }

            @Override
            public void onFailure(Call<RotaViagemApiService.Paradas> c, Throwable t) {
                if (encerrada) return;
                semConexao = emAndamento();
                ouvinte.mudou();
            }
        });
    }

    public void iniciar() {
        if (ocupado) return;
        if (motoristaId == null) {
            ouvinte.aviso("Não foi possível identificar o motorista. Faça login novamente.");
            return;
        }
        ocupar(true);
        api.iniciar(new RotaViagemApiService.Inicio(rotaId, motoristaId))
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> c,
                                           Response<Map<String, Object>> r) {
                        if (encerrada) return;
                        if (r.code() == 409) {
                            ocupar(false);
                            ouvinte.aviso("Esta rota já está em andamento com outro motorista"
                                    + " ou acabou de ser finalizada.");
                            carregar();
                            return;
                        }
                        if (!r.isSuccessful()) {
                            ocupar(false);
                            ouvinte.aviso("Não foi possível iniciar a rota (" + r.code() + ").");
                            return;
                        }
                        enviarParadas("Rota iniciada. Boa viagem!");
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> c, Throwable t) {
                        if (encerrada) return;
                        ocupar(false);
                        ouvinte.aviso("Sem conexão: a rota não foi iniciada.");
                    }
                });
    }

    /** Registra as coletas na API-Redis (a API ignora o reenvio) e recarrega o andamento. */
    private void enviarParadas(String aoTerminar) {
        List<RotaViagemApiService.Parada> lista = new ArrayList<>();
        for (Coleta k : coletas)
            lista.add(new RotaViagemApiService.Parada(k.endereco, k.enderecoId, k.ordem));
        if (lista.isEmpty()) {
            ocupar(false);
            if (aoTerminar != null) ouvinte.aviso(aoTerminar);
            carregar();
            return;
        }
        ocupar(true);
        api.adicionarParadas(rotaId, new RotaViagemApiService.Paradas(lista))
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> c,
                                           Response<Map<String, Object>> r) {
                        if (encerrada) return;
                        ocupar(false);
                        if (r.isSuccessful() && aoTerminar != null) ouvinte.aviso(aoTerminar);
                        carregar();
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> c, Throwable t) {
                        if (encerrada) return;
                        ocupar(false);
                        carregar();
                    }
                });
    }

    /** Toque numa coleta: marca como concluída ou volta para pendente. */
    public void alternar(int indice) {
        if (!podeMexer() || indice < 0 || indice >= coletas.size()) return;
        definir(indice, concluida(indice)
                ? RotaViagemApiService.PENDENTE
                : RotaViagemApiService.CONCLUIDA);
    }

    public void definir(int indice, String novo) {
        if (!podeMexer() || indice < 0 || indice >= coletas.size()) return;
        // guarda antes de chamar a API: se a rede cair no meio, nada se perde
        pendencias.guardar(rotaId, indice, novo);
        ouvinte.mudou();
        enviar(indice, novo, true);
    }

    private void enviar(int indice, String novo, boolean avisarFalha) {
        api.atualizarParada(rotaId, indice, new RotaViagemApiService.NovoStatus(novo, motoristaId))
                .enqueue(new Callback<RotaViagemApiService.Parada>() {
                    @Override
                    public void onResponse(Call<RotaViagemApiService.Parada> c,
                                           Response<RotaViagemApiService.Parada> r) {
                        if (encerrada) return;
                        if (r.isSuccessful()) {
                            semConexao = false;
                            // só limpa se o motorista não mudou de ideia enquanto isso
                            if (novo.equals(pendencias.ler(rotaId).get(indice)))
                                pendencias.remover(rotaId, indice);
                            if (r.body() != null) paradas.put(indice, r.body());
                            ouvinte.mudou();
                            return;
                        }
                        if (r.code() >= 500) {
                            // servidor fora: tenta de novo depois
                            semConexao = true;
                            if (avisarFalha) ouvinte.aviso("Servidor indisponível: a coleta será"
                                    + " enviada quando ele voltar.");
                            ouvinte.mudou();
                            return;
                        }
                        // 403/404/409: a viagem mudou (finalizada, de outro motorista) — vale a API
                        pendencias.remover(rotaId, indice);
                        ouvinte.aviso("A coleta não foi aceita (" + r.code()
                                + "): o andamento foi atualizado.");
                        carregar();
                    }

                    @Override
                    public void onFailure(Call<RotaViagemApiService.Parada> c, Throwable t) {
                        if (encerrada) return;
                        semConexao = true;
                        if (avisarFalha) ouvinte.aviso("Sem conexão: a coleta foi guardada e será"
                                + " enviada quando a internet voltar.");
                        ouvinte.mudou();
                    }
                });
    }

    /** Reenvia o que ficou guardado (ao voltar a conexão ou ao abrir a tela). */
    public void enviarPendencias() {
        if (!emAndamento() || outroMotorista() || enviandoPendencias) return;
        Map<Integer, String> lista = new HashMap<>(pendencias.ler(rotaId));
        if (lista.isEmpty()) return;
        enviandoPendencias = true;
        List<Integer> indices = new ArrayList<>(lista.keySet());
        Collections.sort(indices);
        for (int i : indices) enviar(i, lista.get(i), false);
        enviandoPendencias = false;
    }

    private void descartarPendencias() {
        for (int i : new ArrayList<>(pendencias.ler(rotaId).keySet())) pendencias.remover(rotaId, i);
    }

    /** Pendentes que bloqueiam a finalização (precisam chegar antes na API). */
    public boolean podeFinalizar() {
        return podeMexer() && pendentesDeEnvio() == 0;
    }

    public void finalizar() {
        if (!podeMexer()) return;
        if (pendentesDeEnvio() > 0) {
            enviarPendencias();
            ouvinte.aviso("Ainda há coletas aguardando envio. Aguarde a conexão para finalizar.");
            return;
        }
        ocupar(true);
        api.finalizar(rotaId, new RotaViagemApiService.Motorista(motoristaId))
                .enqueue(new Callback<RotaViagemApiService.Sessao>() {
                    @Override
                    public void onResponse(Call<RotaViagemApiService.Sessao> c,
                                           Response<RotaViagemApiService.Sessao> r) {
                        if (encerrada) return;
                        ocupar(false);
                        if (!r.isSuccessful()) {
                            ouvinte.aviso("Não foi possível finalizar a rota (" + r.code() + ").");
                            carregar();
                            return;
                        }
                        status = RotaViagemApiService.FINALIZADA;
                        ouvinte.finalizou();
                        ouvinte.mudou();
                    }

                    @Override
                    public void onFailure(Call<RotaViagemApiService.Sessao> c, Throwable t) {
                        if (encerrada) return;
                        ocupar(false);
                        ouvinte.aviso("Sem conexão: a rota não foi finalizada.");
                    }
                });
    }

    private void ocupar(boolean sim) {
        ocupado = sim;
        ouvinte.mudou();
    }
}
