package com.example.renovai.view;

import com.example.renovai.RenovaiDialogBuilder;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

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
public class EmpresaPerfilFragment extends EmpresaAbaFragment {

    private EmpresaResponses.MeuPerfil eu;
    private List<CategoriaMaterialResponse> categorias = new ArrayList<>();
    private List<EmpresaResponses.MaterialInteresse> interesses = new ArrayList<>();
    private boolean salvandoInteresses;
    private boolean salvandoDados;
    private AlertDialog edicao;

    public EmpresaPerfilFragment() {
        super(R.layout.activity_empresa_perfil);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle b) {
        super.onViewCreated(view, b);
        titulo("Perfil");

        campo(R.id.campoNomeE, "Nome da empresa *", null, v -> editarCampo("nomeEmpresa", "Nome da empresa", GestorUi.TIPO_TEXTO));
        campo(R.id.campoEmailE, "E-mail *", null, v -> editarCampo("email", "E-mail", GestorUi.TIPO_EMAIL));
        campo(R.id.campoCnpjE, "CNPJ *", null, v -> editarCampo("cnpj", "CNPJ", android.text.InputType.TYPE_CLASS_PHONE));
        campo(R.id.campoEnderecoE, "Endereço *", null, v -> editarCampo("endereco", "Endereço", GestorUi.TIPO_TEXTO));
        campo(R.id.campoTelefoneE, "Telefone *", null, v -> editarCampo("telefone", "Telefone com DDD", GestorUi.TIPO_TELEFONE));
        campo(R.id.campoCidadeE, "Cidade", null, v -> editarCampo("cidade", "Cidade", GestorUi.TIPO_TEXTO));
        campo(R.id.campoDescricaoE, "Descrição", null, v -> editarCampo("descricao", "Descrição da empresa", GestorUi.TIPO_TEXTO | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE));
        campo(R.id.campoInteresseE, "Materiais de interesse", null, v -> editarInteresses());

        findViewById(R.id.btnSuporteE)
                .setOnClickListener(v -> SuporteChatActivity.abrir(requireContext()));
        findViewById(R.id.btnSairE)
                .setOnClickListener(
                        v ->
                                GestorUi.confirmar(
                                        tela(),
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
    public void onResume() {
        super.onResume();
        if (!vivo()) return;
        if (eu == null) findViewById(R.id.progressoPerfilE).setVisibility(View.VISIBLE);
        EmpresaData.meuPerfil(
                false,
                new com.example.renovai.GestorData.Ouvinte<EmpresaResponses.MeuPerfil>() {
                    @Override
                    public void aoReceber(EmpresaResponses.MeuPerfil p, boolean c) {
                        eu = p;
                        if (vivo()) {
                            if (!salvandoDados && !salvandoInteresses)
                                findViewById(R.id.progressoPerfilE).setVisibility(View.GONE);
                            mostrar();
                        }
                    }

                    @Override
                    public void aoErro(String m) {
                        if (vivo()) {
                            findViewById(R.id.progressoPerfilE).setVisibility(View.GONE);
                            toast(m);
                        }
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
        valorCampo2(R.id.campoTelefoneE, eu.telefone);
        valorCampo2(R.id.campoCidadeE, eu.cidade);
        valorCampo2(R.id.campoDescricaoE, eu.descricao);
        android.widget.ImageView foto = findViewById(R.id.imgPerfilEmpresa);
        foto.setVisibility(com.example.renovai.ImagemUrl.normalizar(eu.imagemUrl) == null ? View.GONE : View.VISIBLE);
        com.example.renovai.Imagens.carregar(foto, eu.imagemUrl, true, com.example.renovai.TipoImagem.PERFIL);
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
        View.OnClickListener editar = v -> {
            if (eu == null) { toast(getString(R.string.perfil_carregando)); return; }
            if (!salvandoDados && !salvandoInteresses) aoEditar.onClick(v);
        };
        lapis.setContentDescription("Editar " + rotulo.replace(" *", ""));
        lapis.setOnClickListener(editar);
        c.findViewById(R.id.txtCampoValor).setOnClickListener(editar);
        return c;
    }

    private void editarCampo(String campo, String titulo, int tipo) {
        if (edicao != null && edicao.isShowing()) return;
        String atual;
        switch (campo) {
            case "nomeEmpresa": atual = eu.nomeEmpresa; break;
            case "email": atual = eu.email; break;
            case "cnpj": atual = eu.cnpj; break;
            case "endereco": atual = eu.endereco; break;
            case "telefone": atual = eu.telefone; break;
            case "cidade": atual = eu.cidade; break;
            default: atual = eu.descricao;
        }
        com.google.android.material.textfield.TextInputLayout input =
                new com.google.android.material.textfield.TextInputLayout(tela());
        input.setHint(titulo);
        input.setPadding(GestorUi.dp(tela(), 23), GestorUi.dp(tela(), 8), GestorUi.dp(tela(), 23), 0);
        com.google.android.material.textfield.TextInputEditText texto =
                new com.google.android.material.textfield.TextInputEditText(input.getContext());
        texto.setInputType(tipo);
        texto.setText(atual);
        texto.setSelectAllOnFocus(true);
        input.addView(texto, new android.widget.LinearLayout.LayoutParams(-1, -2));
        input.setHelperText("email".equals(campo) ? "Após alterar o e-mail, será necessário entrar novamente."
                : "telefone".equals(campo) ? "Ex.: (11) 99999-9999. Campo obrigatório."
                : "cidade".equals(campo) || "descricao".equals(campo) ? "Campo opcional." : "Campo obrigatório.");
        edicao = new RenovaiDialogBuilder(tela()).setTitle(titulo).setView(input)
                .setPositiveButton("Salvar", null).setNegativeButton("Cancelar", null).show();
        edicao.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (salvandoDados) return;
            String valor = texto.getText() == null ? "" : texto.getText().toString().trim();
            String erro = com.example.renovai.PerfilEmpresaValidacao.erro(campo, valor);
            input.setError(erro);
            if (erro != null) return;
            salvar(campo, valor, input);
        });
    }

    private void salvar(String campo, String valor, com.google.android.material.textfield.TextInputLayout input) {
        if (salvandoDados || eu == null) return;
        salvandoDados = true;
        String token = SessionManager.getToken();
        boolean mudouEmail = "email".equals(campo) && !valor.equalsIgnoreCase(eu.email);
        AlertDialog dialog = edicao;
        RenovaiDialogBuilder.emAndamento(dialog, true);
        input.getEditText().setEnabled(false);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Salvando…");
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
            case "telefone": body.telefone = valor; break;
            case "cidade": body.cidade = valor; break;
            case "descricao": body.descricao = valor; break;
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
                                salvandoDados = false;
                                if (!java.util.Objects.equals(token, SessionManager.getToken())) return;
                                if (r.isSuccessful() && r.body() != null) {
                                    eu = r.body();
                                    EmpresaData.invalidar("empresa:meuPerfil");
                                    GestorCache.guardar("empresa:meuPerfil", eu);
                                    dialog.dismiss();
                                    if (mudouEmail) {
                                        if (vivo()) toast("E-mail alterado. Entre novamente com o novo e-mail.");
                                        if (isAdded()) sair();
                                        else SessionManager.logout();
                                        return;
                                    }
                                }
                                if (!vivo()) return;
                                prog.setVisibility(View.GONE);
                                if (r.isSuccessful() && r.body() != null) {
                                    mostrar();
                                    toast("Dados atualizados.");
                                } else {
                                    input.setError(EmpresaData.erro(r));
                                    RenovaiDialogBuilder.emAndamento(dialog, false);
                                    input.getEditText().setEnabled(true);
                                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Salvar");
                                }
                            }

                            @Override
                            public void onFailure(Call<EmpresaResponses.MeuPerfil> c, Throwable t) {
                                salvandoDados = false;
                                if (!java.util.Objects.equals(token, SessionManager.getToken())) return;
                                if (!vivo()) return;
                                prog.setVisibility(View.GONE);
                                input.setError("Não foi possível confirmar o salvamento. Confira os dados antes de tentar novamente.");
                                RenovaiDialogBuilder.emAndamento(dialog, false);
                                input.getEditText().setEnabled(true);
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Salvar");
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
        new RenovaiDialogBuilder(tela())
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
        Intent i = new Intent(tela(), LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    @Override public void onDestroyView() {
        if (edicao != null) { edicao.dismiss(); edicao = null; }
        super.onDestroyView();
    }
}
