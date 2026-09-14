package com.example.renovai;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class EmpresaActivity extends AppCompatActivity {

    private View menuFavoritas, menuCooperativas, menuHome, menuPedidos, menuPerfil;
    private View abaSelecionadaAtual = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_empresa);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int folgaExtraPx = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());

            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);

            View customMenu = findViewById(R.id.layout_custom_bottom_menu);
            if (customMenu != null) {
                customMenu.setPadding(
                        customMenu.getPaddingLeft(),
                        customMenu.getPaddingTop(),
                        customMenu.getPaddingRight(),
                        systemBars.bottom + folgaExtraPx
                );
            }
            return insets;
        });

        menuFavoritas = findViewById(R.id.menu_favoritas);
        menuCooperativas = findViewById(R.id.menu_cooperativas);
        menuHome = findViewById(R.id.menu_home);
        menuPedidos = findViewById(R.id.menu_pedidos);
        menuPerfil = findViewById(R.id.menu_perfil);

        menuFavoritas.setOnClickListener(v -> selecionarAba(menuFavoritas, R.id.container_favoritas, R.drawable.favoritos_empresa_icon));
        menuCooperativas.setOnClickListener(v -> selecionarAba(menuCooperativas, R.id.container_cooperativas, R.drawable.cooperativa_empresa_icon));
        menuHome.setOnClickListener(v -> selecionarAba(menuHome, R.id.container_home, R.drawable.home_empresa_icon));
        menuPedidos.setOnClickListener(v -> selecionarAba(menuPedidos, R.id.container_pedidos, R.drawable.pedidos_empresa_icon));
        menuPerfil.setOnClickListener(v -> selecionarAba(menuPerfil, R.id.container_perfil, R.drawable.perfil_empresa_icon));

        // Inicia na aba Home por padrão
        selecionarAba(menuHome, R.id.container_home, R.drawable.home_empresa_icon);
    }

    private void selecionarAba(View abaClicada, int containerId, int iconeResId) {
        if (abaSelecionadaAtual == abaClicada) return;

        // Reseta todas as abas
        resetarAba(menuFavoritas, R.id.container_favoritas, R.drawable.favoritos_empresa_icon);
        resetarAba(menuCooperativas, R.id.container_cooperativas, R.drawable.cooperativa_empresa_icon);
        resetarAba(menuHome, R.id.container_home, R.drawable.home_empresa_icon);
        resetarAba(menuPedidos, R.id.container_pedidos, R.drawable.pedidos_empresa_icon);
        resetarAba(menuPerfil, R.id.container_perfil, R.drawable.perfil_empresa_icon);

        // Destaca a aba clicada
        View container = abaClicada.findViewById(containerId);
        if (container != null) {
            container.setBackgroundResource(R.drawable.seletor_menu_background_empresa);
            ImageView img = (ImageView) ((android.view.ViewGroup) container).getChildAt(0);
            if (img != null) {
                img.setImageResource(iconeResId);
                img.setColorFilter(ContextCompat.getColor(this, R.color.seletor_menu_icon_empresa));
            }
        }

        TextView txt = (TextView) ((android.view.ViewGroup) abaClicada).getChildAt(1);
        if (txt != null) {
            txt.setTextColor(ColorStateList.valueOf(0xFFD4E699)); // Amarelo claro
        }

        abaSelecionadaAtual = abaClicada;
    }

    private void resetarAba(View aba, int containerId, int iconeResId) {
        if (aba == null) return;

        View container = aba.findViewById(containerId);
        if (container != null) {
            container.setBackground(null);
            ImageView img = (ImageView) ((android.view.ViewGroup) container).getChildAt(0);
            if (img != null) {
                img.setImageResource(iconeResId);
                img.setColorFilter(ContextCompat.getColor(this, android.R.color.white));
            }
        }

        TextView txt = (TextView) ((android.view.ViewGroup) aba).getChildAt(1);
        if (txt != null) {
            txt.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }
    }
}