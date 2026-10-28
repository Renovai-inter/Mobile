package com.example.renovai.view;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.dto.response.CooperativaResponse;
import com.example.renovai.dto.response.EmpresaResponse;
import com.example.renovai.dto.response.PerfilResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

/**
 * Abas de lista do Admin do site — Cooperativas, Empresas e Contas (perfis) de todo o sistema:
 * busca pelo nome, detalhes de cada item e as ações que a API libera para o ADMIN_SITE
 * (excluir; nas contas também desativar).
 */
public class AdminListaFragment extends AdminAbaFragment {

    private static final String ARG_TIPO = "tipo";
    static final int COOPERATIVAS = 0, EMPRESAS = 1, CONTAS = 2;

    /** Uma linha da lista, igual para os três tipos. */
    private static final class Item {
        String id, nome, sub;
        boolean inativo;
        String[][] detalhes;
    }

    private int tipo;
    private int filtro; // contas: 0 todas, 1 ativas, 2 inativas
    private final List<Item> todos = new ArrayList<>();
    private EditText busca;

    public AdminListaFragment() {
        super(R.layout.fragment_admin_lista);
    }

    public static AdminListaFragment nova(int tipo) {
        AdminListaFragment f = new AdminListaFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_TIPO, tipo);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle b) {
        super.onViewCreated(view, b);
        tipo = requireArguments().getInt(ARG_TIPO);
        ((TextView) findViewById(R.id.txtTituloLista))
                .setText(tipo == COOPERATIVAS ? "Cooperativas" : tipo == EMPRESAS ? "Empresas" : "Contas");
        busca = findViewById(R.id.edtBusca);
        busca.setHint(tipo == CONTAS ? "Buscar pelo e-mail ou vínculo" : "Buscar pelo nome");
        busca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) {}
            @Override public void afterTextChanged(Editable s) { render(); }
        });
        if (tipo == CONTAS) {
            LinearLayout trilha = findViewById(R.id.trilhaFiltro);
            trilha.setVisibility(View.VISIBLE);
            GestorUi.segmentado(trilha, new String[] {"Todas", "Ativas", "Inativas"}, filtro, false, i -> {
                filtro = i;
                render();
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        carregar();
    }

    private void carregar() {
        View progresso = findViewById(R.id.progressoLista);
        if (todos.isEmpty()) progresso.setVisibility(View.VISIBLE);
        Runnable fim = () -> progresso.setVisibility(View.GONE);
        if (tipo == COOPERATIVAS) chamar(api().cooperativas(), this::receberCooperativas, fim);
        else if (tipo == EMPRESAS) chamar(api().empresas(), this::receberEmpresas, fim);
        else chamar(api().perfis(), this::receberContas, fim);
    }

    private void receberCooperativas(List<CooperativaResponse> lista) {
        todos.clear();
        if (lista != null) for (CooperativaResponse c : lista) {
            Item it = new Item();
            it.id = c.getCooperativaId();
            it.nome = c.getNome() == null ? "Cooperativa" : c.getNome();
            String cooperados = c.getNumeroCooperados() == null ? null : c.getNumeroCooperados() + " cooperados";
            it.sub = juntar(c.getCidade(), cooperados);
            it.detalhes = new String[][] {
                {"Cidade", c.getCidade()},
                {"Cooperados", c.getNumeroCooperados() == null ? null : String.valueOf(c.getNumeroCooperados())},
                {"Horário", c.getHorarioFuncionamento()},
                {"Contato preferencial", c.getContatoPreferencial()},
                {"Descrição", c.getDescricao()},
            };
            todos.add(it);
        }
        render();
    }

    private void receberEmpresas(List<EmpresaResponse> lista) {
        todos.clear();
        if (lista != null) for (EmpresaResponse e : lista) {
            Item it = new Item();
            it.id = e.getEmpresaId();
            it.nome = e.getNome() == null ? "Empresa" : e.getNome();
            it.sub = e.getDescricao() == null || e.getDescricao().isEmpty() ? "Empresa" : e.getDescricao();
            it.detalhes = new String[][] {{"Descrição", e.getDescricao()}};
            todos.add(it);
        }
        render();
    }

    private void receberContas(List<PerfilResponse> lista) {
        todos.clear();
        if (lista != null) for (PerfilResponse p : lista) {
            Item it = new Item();
            it.id = p.getPerfilId();
            it.nome = p.getEmail() == null ? "Conta" : p.getEmail();
            String vinculo = p.getEmpresaNome() != null ? "Empresa · " + p.getEmpresaNome()
                    : p.getCooperativaNome() != null ? "Cooperativa · " + p.getCooperativaNome() : "Sem vínculo";
            it.inativo = !Boolean.TRUE.equals(p.getEstaAtivo());
            it.sub = vinculo + (it.inativo ? " · Inativa" : "");
            it.detalhes = new String[][] {
                {"E-mail", p.getEmail()},
                {"CNPJ", p.getCnpj()},
                {"Vínculo", vinculo},
                {"Situação", it.inativo ? "Inativa" : "Ativa"},
            };
            todos.add(it);
        }
        render();
    }

    private void render() {
        if (!vivo()) return;
        String q = GestorUi.norm(busca.getText().toString().trim());
        List<Item> visiveis = new ArrayList<>();
        for (Item it : todos) {
            if (filtro == 1 && it.inativo || filtro == 2 && !it.inativo) continue;
            if (!q.isEmpty() && !GestorUi.norm(it.nome + " " + it.sub).contains(q)) continue;
            visiveis.add(it);
        }
        visiveis.sort((a, b) -> a.nome.compareToIgnoreCase(b.nome));
        ((TextView) findViewById(R.id.txtContador))
                .setText(visiveis.size() + (visiveis.size() == 1 ? " encontrado" : " encontrados"));
        findViewById(R.id.txtVazio).setVisibility(visiveis.isEmpty() ? View.VISIBLE : View.GONE);

        LinearLayout raiz = findViewById(R.id.listaItens);
        raiz.removeAllViews();
        for (Item it : visiveis) {
            View v = LayoutInflater.from(requireContext()).inflate(R.layout.item_gestor_funcionario, raiz, false);
            v.findViewById(R.id.bgAvatarFunc).setBackgroundResource(
                    it.inativo ? R.drawable.avatar_gray_background
                            : tipo == EMPRESAS ? R.drawable.avatar_maroon_background : R.drawable.avatar_green_background);
            TextView avatar = v.findViewById(R.id.txtAvatarFunc);
            avatar.setText(GestorUi.iniciais(it.nome));
            avatar.setTextColor(it.inativo ? Color.parseColor("#BDBDBD") : Color.WHITE);
            ((TextView) v.findViewById(R.id.txtFuncNome)).setText(it.nome);
            ((TextView) v.findViewById(R.id.txtFuncCargo)).setText(it.sub);
            v.setOnClickListener(x -> detalhar(it));
            raiz.addView(v);
        }
    }

    private void detalhar(Item it) {
        StringBuilder texto = new StringBuilder();
        for (String[] linha : it.detalhes) {
            if (linha[1] == null || linha[1].trim().isEmpty()) continue;
            if (texto.length() > 0) texto.append("\n\n");
            texto.append(linha[0]).append(":\n").append(linha[1].trim());
        }
        MaterialAlertDialogBuilder dialogo = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(it.nome)
                .setMessage(texto.length() == 0 ? "Sem mais informações." : texto)
                .setPositiveButton("Fechar", null)
                .setNegativeButton("Excluir", (d, w) -> GestorUi.confirmar(
                        tela(),
                        "Excluir " + rotulo(),
                        "Excluir \"" + it.nome + "\" do Renovaí? Essa ação não pode ser desfeita.",
                        "Excluir",
                        () -> executar(excluir(it.id), rotuloCapitalizado() + " excluída.")));
        if (tipo == CONTAS && !it.inativo) {
            dialogo.setNeutralButton("Desativar", (d, w) -> GestorUi.confirmar(
                    tela(),
                    "Desativar conta",
                    "A conta " + it.nome + " não vai mais conseguir entrar no Renovaí. Deseja continuar?",
                    "Desativar",
                    () -> executar(api().desativarPerfil(it.id), "Conta desativada.")));
        }
        dialogo.show();
    }

    private Call<Void> excluir(String id) {
        if (tipo == COOPERATIVAS) return api().excluirCooperativa(id);
        if (tipo == EMPRESAS) return api().excluirEmpresa(id);
        return api().excluirPerfil(id);
    }

    private <T> void executar(Call<T> call, String sucesso) {
        View progresso = findViewById(R.id.progressoLista);
        progresso.setVisibility(View.VISIBLE);
        chamar(call, r -> {
            toast(sucesso);
            carregar();
        }, () -> progresso.setVisibility(View.GONE));
    }

    private String rotulo() {
        return tipo == COOPERATIVAS ? "cooperativa" : tipo == EMPRESAS ? "empresa" : "conta";
    }

    private String rotuloCapitalizado() {
        String r = rotulo();
        return Character.toUpperCase(r.charAt(0)) + r.substring(1);
    }

    private static String juntar(String a, String b) {
        boolean temA = a != null && !a.trim().isEmpty(), temB = b != null && !b.trim().isEmpty();
        if (temA && temB) return a.trim() + " · " + b;
        if (temA) return a.trim();
        return temB ? b : "Sem informações";
    }
}
