package com.example.renovai.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.renovai.EmpresaBottomNav;
import com.example.renovai.EmpresaCarrinho;
import com.example.renovai.EmpresaData;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.dto.request.EmpresaRequests;
import com.example.renovai.dto.response.PedidoResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Confirma e envia o pedido em uma única operação idempotente. */
public class EmpresaFinalizarPedidoActivity extends EmpresaBaseActivity {
    private boolean enviando;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_empresa_finalizar_pedido, EmpresaBottomNav.Aba.NENHUMA))
            return;
        titulo("Confirmar pedido");
        if (EmpresaCarrinho.itens.isEmpty()) {
            toast("O carrinho está vazio.");
            finish();
            return;
        }
        ((TextView) findViewById(R.id.txtNomeCoopFinal)).setText(EmpresaCarrinho.cooperativaNome);
        ((TextView) findViewById(R.id.txtIniciaisCoopFinal))
                .setText(GestorUi.iniciais(EmpresaCarrinho.cooperativaNome));
        ((TextView) findViewById(R.id.txtTotalFinal))
                .setText(GestorUi.dinheiro(EmpresaCarrinho.total()));
        androidx.activity.OnBackPressedCallback voltar =
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (enviando || EmpresaCarrinho.envio != null)
                            toast("Confirme o envio antes de alterar este pedido.");
                        else finish();
                    }
                };
        getOnBackPressedDispatcher().addCallback(this, voltar);
        findViewById(R.id.btnVoltar).setOnClickListener(v -> voltar.handleOnBackPressed());
        findViewById(R.id.btnConcluirPedido).setOnClickListener(v -> concluir());
    }

    private void concluir() {
        if (enviando) return;
        if (EmpresaCarrinho.envio == null) {
            EmpresaRequests.EnviarPedido body = new EmpresaRequests.EnviarPedido();
            body.cooperativaId = EmpresaCarrinho.cooperativaId;
            body.chaveSolicitacao = EmpresaCarrinho.chaveSolicitacao;
            for (EmpresaCarrinho.Item it : EmpresaCarrinho.itens)
                body.itens.add(
                        new EmpresaRequests.PedidoItem(
                                it.materialId, it.quantidadeKg, it.precoUnitario));
            EmpresaCarrinho.envio = body;
        }
        if (!EmpresaCarrinho.salvarPendente(this)) {
            EmpresaCarrinho.envio = null;
            toast("Não foi possível guardar a solicitação neste dispositivo. Tente novamente.");
            return;
        }
        final String token = com.example.renovai.SessionManager.getToken();
        enviando = true;
        findViewById(R.id.progressoFinal).setVisibility(View.VISIBLE);
        findViewById(R.id.btnConcluirPedido).setEnabled(false);
        EmpresaData.api()
                .enviarPedido(EmpresaCarrinho.envio)
                .enqueue(
                        new Callback<PedidoResponse>() {
                            @Override
                            public void onResponse(
                                    Call<PedidoResponse> c, Response<PedidoResponse> r) {
                                if (!java.util.Objects.equals(
                                        token, com.example.renovai.SessionManager.getToken()))
                                    return;
                                if (!r.isSuccessful() || r.body() == null) {
                                    if (r.code() >= 400 && r.code() < 500) {
                                        EmpresaCarrinho.envio = null;
                                        EmpresaCarrinho.apagarPendente(
                                                EmpresaFinalizarPedidoActivity.this);
                                    }
                                    falhar(EmpresaData.erro(r));
                                    return;
                                }
                                EmpresaData.invalidar(
                                        "empresa:pedidos",
                                        "empresa:dashboard",
                                        "empresa:negociacoes");
                                EmpresaCarrinho.apagarPendente(EmpresaFinalizarPedidoActivity.this);
                                EmpresaCarrinho.limpar();
                                if (!vivo()) return;
                                toast("Pedido enviado. Aguarde a resposta da cooperativa.");
                                Intent i =
                                        new Intent(
                                                EmpresaFinalizarPedidoActivity.this,
                                                EmpresaPedidosActivity.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(i);
                                finish();
                            }

                            @Override
                            public void onFailure(Call<PedidoResponse> c, Throwable t) {
                                if (!java.util.Objects.equals(
                                        token, com.example.renovai.SessionManager.getToken()))
                                    return;
                                falhar(
                                        "Não foi possível confirmar o envio. Tente novamente para"
                                            + " consultar a mesma solicitação.");
                            }
                        });
    }

    private void falhar(String msg) {
        enviando = false;
        if (!vivo()) return;
        findViewById(R.id.progressoFinal).setVisibility(View.GONE);
        findViewById(R.id.btnConcluirPedido).setEnabled(true);
        toast(msg);
    }
}
