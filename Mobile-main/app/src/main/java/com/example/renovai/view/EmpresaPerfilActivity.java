package com.example.renovai.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.example.renovai.EmpresaBottomNav;
import com.example.renovai.EmpresaCarrinho;
import com.example.renovai.EmpresaData;
import com.example.renovai.GestorCache;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.SessionManager;
import com.example.renovai.dto.request.EmpresaRequests;
import com.example.renovai.dto.response.CategoriaMaterialResponse;
import com.example.renovai.dto.response.EmpresaResponses;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Tela 5.6 — Perfil da Empresa: nome, e-mail, CNPJ, endereço e materiais de interesse (editáveis).
 */
public class EmpresaPerfilActivity extends EmpresaBaseActivity {

    private EmpresaResponses.MeuPerfil eu;
    private List<CategoriaMaterialResponse> categorias = new ArrayList<>();
    private List<EmpresaResponses.MaterialInteresse> interesses = new ArrayList<>();
    private boolean salvandoInteresses;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_empresa_perfil, EmpresaBottomNav.Aba.PERFIL)) return;
        titulo("Perfil");

        campo(
                R.id.campoNomeE,
                "Nome da empresa",
                null,
                v ->
                        GestorUi.pedirTexto(
                                this,
                                "Nome da empresa",
                                "",
                                eu != null ? eu.nomeEmpresa : "",
                                GestorUi.TIPO_TEXTO,
                                t -> salvar("nomeEmpresa", t)));
        campo(
                R.id.campoEmailE,
                "E-mail",
                null,
                v ->
                        GestorUi.pedirTexto(
                                this,
                                "E-mail",
                                "email@exemplo.com",
                                eu != null ? eu.email : "",
                                GestorUi.TIPO_EMAIL,
                                t -> salvar("email", t)));
        campo(
                R.id.campoCnpjE,
                "CNPJ",
                null,
                v ->
                        GestorUi.pedirTexto(
                                this,
                                "CNPJ (00.000.000/0000-00)",
                                "",
                                eu != null ? eu.cnpj : "",
                                GestorUi.TIPO_TEXTO,
                                t -> salvar("cnpj", t)));
        campo(
                R.id.campoEnderecoE,
                "Endereço",
                null,
                v ->
                        GestorUi.pedirTexto(
                                this,
                                "Endereço",
                                "",
                                eu != null ? eu.endereco : "",
                                GestorUi.TIPO_TEXTO,
                                t -> salvar("endereco", t)));
        campo(R.id.campoInteresseE, "Materiais de interesse", null, v -> editarInteresses());

        findViewById(R.id.btnSuporteE)
                .setOnClickListener(
                        v ->
                                new AlertDialog.Builder(this)
                                        .setTitle("Suporte")
                                        .setMessage(
                                                "Precisa de ajuda? Envie um e-mail para"
                                                        + " suporte@renovai.com.")
                                        .setPositiveButton(
                                                "Enviar e-mail",
                                                (d, w) -> {
                                                    Intent i =
                                                            new Intent(
                                                                    Intent.ACTION_SENDTO,
                                                                    Uri.parse(
                                                                            "mailto:suporte@renovai.com"));
                                                    i.putExtra(
                                                            Intent.EXTRA_SUBJECT,
                                                            "Suporte — app Renovaí (Empresa)");
                                                    try {
                                                        startActivity(i);
                                                    } catch (Exception e) {
                                                        toast("Nenhum app de e-mail encontrado.");
                                                    }
                                                })
                                        .setNegativeButton("Fechar", null)
                                        .show());
        findViewById(R.id.btnSairE)
                .setOnClickListener(
                        v ->
                                GestorUi.confirmar(
                                        this,
                                        "Sair da conta",
                                        "Deseja sair da conta?",
                                        "Sair",
                                        this::sair));

        EmpresaData.categorias(
                new com.example.renovai.GestorData.Ouvinte<List<CategoriaMaterialResponse>>() {
                    @Override
                    public void aoReceber(List<CategoriaMaterialResponse> l, boolean c) {
                        categorias = l;
                    }

                    @Override
                    public void aoErro(String m) {}
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!vivo()) return;
        EmpresaData.meuPerfil(
                false,
                new com.example.renovai.GestorData.Ouvinte<EmpresaResponses.MeuPerfil>() {
                    @Override
                    public void aoReceber(EmpresaResponses.MeuPerfil p, boolean c) {
                        eu = p;
                        if (vivo()) mostrar();
                    }

                    @Override
                    public void aoErro(String m) {
                        if (vivo()) toast(m);
                    }
                });
        EmpresaData.materiaisInteresse(
                false,
                new com.example.renovai.GestorData.Ouvinte<
                        List<EmpresaResponses.MaterialInteresse>>() {
                    @Override
                    public void aoReceber(List<EmpresaResponses.MaterialInteresse> l, boolean c) {
                        interesses = l;
                        if (vivo()) mostrarInteresses();
                    }

                    @Override
                    public void aoErro(String m) {}
                });
    }

    private void mostrar() {
        preencherHeader(eu.nomeEmpresa);
        ((android.widget.TextView) findViewById(R.id.txtPerfilNomeE))
                .setText(eu.nomeEmpresa == null ? "Empresa" : eu.nomeEmpresa);
        ((android.widget.TextView) findViewById(R.id.txtIniciaisPerfilE))
                .setText(GestorUi.iniciais(eu.nomeEmpresa));
        valorCampo2(R.id.campoNomeE, eu.nomeEmpresa);
        valorCampo2(R.id.campoEmailE, eu.email);
        valorCampo2(R.id.campoCnpjE, eu.cnpj);
        valorCampo2(R.id.campoEnderecoE, eu.endereco);
    }

    private void mostrarInteresses() {
        StringBuilder sb = new StringBuilder();
        for (EmpresaResponses.MaterialInteresse m : interesses) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(m.categoriaNome);
        }
        valorCampo2(R.id.campoInteresseE, sb.length() == 0 ? "Nenhum selecionado" : sb.toString());
    }

    private void valorCampo2(int includeId, String valor) {
        ((android.widget.TextView) findViewById(includeId).findViewById(R.id.txtCampoValor))
                .setText(valor == null || valor.isEmpty() ? "—" : valor);
    }

    /**
     * Preenche um campo "rótulo + valor + lápis" (view_gestor_campo), igual ao Gestor/Motorista.
     */
    private View campo(int includeId, String rotulo, String valor, View.OnClickListener aoEditar) {
        View c = findViewById(includeId);
        ((android.widget.TextView) c.findViewById(R.id.txtCampoRotulo)).setText(rotulo);
        ((android.widget.TextView) c.findViewById(R.id.txtCampoValor))
                .setText(valor == null || valor.isEmpty() ? "—" : valor);
        View lapis = c.findViewById(R.id.btnCampoEditar);
        lapis.setVisibility(View.VISIBLE);
        lapis.setOnClickListener(aoEditar);
        c.findViewById(R.id.txtCampoValor).setOnClickListener(aoEditar);
        return c;
    }

    private void salvar(String campo, String valor) {
        if ("cnpj".equals(campo) && !valor.replaceAll("\\D", "").matches("[0-9]{14}")) {
            toast("CNPJ inválido (use 00.000.000/0000-00).");
            return;
        }
        if ("nomeEmpresa".equals(campo) && valor.isEmpty()) {
            toast("O nome não pode ficar vazio.");
            return;
        }
        if (valor.isEmpty() || valor.length() > 255) {
            toast("Preencha o campo com até 255 caracteres.");
            return;
        }
        EmpresaRequests.AtualizarMeuPerfil body = new EmpresaRequests.AtualizarMeuPerfil();
        switch (campo) {
            case "nomeEmpresa":
                body.nomeEmpresa = valor;
                break;
            case "email":
                body.email = valor;
                break;
            case "cnpj":
                body.cnpj = valor;
                break;
            default:
                body.endereco = valor;
        }
        View prog = findViewById(R.id.progressoPerfilE);
        prog.setVisibility(View.VISIBLE);
        EmpresaData.api()
                .atualizarMeuPerfil(body)
                .enqueue(
                        new Callback<EmpresaResponses.MeuPerfil>() {
                            @Override
                            public void onResponse(
                                    Call<EmpresaResponses.MeuPerfil> c,
                                    Response<EmpresaResponses.MeuPerfil> r) {
                                if (!vivo()) return;
                                prog.setVisibility(View.GONE);
                                if (r.isSuccessful() && r.body() != null) {
                                    eu = r.body();
                                    EmpresaData.invalidar("empresa:meuPerfil");
                                    mostrar();
                                    if ("email".equals(campo)) {
                                        toast(
                                                "E-mail alterado. Entre novamente com o novo"
                                                        + " e-mail.");
                                        sair();
                                    } else toast("Dados atualizados.");
                                } else {
                                    toast(EmpresaData.erro(r));
                                }
                            }

                            @Override
                            public void onFailure(Call<EmpresaResponses.MeuPerfil> c, Throwable t) {
                                if (!vivo()) return;
                                prog.setVisibility(View.GONE);
                                toast("Sem conexão com o servidor.");
                            }
                        });
    }

    private void editarInteresses() {
        if (salvandoInteresses) return;
        if (categorias.isEmpty()) {
            toast("Aguarde o carregamento das categorias.");
            return;
        }
        String[] nomes = new String[categorias.size()];
        boolean[] marcados = new boolean[categorias.size()];
        Set<String> idsAtuais = new LinkedHashSet<>();
        for (EmpresaResponses.MaterialInteresse m : interesses) idsAtuais.add(m.categoriaId);
        for (int i = 0; i < categorias.size(); i++) {
            nomes[i] = categorias.get(i).getNomeCategoria();
            marcados[i] = idsAtuais.contains(categorias.get(i).getCategoriaId());
        }
        boolean[] selecao = marcados.clone();
        new AlertDialog.Builder(this)
                .setTitle("Materiais de interesse")
                .setMultiChoiceItems(
                        nomes, selecao, (d, which, checked) -> selecao[which] = checked)
                .setPositiveButton("Salvar", (d, w) -> aplicarInteresses(marcados, selecao))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void aplicarInteresses(boolean[] antes, boolean[] depois) {
        if (salvandoInteresses || java.util.Arrays.equals(antes, depois)) return;
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < depois.length; i++)
            if (depois[i]) ids.add(categorias.get(i).getCategoriaId());
        if (ids.size() > 100) {
            toast("Selecione até 100 categorias.");
            return;
        }
        salvandoInteresses = true;
        findViewById(R.id.progressoPerfilE).setVisibility(View.VISIBLE);
        EmpresaData.api()
                .substituirInteresses(new EmpresaRequests.SubstituirInteresses(ids))
                .enqueue(
                        new Callback<List<EmpresaResponses.MaterialInteresse>>() {
                            @Override
                            public void onResponse(
                                    Call<List<EmpresaResponses.MaterialInteresse>> c,
                                    Response<List<EmpresaResponses.MaterialInteresse>> r) {
                                salvandoInteresses = false;
                                if (r.isSuccessful() && r.body() != null) {
                                    EmpresaData.invalidar(
                                            "empresa:interesses", "empresa:meuPerfil");
                                    GestorCache.guardar("empresa:interesses", r.body());
                                    interesses = r.body();
                                }
                                if (!vivo()) return;
                                findViewById(R.id.progressoPerfilE).setVisibility(View.GONE);
                                if (r.isSuccessful() && r.body() != null) {
                                    mostrarInteresses();
                                    toast("Materiais de interesse atualizados.");
                                } else toast(EmpresaData.erro(r));
                            }

                            @Override
                            public void onFailure(
                                    Call<List<EmpresaResponses.MaterialInteresse>> c, Throwable t) {
                                salvandoInteresses = false;
                                EmpresaData.invalidar("empresa:interesses");
                                if (!vivo()) return;
                                findViewById(R.id.progressoPerfilE).setVisibility(View.GONE);
                                toast(
                                        "Não foi possível confirmar a atualização. Confira a"
                                            + " seleção antes de tentar novamente.");
                                EmpresaData.materiaisInteresse(
                                        true,
                                        new com.example.renovai.GestorData.Ouvinte<
                                                List<EmpresaResponses.MaterialInteresse>>() {
                                            @Override
                                            public void aoReceber(
                                                    List<EmpresaResponses.MaterialInteresse> l,
                                                    boolean cache) {
                                                if (!cache) {
                                                    interesses = l;
                                                    if (vivo()) mostrarInteresses();
                                                }
                                            }

                                            @Override
                                            public void aoErro(String m) {
                                                if (vivo()) toast(m);
                                            }
                                        });
                            }
                        });
    }

    private void sair() {
        SessionManager.logout();
        GestorCache.limpar();
        EmpresaCarrinho.limpar();
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }
}
