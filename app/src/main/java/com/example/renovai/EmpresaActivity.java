package com.example.renovai;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.renovai.dto.response.EmpresaResponse;
import com.example.renovai.view.HomeFragment;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmpresaActivity extends AppCompatActivity {

    private TextView txtNomeEmpresa, txtIniciaisEmpresa;
    private ImageView imgFotoEmpresa;

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
        // CABEÇALHO (nome + foto da empresa)
        // =========================================================

        txtNomeEmpresa = findViewById(R.id.txtNomeEmpresa);
        txtIniciaisEmpresa = findViewById(R.id.txtIniciaisEmpresa);
        imgFotoEmpresa = findViewById(R.id.imgFotoEmpresa);
        View btnPerfilHeader = findViewById(R.id.btnPerfilHeader);

        BottomNavigationView menu = findViewById(R.id.bottom_navigation_empresa);

        // Clicar na foto do cabeçalho leva direto pra aba Perfil.
        btnPerfilHeader.setOnClickListener(v -> menu.setSelectedItemId(R.id.nav_perfil));

        carregarDadosEmpresa();


        // =========================================================
        // MENU INFERIOR
        // =========================================================

        View conteudo = findViewById(R.id.conteudo_empresa);

        int alturaBaseMenuPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics());

        ViewCompat.setOnApplyWindowInsetsListener(
                menu,
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

                    ViewGroup.LayoutParams params = v.getLayoutParams();
                    params.height = alturaBaseMenuPx + systemBars.bottom;
                    v.setLayoutParams(params);

                    v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);

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

                trocarFragment(new HomeFragment());
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

        if (savedInstanceState == null) {
            trocarFragment(new HomeFragment());
        }
    }

    private void trocarFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.conteudo_empresa, fragment)
                .commit();
    }

    private void carregarDadosEmpresa() {
        String empresaId = SessionManager.getEmpresaId();
        if (empresaId == null || empresaId.trim().isEmpty()) {
            return; // sessão sem empresa vinculada; o HomeFragment já tenta resolver e avisa
        }

        EmpresaApiService service = ApiClient.createService(EmpresaApiService.class);
        service.buscarPorId(empresaId).enqueue(new Callback<EmpresaResponse>() {
            @Override
            public void onResponse(Call<EmpresaResponse> call, Response<EmpresaResponse> response) {
                if (isFinishing() || !response.isSuccessful() || response.body() == null) return;
                preencherCabecalho(response.body());
            }

            @Override
            public void onFailure(Call<EmpresaResponse> call, Throwable t) {
                // Mantém os valores padrão do layout; a Home segue funcionando normalmente.
            }
        });
    }

    private void preencherCabecalho(EmpresaResponse empresa) {
        String nome = empresa.getNome() != null ? empresa.getNome() : "Empresa";
        txtNomeEmpresa.setText(nome);

        String imagemUrl = empresa.getImagemUrl();
        if (imagemUrl != null && !imagemUrl.trim().isEmpty()) {
            txtIniciaisEmpresa.setVisibility(View.GONE);
            Glide.with(this).load(imagemUrl).circleCrop().into(imgFotoEmpresa);
        } else {
            txtIniciaisEmpresa.setVisibility(View.VISIBLE);
            txtIniciaisEmpresa.setText(gerarIniciais(nome));
        }
    }

    private String gerarIniciais(String nome) {
        String[] partes = nome.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, partes.length); i++) {
            if (!partes[i].isEmpty()) sb.append(Character.toUpperCase(partes[i].charAt(0)));
        }
        return sb.length() > 0 ? sb.toString() : "--";
    }
}
