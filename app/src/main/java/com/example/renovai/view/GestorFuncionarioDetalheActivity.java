package com.example.renovai.view;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.renovai.GestorBottomNav;
import com.example.renovai.GestorCache;
import com.example.renovai.GestorData;
import com.example.renovai.R;
import com.example.renovai.dto.response.FuncionarioResponse;
import com.example.renovai.dto.response.GestorResponses;
import com.example.renovai.dto.response.GestorResponses.FuncionarioDetalhe;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Detalhes do funcionário (4.9): nome e função. A função é editável via PUT
 * /funcionarios/{id}/cargo/{cargoId}.
 */
public class GestorFuncionarioDetalheActivity extends GestorBaseActivity {

    public static final String EXTRA_ID = "funcionarioId";

    private String id;
    private boolean salvando;
    private FuncionarioDetalhe func;
    private String novoCargoId, novoCargoNome;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_gestor_funcionario_detalhe, GestorBottomNav.Aba.NENHUMA))
            return;
        titulo("Detalhes do Funcionário");
        topoPerfil("Perfil: Funcionário");
        id = getIntent().getStringExtra(EXTRA_ID);

        campo(R.id.campoNome, "Nome", null, null);
        findViewById(R.id.campoEmail)
                .setVisibility(View.GONE); // a API não devolve e-mail de funcionário
        campo(R.id.campoFuncao, "Função", null, v -> escolherCargo());
        findViewById(R.id.btnSalvar).setOnClickListener(v -> salvar());

        GestorData.funcionarios(
                false,
                (l, c) -> {
                    for (FuncionarioDetalhe f : l)
                        if (f.funcionarioId != null && f.funcionarioId.equalsIgnoreCase(id))
                            func = f;
                    if (vivo()) preencher();
                });
    }

    private void preencher() {
        if (func == null) return;
        valorCampo(R.id.campoNome, func.nome);
        valorCampo(R.id.campoFuncao, novoCargoNome != null ? novoCargoNome : func.cargo);
        String st =
                Boolean.TRUE.equals(func.pendente)
                        ? "Pré-cadastro pendente: o funcionário ainda não completou o cadastro."
                        : func.inativo() ? "Funcionário inativo." : "Funcionário ativo.";
        ((TextView) findViewById(R.id.txtStatusFunc)).setText(st);
    }

    private void escolherCargo() {
        GestorData.cargos(
                new GestorData.Ouvinte<List<GestorResponses.Cargo>>() {
                    private boolean apresentado;

                    @Override
                    public void aoReceber(List<GestorResponses.Cargo> l, boolean c) {
                        if (!vivo()
                                || apresentado
                                || (c && !GestorCache.valido(GestorCache.CARGOS))) return;
                        apresentado = true;
                        final List<GestorResponses.Cargo> cargos = new ArrayList<>(l);
                        String[] nomes = new String[cargos.size()];
                        for (int i = 0; i < nomes.length; i++) nomes[i] = cargos.get(i).cargo;
                        new AlertDialog.Builder(GestorFuncionarioDetalheActivity.this)
                                .setTitle("Função")
                                .setItems(
                                        nomes,
                                        (d, w) -> {
                                            novoCargoId = cargos.get(w).cargoId;
                                            novoCargoNome = cargos.get(w).cargo;
                                            valorCampo(R.id.campoFuncao, novoCargoNome);
                                        })
                                .setNegativeButton("Cancelar", null)
                                .show();
                    }

                    @Override
                    public void aoErro(String m) {
                        toast(m);
                    }
                });
    }

    private void salvar() {
        if (func == null || salvando) return;
        if (novoCargoId == null) {
            toast("Nenhuma alteração para salvar.");
            return;
        }
        salvando = true;
        findViewById(R.id.btnSalvar).setEnabled(false);
        final View prog = findViewById(R.id.progresso);
        prog.setVisibility(View.VISIBLE);
        GestorData.api()
                .atualizarCargo(id, novoCargoId)
                .enqueue(
                        new Callback<FuncionarioResponse>() {
                            @Override
                            public void onResponse(
                                    Call<FuncionarioResponse> c, Response<FuncionarioResponse> r) {
                                if (r.isSuccessful()) concluido(prog);
                                else {
                                    salvando = false;
                                    if (!vivo()) return;
                                    findViewById(R.id.btnSalvar).setEnabled(true);
                                    prog.setVisibility(View.GONE);
                                    toast(
                                            "Não foi possível alterar a função (erro "
                                                    + r.code()
                                                    + ").");
                                }
                            }

                            @Override
                            public void onFailure(Call<FuncionarioResponse> c, Throwable t) {
                                salvando = false;
                                if (!vivo()) return;
                                findViewById(R.id.btnSalvar).setEnabled(true);
                                prog.setVisibility(View.GONE);
                                toast("Sem conexão com o servidor.");
                            }
                        });
    }

    private void concluido(View prog) {
        if (!vivo()) return;
        prog.setVisibility(View.GONE);
        GestorData.invalidar(GestorCache.FUNCIONARIOS);
        toast("Funcionário atualizado.");
        finish();
    }
}
