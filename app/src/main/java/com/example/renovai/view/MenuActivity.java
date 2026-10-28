package com.example.renovai.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.renovai.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Casca dos fluxos com menu inferior (Cooperado, Gestor, Empresa e Motorista): cada aba do menu é
 * um Fragment trocado dentro desta Activity, e cada troca entra no histórico. O voltar (a seta da
 * tela ou o gesto do sistema) desfaz a última troca e volta para a aba anterior; sem histórico,
 * fecha a casca e volta para a tela que a abriu.
 */
public abstract class MenuActivity extends AppCompatActivity {

    /** Id do item do menu (R.id.nav_...) da aba que a casca deve mostrar ao abrir. */
    public static final String EXTRA_ABA = "com.example.renovai.ABA";

    private BottomNavigationView nav;

    /** Abre a casca na aba pedida, por cima da tela atual (o voltar retorna a ela). */
    public static void abrir(Context c, Class<? extends MenuActivity> casca, int aba) {
        Intent i = new Intent(c, casca);
        i.putExtra(EXTRA_ABA, aba);
        c.startActivity(i);
    }

    /** Id do item do menu da aba principal (Home). */
    protected abstract int abaInicial();

    /** Fragment da aba (id do item do menu), ou null se o id não é uma aba deste fluxo. */
    @Nullable
    protected abstract Fragment criarAba(int aba);

    /**
     * Monta a casca: o layout precisa ter o container R.id.conteudoAbas e o menu com o id
     * informado. Na primeira abertura mostra a aba do EXTRA_ABA (ou a Home), sem histórico.
     */
    protected BottomNavigationView montar(int layout, int idNav, @Nullable Bundle estado) {
        setContentView(layout);
        nav = findViewById(idNav);

        FragmentManager fm = getSupportFragmentManager();
        if (estado == null) {
            int aba = getIntent().getIntExtra(EXTRA_ABA, abaInicial());
            Fragment f = criarAba(aba);
            if (f == null) {
                aba = abaInicial();
                f = criarAba(aba);
            }
            fm.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.conteudoAbas, f, String.valueOf(aba))
                    .commitNow();
        }
        fm.addOnBackStackChangedListener(this::marcarAbaAtual);
        marcarAbaAtual();

        nav.setOnItemSelectedListener(
                item -> {
                    abrirAba(item.getItemId());
                    return true;
                });
        return nav;
    }

    /** Troca para a aba pedida e guarda a atual no histórico (o voltar retorna a ela). */
    public void abrirAba(int aba) {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.isStateSaved()) return;
        String tag = String.valueOf(aba);
        Fragment atual = fm.findFragmentById(R.id.conteudoAbas);
        if (atual != null && tag.equals(atual.getTag())) return;
        Fragment f = criarAba(aba);
        if (f == null) return;
        fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.conteudoAbas, f, tag)
                .addToBackStack(tag)
                .commit();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.hasExtra(EXTRA_ABA)) abrirAba(intent.getIntExtra(EXTRA_ABA, abaInicial()));
    }

    /** Deixa marcada no menu a aba que está na tela (inclusive depois de voltar). */
    private void marcarAbaAtual() {
        if (nav == null) return;
        Fragment atual = getSupportFragmentManager().findFragmentById(R.id.conteudoAbas);
        if (atual == null || atual.getTag() == null) return;
        try {
            MenuItem item = nav.getMenu().findItem(Integer.parseInt(atual.getTag()));
            if (item != null) item.setChecked(true);
        } catch (NumberFormatException ignorado) {
            // tag que não é id de aba: não mexe no menu
        }
    }
}
