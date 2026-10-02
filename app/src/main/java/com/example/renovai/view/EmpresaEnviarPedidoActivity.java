package com.example.renovai.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.renovai.EmpresaBottomNav;
import com.example.renovai.EmpresaCarrinho;
import com.example.renovai.EmpresaData;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.dto.response.EmpresaResponses;
import com.example.renovai.dto.response.MaterialResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

/** Tela 5.3 — Pedido: escolhe material e peso, acumula no carrinho, segue para a finalização. */
public class EmpresaEnviarPedidoActivity extends EmpresaBaseActivity {

    public static final String EXTRA_COOPERATIVA_ID = "cooperativaId",
            EXTRA_COOPERATIVA_NOME = "cooperativaNome";

    private List<MaterialResponse> materiais = new ArrayList<>();
    private MaterialResponse selecionado;
    private final java.util.Map<String, Double> estoques = new java.util.HashMap<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_empresa_enviar_pedido, EmpresaBottomNav.Aba.NENHUMA))
            return;
        titulo("Pedido");

        String coopId = getIntent().getStringExtra(EXTRA_COOPERATIVA_ID);
        String coopNome = getIntent().getStringExtra(EXTRA_COOPERATIVA_NOME);

        if (coopId == null) {
            toast("Erro ao carregar dados da cooperativa.");
            finish();
            return;
        }

        EmpresaCarrinho.restaurarPendente(this);
        if (EmpresaCarrinho.envio != null) {
            startActivity(new Intent(this, EmpresaFinalizarPedidoActivity.class));
            finish();
            return;
        }
        EmpresaCarrinho.iniciar(coopId, coopNome);

        ((TextView) findViewById(R.id.txtNomeCoopPedido))
                .setText(coopNome == null ? "Cooperativa" : coopNome);
        ((TextView) findViewById(R.id.txtIniciaisCoopPedido)).setText(GestorUi.iniciais(coopNome));

        EmpresaData.api()
                .perfilPublico(coopId)
                .enqueue(
                        new Callback<>() {
                            @Override
                            public void onResponse(
                                    Call<EmpresaResponses.CooperativaPerfilPublico> c,
                                    Response<EmpresaResponses.CooperativaPerfilPublico> r) {
                                if (r.isSuccessful() && r.body() != null && vivo()) {
                                    ((TextView) findViewById(R.id.txtContatoCoopPedido))
                                            .setText(r.body().contato());
                                    if (r.body().materiaisDisponiveis != null)
                                        for (com.example.renovai.dto.response.GestorResponses
                                                        .Estoque
                                                e : r.body().materiaisDisponiveis)
                                            if (e.materialId != null && e.quantidadeKg != null)
                                                estoques.put(e.materialId, e.quantidadeKg);
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<EmpresaResponses.CooperativaPerfilPublico> c,
                                    Throwable t) {}
                        });

        EmpresaData.api()
                .materiaisDisponiveis()
                .enqueue(
                        new Callback<>() {
                            @Override
                            public void onResponse(
                                    Call<List<MaterialResponse>> c,
                                    Response<List<MaterialResponse>> r) {
                                if (!r.isSuccessful() || r.body() == null || !vivo()) return;
                                materiais = new ArrayList<>();
                                for (MaterialResponse m : r.body())
                                    if (Boolean.TRUE.equals(m.getEstaDisponivel()))
                                        materiais.add(m);
                                if (materiais.isEmpty())
                                    toast(
                                            "Esta cooperativa não tem materiais disponíveis no"
                                                + " momento.");
                            }

                            @Override
                            public void onFailure(Call<List<MaterialResponse>> c, Throwable t) {
                                if (vivo()) toast("Sem conexão com o servidor.");
                            }
                        });

        findViewById(R.id.campoMaterial).setOnClickListener(v -> escolherMaterial());

        EditText edtPeso = findViewById(R.id.edtPesoPedido);
        edtPeso.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        atualizarTotal();
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });

        ((EditText) findViewById(R.id.edtPrecoProposto))
                .addTextChangedListener(
                        new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int a, int c, int d) {}

                            @Override
                            public void onTextChanged(CharSequence s, int a, int c, int d) {
                                atualizarTotal();
                            }

                            @Override
                            public void afterTextChanged(Editable s) {}
                        });
        findViewById(R.id.btnAdicionarCarrinho).setOnClickListener(v -> adicionar());
        findViewById(R.id.btnFinalizarPedido)
                .setOnClickListener(
                        v -> {
                            if (selecionado != null
                                    || !((EditText) findViewById(R.id.edtPesoPedido))
                                            .getText()
                                            .toString()
                                            .trim()
                                            .isEmpty()) {
                                if (!adicionar()) return;
                            }
                            if (EmpresaCarrinho.itens.isEmpty()) {
                                toast("Adicione um material ao pedido.");
                                return;
                            }
                            startActivity(new Intent(this, EmpresaFinalizarPedidoActivity.class));
                        });
        renderCarrinho();
    }

    private void escolherMaterial() {
        if (materiais.isEmpty()) {
            toast("Nenhum material disponível para esta cooperativa.");
            return;
        }
        List<MaterialResponse> disponiveis = new ArrayList<>();
        for (MaterialResponse m : materiais)
            if (estoques.getOrDefault(m.getMaterialId(), 0d) > 0) disponiveis.add(m);
        if (disponiveis.isEmpty()) {
            toast(
                    "Nenhum material com estoque disponível. Aguarde o carregamento ou tente outra"
                        + " cooperativa.");
            return;
        }
        String[] nomes = new String[disponiveis.size()];
        for (int i = 0; i < nomes.length; i++)
            nomes[i] =
                    disponiveis.get(i).getCategoriaNome()
                            + " — "
                            + GestorUi.kg(estoques.get(disponiveis.get(i).getMaterialId()));
        new AlertDialog.Builder(this)
                .setTitle("Tipo de Material")
                .setItems(
                        nomes,
                        (d, w) -> {
                            selecionado = disponiveis.get(w);
                            ((EditText) findViewById(R.id.edtPrecoProposto))
                                    .setText(
                                            selecionado.getPrecoSugerido() == null
                                                    ? ""
                                                    : selecionado
                                                            .getPrecoSugerido()
                                                            .toPlainString());
                            ((TextView) findViewById(R.id.txtMaterialSel))
                                    .setText(selecionado.getCategoriaNome());
                            ((TextView) findViewById(R.id.txtMaterialSel)).setTextColor(0xFF222222);
                            atualizarTotal();
                        })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private double pesoAtual() {
        Double p =
                GestorUi.parseValor(
                        ((EditText) findViewById(R.id.edtPesoPedido)).getText().toString());
        return p == null ? 0 : p;
    }

    private void atualizarTotal() {
        Double informado =
                GestorUi.parseValor(
                        ((EditText) findViewById(R.id.edtPrecoProposto)).getText().toString());
        double preco = informado == null || !Double.isFinite(informado) ? 0 : informado;
        ((TextView) findViewById(R.id.txtPrecoKgPedido)).setText(GestorUi.dinheiro(preco));
        ((TextView) findViewById(R.id.txtPrecoTotalPedido))
                .setText(GestorUi.dinheiro(preco * pesoAtual()));
    }

    /**
     * Adiciona o material atual ao carrinho; retorna false (e avisa) se os campos não estão
     * prontos.
     */
    private boolean adicionar() {
        if (selecionado == null) {
            toast("Selecione o tipo de material.");
            return false;
        }
        double peso = pesoAtual();
        if (!Double.isFinite(peso)
                || peso < 0.001
                || java.math.BigDecimal.valueOf(peso).stripTrailingZeros().scale() > 3
                || peso >= 10000000) {
            toast("Informe um peso válido.");
            return false;
        }
        Double precoInformado =
                GestorUi.parseValor(
                        ((EditText) findViewById(R.id.edtPrecoProposto)).getText().toString());
        if (precoInformado == null
                || !Double.isFinite(precoInformado)
                || precoInformado < 0.01
                || precoInformado >= 100000000
                || java.math.BigDecimal.valueOf(precoInformado).stripTrailingZeros().scale() > 2) {
            toast("Informe um valor por kg válido, com até duas casas decimais.");
            return false;
        }
        double preco = precoInformado;
        double acumulado = peso;
        for (EmpresaCarrinho.Item it : EmpresaCarrinho.itens)
            if (selecionado.getMaterialId().equals(it.materialId)) acumulado += it.quantidadeKg;
        if (acumulado > estoques.getOrDefault(selecionado.getMaterialId(), 0d)) {
            toast("Quantidade maior que o estoque disponível.");
            return false;
        }
        if (EmpresaCarrinho.itens.size() >= 100) {
            toast("Limite de 100 itens por pedido.");
            return false;
        }
        EmpresaCarrinho.itens.add(
                new EmpresaCarrinho.Item(
                        selecionado.getMaterialId(), selecionado.getCategoriaNome(), peso, preco));
        selecionado = null;
        ((TextView) findViewById(R.id.txtMaterialSel)).setText("Selecionar material");
        ((TextView) findViewById(R.id.txtMaterialSel)).setTextColor(0xFF8A8A8A);
        ((EditText) findViewById(R.id.edtPesoPedido)).setText("");
        ((EditText) findViewById(R.id.edtPrecoProposto)).setText("");
        atualizarTotal();
        renderCarrinho();
        return true;
    }

    private void renderCarrinho() {
        LinearLayout raiz = findViewById(R.id.listaCarrinho);
        raiz.removeAllViews();
        for (int i = 0; i < EmpresaCarrinho.itens.size(); i++) {
            final int idx = i;
            EmpresaCarrinho.Item it = EmpresaCarrinho.itens.get(i);
            View v = LayoutInflater.from(this).inflate(R.layout.item_empresa_carrinho, raiz, false);
            ((TextView) v.findViewById(R.id.txtCarrinhoNome)).setText(it.categoriaNome);
            ((TextView) v.findViewById(R.id.txtCarrinhoKg)).setText(GestorUi.kg(it.quantidadeKg));
            ((TextView) v.findViewById(R.id.txtCarrinhoValor))
                    .setText(GestorUi.dinheiro(it.total()));
            v.findViewById(R.id.btnRemoverCarrinho)
                    .setOnClickListener(
                            x -> {
                                EmpresaCarrinho.itens.remove(idx);
                                renderCarrinho();
                            });
            raiz.addView(v);
        }
    }
}
