package com.example.renovai;

import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class EmpresaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_empresa);

        // =========================================================
        // BARRA DO SISTEMA
        // =========================================================

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {

                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            0
                    );

                    return insets;
                }
        );


        // =========================================================
        // MENU INFERIOR
        // =========================================================

        BottomNavigationView menu = findViewById(R.id.bottom_navigation_empresa);
        android.view.View conteudo = findViewById(R.id.conteudo_empresa);

        // Altura "visível" do menu (ícone + texto), igual em qualquer aparelho.
        // (72dp = mesmo valor definido em activity_empresa.xml, aqui só serve de base
        // pro cálculo da altura total abaixo)
        int alturaBaseMenuPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics());

        // Aparelhos com navegação por 3 botões reservam bem mais espaço do que
        // aparelhos com gesto. Em vez de espremer esse espaço DENTRO de uma altura
        // fixa (o que sumia com o menu em celulares com 3 botões), a altura do menu
        // cresce: 72dp de conteúdo + o que o sistema precisar embaixo.
        ViewCompat.setOnApplyWindowInsetsListener(
                menu,
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

                    ViewGroup.LayoutParams params = v.getLayoutParams();
                    params.height = alturaBaseMenuPx + systemBars.bottom;
                    v.setLayoutParams(params);

                    v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);

                    // O conteúdo da tela (Home, cards, etc.) precisa reservar a mesma
                    // altura total do menu, senão fica escondido atrás dele.
                    ViewGroup.MarginLayoutParams conteudoParams =
                            (ViewGroup.MarginLayoutParams) conteudo.getLayoutParams();
                    conteudoParams.bottomMargin = alturaBaseMenuPx + systemBars.bottom;
                    conteudo.setLayoutParams(conteudoParams);

                    return insets;
                }
        );


        // =========================================================
        // CLIQUE NOS ITENS
        // =========================================================

        menu.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_favoritas) {

                // Tela Favoritas
                return true;

            } else if (id == R.id.nav_cooperativas) {

                // Tela Cooperativas
                return true;

            } else if (id == R.id.nav_home) {

                // Tela Home
                return true;

            } else if (id == R.id.nav_pedidos) {

                // Tela Pedidos
                return true;

            } else if (id == R.id.nav_perfil) {

                // Tela Perfil
                return true;
            }

            return false;
        });


        // =========================================================
        // HOME SELECIONADA INICIALMENTE
        // =========================================================

        menu.setSelectedItemId(R.id.nav_home);
    }
}
