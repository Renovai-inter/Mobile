package com.example.renovai.view;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.renovai.GestorBottomNav;
import com.example.renovai.GestorData;
import com.example.renovai.GestorUi;
import com.example.renovai.R;
import com.example.renovai.dto.response.GestorResponses.FuncionarioDetalhe;

import java.util.ArrayList;
import java.util.List;

/** Tela 4.9 — Lista de funcionários (Pendentes / Ativos / Inativos) com busca. */
public class GestorFuncionariosActivity extends GestorBaseActivity {

    private List<FuncionarioDetalhe> todos = new ArrayList<>();
    private EditText busca;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!preparar(R.layout.activity_gestor_funcionarios, GestorBottomNav.Aba.EQUIPE)) return;
        busca = findViewById(R.id.edtBusca);
        busca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int c, int d) { render(); }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!vivo() || funcId() == null) return;
        GestorData.funcionarios(false, new GestorData.Ouvinte<List<FuncionarioDetalhe>>() {
            @Override public void aoReceber(List<FuncionarioDetalhe> l, boolean c) { todos = l; if (vivo()) render(); }
            @Override public void aoErro(String m) { if (vivo()) toast(m); }
        });
    }

    private void render() {
        LinearLayout secoes = findViewById(R.id.secoes);
        secoes.removeAllViews();
        String q = GestorUi.norm(busca.getText().toString().trim());
        List<FuncionarioDetalhe> pend = new ArrayList<>(), ativos = new ArrayList<>(), inat = new ArrayList<>();
        for (FuncionarioDetalhe f : todos) {
            if (!q.isEmpty() && !GestorUi.norm(f.nome).contains(q)) continue;
            if (Boolean.TRUE.equals(f.pendente)) pend.add(f);
            else if (f.inativo()) inat.add(f);
            else ativos.add(f);
        }
        secao(secoes, "PENDENTES", pend, Color.parseColor("#882B4E"), R.drawable.avatar_maroon_background, false);
        secao(secoes, "ATIVOS", ativos, Color.parseColor("#519059"), R.drawable.avatar_green_background, false);
        secao(secoes, "INATIVOS", inat, Color.parseColor("#DDDDDD"), R.drawable.avatar_gray_background, true);
        findViewById(R.id.txtVazio).setVisibility(pend.isEmpty() && ativos.isEmpty() && inat.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void secao(LinearLayout raiz, String titulo, List<FuncionarioDetalhe> lista, int corBorda, int avatarBg, boolean apagado) {
        if (lista.isEmpty()) return;
        View s = LayoutInflater.from(this).inflate(R.layout.item_gestor_secao, raiz, false);
        ((TextView) s.findViewById(R.id.txtSecaoTitulo)).setText(titulo);
        ((TextView) s.findViewById(R.id.txtSecaoContador)).setText(String.valueOf(lista.size()));
        raiz.addView(s);
        for (FuncionarioDetalhe f : lista) {
            View v = LayoutInflater.from(this).inflate(R.layout.item_gestor_funcionario, raiz, false);
            GestorUi.borda(v.findViewById(R.id.cardFunc), corBorda);
            v.findViewById(R.id.bgAvatarFunc).setBackgroundResource(avatarBg);
            TextView av = v.findViewById(R.id.txtAvatarFunc);
            av.setText(GestorUi.iniciais(f.nome));
            av.setTextColor(apagado ? Color.parseColor("#BDBDBD") : Color.WHITE);
            TextView nome = v.findViewById(R.id.txtFuncNome);
            nome.setText(f.nome == null ? "—" : f.nome);
            nome.setTextColor(apagado ? Color.parseColor("#777777") : Color.parseColor("#111111"));
            ((TextView) v.findViewById(R.id.txtFuncCargo)).setText(f.cargo == null ? "Função" : f.cargo);
            v.setOnClickListener(x -> {
                Intent i = new Intent(this, GestorFuncionarioDetalheActivity.class);
                i.putExtra(GestorFuncionarioDetalheActivity.EXTRA_ID, f.funcionarioId);
                startActivity(i);
            });
            raiz.addView(v);
        }
    }
}
