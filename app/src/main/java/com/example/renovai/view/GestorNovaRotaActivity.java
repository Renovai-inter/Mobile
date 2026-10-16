package com.example.renovai.view;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.renovai.GestorBottomNav;
import com.example.renovai.GestorCache;
import com.example.renovai.GestorData;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.RotaEstimador;
import com.example.renovai.dto.request.GestorRequests;
import com.example.renovai.dto.response.GestorResponses;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nova rota: nome, paradas em ordem (partida → coletas → fim) e resumo estimado; grava rota +
 * endereços + paradas.
 */
public class GestorNovaRotaActivity extends GestorBaseActivity {

    private static class Parada {
        String texto, logradouro, numero, bairro;
    }

    private final List<Parada> paradas = new ArrayList<>();
    private int geracao = 0;
    private boolean salvando;
    private String rotaCriadaId, nomeCriado;
    private int proximaParada;
    private final java.util.Map<Integer, String> enderecosCriados = new java.util.HashMap<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_gestor_nova_rota, GestorBottomNav.Aba.NENHUMA)) return;
        titulo("Nova Rota");
        ((EditText) findViewById(R.id.edtNomeRota))
                .setText("Rota - " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM")));
        findViewById(R.id.btnAdicionarParada).setOnClickListener(v -> adicionar());
        findViewById(R.id.btnCriarRota).setOnClickListener(v -> criar());
        render();
    }

    private void adicionar() {
        if (salvando || rotaCriadaId != null) {
            toast("Finalize o cadastro da rota antes de alterar as paradas.");
            return;
        }
        EditText e = findViewById(R.id.edtNovaParada);
        String t = e.getText().toString().trim();
        if (t.isEmpty()) {
            toast("Digite o endereço da parada.");
            return;
        }
        Parada p = new Parada();
        p.texto = t.length() > 255 ? t.substring(0, 255) : t;
        String resto = t;
        Matcher m = Pattern.compile(",\\s*(\\d+\\w*)\\s*$").matcher(resto);
        if (m.find()) {
            p.numero = m.group(1);
            resto = resto.substring(0, m.start());
        }
        String[] partes = resto.split("\\s+-\\s+");
        p.logradouro = partes[0].trim();
        if (partes.length > 1) p.bairro = partes[1].trim();
        if (p.logradouro.isEmpty()) p.logradouro = p.texto;
        paradas.add(p);
        e.setText("");
        render();
    }

    private void render() {
        LinearLayout raiz = findViewById(R.id.listaParadas);
        raiz.removeAllViews();
        int n = paradas.size();
        for (int i = 0; i < n; i++) {
            final int idx = i;
            boolean ini = i == 0, fim = i == n - 1 && n > 1;
            View v = LayoutInflater.from(this).inflate(R.layout.item_gestor_parada, raiz, false);
            TextView num = v.findViewById(R.id.txtParadaNumero);
            num.setText(String.valueOf(i + 1));
            num.setBackgroundResource(
                    ini
                            ? R.drawable.stop_filled_green
                            : fim
                                    ? R.drawable.avatar_maroon_background
                                    : R.drawable.stop_outline_green);
            num.setTextColor(ini || fim ? Color.WHITE : Color.parseColor("#519059"));
            ((TextView) v.findViewById(R.id.txtParadaTipo))
                    .setText(ini ? "Ponto de Partida" : fim ? "Fim" : "Coleta");
            ((TextView) v.findViewById(R.id.txtParadaEndereco)).setText(paradas.get(i).texto);
            TextView tag = v.findViewById(R.id.txtParadaTag);
            tag.setText(ini ? "Início" : fim ? "Fim" : "Coleta");
            tag.setBackgroundResource(
                    ini
                            ? R.drawable.btn_green_background
                            : fim
                                    ? R.drawable.card_maroon_background
                                    : R.drawable.pill_light_green);
            tag.setTextColor(ini || fim ? Color.WHITE : Color.parseColor("#3F6B3F"));
            v.findViewById(R.id.linhaParada)
                    .setVisibility(i == n - 1 ? View.INVISIBLE : View.VISIBLE);
            v.setOnLongClickListener(
                    x -> {
                        if (salvando || rotaCriadaId != null) return true;
                        GestorUi.confirmar(
                                this,
                                "Remover parada",
                                "Remover \"" + paradas.get(idx).texto + "\" da rota?",
                                "Remover",
                                () -> {
                                    paradas.remove(idx);
                                    render();
                                });
                        return true;
                    });
            raiz.addView(v);
        }
        findViewById(R.id.txtSemParadas).setVisibility(n == 0 ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.txtResParadas)).setText(String.valueOf(n));
        TextView dist = findViewById(R.id.txtResDistancia), tempo = findViewById(R.id.txtResTempo);
        dist.setText("—");
        tempo.setText("—");
        final int g = ++geracao;
        if (n >= 2) {
            dist.setText("calculando...");
            List<String> qs = new ArrayList<>();
            for (Parada p : paradas) qs.add(p.texto);
            RotaEstimador.estimar(
                    this,
                    qs,
                    new RotaEstimador.Callback() {
                        @Override
                        public void resultado(double km, int min) {
                            if (g == geracao && vivo()) {
                                dist.setText(RotaEstimador.textoKm(km) + " (est.)");
                                tempo.setText(RotaEstimador.textoTempo(min) + " (est.)");
                            }
                        }

                        @Override
                        public void indisponivel() {
                            if (g == geracao && vivo()) dist.setText("—");
                        }
                    });
        }
    }

    private void criar() {
        if (salvando) return;
        String nome = ((EditText) findViewById(R.id.edtNomeRota)).getText().toString().trim();
        if (nome.isEmpty()) {
            toast("Dê um nome para a rota.");
            return;
        }
        if (paradas.size() < 3) {
            toast("Adicione a partida, uma coleta e o ponto de retorno.");
            return;
        }
        salvando = true;
        View prog = findViewById(R.id.progresso);
        prog.setVisibility(View.VISIBLE);
        findViewById(R.id.btnCriarRota).setEnabled(false);
        if (rotaCriadaId != null) {
            gravarParada(prog, rotaCriadaId, proximaParada);
            return;
        }
        nomeCriado = nome;
        GestorData.api()
                .criarRota(new GestorRequests.Rota(coopId(), nome, true))
                .enqueue(
                        new Callback<GestorResponses.Rota>() {
                            @Override
                            public void onResponse(
                                    Call<GestorResponses.Rota> c,
                                    Response<GestorResponses.Rota> r) {
                                if (!r.isSuccessful() || r.body() == null) {
                                    erro(
                                            prog,
                                            "Não foi possível criar a rota (erro "
                                                    + r.code()
                                                    + ").");
                                    return;
                                }
                                rotaCriadaId = r.body().rotaId;
                                if (vivo())
                                    ((EditText) findViewById(R.id.edtNomeRota)).setEnabled(false);
                                gravarParada(prog, rotaCriadaId, 0);
                            }

                            @Override
                            public void onFailure(Call<GestorResponses.Rota> c, Throwable t) {
                                erro(prog, "Sem conexão com o servidor.");
                            }
                        });
    }

    private void gravarParada(View prog, String rotaId, int i) {
        if (i >= paradas.size()) {
            GestorRotaLocal.marcarCriada(this, rotaId);
            GestorData.invalidar(GestorCache.ROTAS);
            if (!vivo()) return;
            prog.setVisibility(View.GONE);
            toast("Rota criada com " + paradas.size() + " paradas.");
            finish();
            return;
        }
        proximaParada = i;
        Parada p = paradas.get(i);
        if (enderecosCriados.containsKey(i)) {
            vincularParada(prog, rotaId, i, enderecosCriados.get(i));
            return;
        }
        GestorRequests.Endereco e = new GestorRequests.Endereco();
        e.logradouro = p.logradouro;
        e.numero = p.numero;
        e.bairro = p.bairro;
        GestorData.api()
                .criarEndereco(e)
                .enqueue(
                        new Callback<GestorResponses.Endereco>() {
                            @Override
                            public void onResponse(
                                    Call<GestorResponses.Endereco> c,
                                    Response<GestorResponses.Endereco> r) {
                                if (!r.isSuccessful() || r.body() == null) {
                                    erro(
                                            prog,
                                            "Rota criada, mas o endereço "
                                                    + (i + 1)
                                                    + " falhou (erro "
                                                    + r.code()
                                                    + ").");
                                    return;
                                }
                                enderecosCriados.put(i, r.body().enderecoId);
                                vincularParada(prog, rotaId, i, r.body().enderecoId);
                            }

                            @Override
                            public void onFailure(Call<GestorResponses.Endereco> c, Throwable t) {
                                erro(prog, "Sem conexão com o servidor.");
                            }
                        });
    }

    private void vincularParada(View prog, String rotaId, int i, String enderecoId) {
        String tipo = i == 0 ? "INICIO" : i == paradas.size() - 1 ? "FIM" : "COLETA";
        GestorData.api()
                .adicionarEnderecoRota(
                        new GestorRequests.RotaEnd(
                                rotaId, enderecoId, paradas.get(i).texto, tipo, i + 1))
                .enqueue(
                        new Callback<GestorResponses.RotaEnd>() {
                            public void onResponse(
                                    Call<GestorResponses.RotaEnd> c,
                                    Response<GestorResponses.RotaEnd> r) {
                                if (!r.isSuccessful()) {
                                    erro(
                                            prog,
                                            "Não foi possível registrar a parada " + (i + 1) + ".");
                                    return;
                                }
                                proximaParada = i + 1;
                                gravarParada(prog, rotaId, proximaParada);
                            }

                            public void onFailure(Call<GestorResponses.RotaEnd> c, Throwable t) {
                                // GET confirma se o vínculo foi gravado antes de permitir uma
                                // repetição.
                                GestorData.api()
                                        .rota(rotaId)
                                        .enqueue(
                                                new Callback<GestorResponses.Rota>() {
                                                    public void onResponse(
                                                            Call<GestorResponses.Rota> c2,
                                                            Response<GestorResponses.Rota> r2) {
                                                        if (r2.isSuccessful()
                                                                && r2.body() != null
                                                                && r2.body().enderecos != null) {
                                                            for (GestorResponses.RotaEnd e :
                                                                    r2.body().enderecos)
                                                                if (e.ordem != null
                                                                        && e.ordem == i + 1
                                                                        && enderecoId.equals(
                                                                                e.enderecoId)) {
                                                                    proximaParada = i + 1;
                                                                    break;
                                                                }
                                                        }
                                                        erro(
                                                                prog,
                                                                "Não foi possível confirmar a"
                                                                    + " parada. Confira a rota"
                                                                    + " antes de continuar.");
                                                    }

                                                    public void onFailure(
                                                            Call<GestorResponses.Rota> c2,
                                                            Throwable t2) {
                                                        erro(
                                                                prog,
                                                                "Sem conexão. Confira a rota antes"
                                                                    + " de continuar.");
                                                    }
                                                });
                            }
                        });
    }

    private void erro(View prog, String msg) {
        salvando = false;
        if (!vivo()) return;
        prog.setVisibility(View.GONE);
        findViewById(R.id.btnCriarRota).setEnabled(true);
        GestorData.invalidar(GestorCache.ROTAS);
        toast(msg);
    }
}
