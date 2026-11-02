package com.example.renovai.view;

import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.widget.ListPopupWindow;
import com.example.renovai.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Sugestões pelo mesmo geocodificador usado para localizar as paradas no mapa. */
final class RotaEnderecoSugestoes {
    private final EditText campo;
    private final TextView aviso;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ListPopupWindow lista;
    private final TextWatcher observador;
    private int geracao;
    private boolean encerrado, preenchendo;
    private Address selecionado;
    private String textoSelecionado;

    RotaEnderecoSugestoes(EditText campo, TextView aviso) {
        this.campo = campo;
        this.aviso = aviso;
        lista = new ListPopupWindow(campo.getContext());
        lista.setAnchorView(campo);
        observador = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (preenchendo) return;
                selecionado = null;
                textoSelecionado = null;
                int pedido = ++geracao;
                main.removeCallbacksAndMessages(null);
                lista.dismiss();
                aviso.setText(R.string.rota_endereco_dica);
                String consulta = s.toString().trim();
                if (consulta.length() >= 4)
                    main.postDelayed(() -> buscar(consulta, pedido), 500);
            }
            public void afterTextChanged(Editable e) {}
        };
        campo.addTextChangedListener(observador);
    }

    private boolean atual(int pedido) {
        return !encerrado && pedido == geracao && campo.hasFocus() && campo.hasWindowFocus();
    }

    private void buscar(String consulta, int pedido) {
        if (!atual(pedido)) return;
        if (!Geocoder.isPresent()) {
            aviso.setText(R.string.rota_endereco_indisponivel);
            return;
        }
        aviso.setText(R.string.rota_endereco_buscando);
        try {
            new Geocoder(campo.getContext().getApplicationContext(), new Locale("pt", "BR"))
                    .getFromLocationName(consulta, 5, new Geocoder.GeocodeListener() {
                        public void onGeocode(List<Address> enderecos) {
                            main.post(() -> mostrar(enderecos, pedido));
                        }
                        public void onError(String mensagem) {
                            main.post(() -> {
                                if (atual(pedido)) aviso.setText(R.string.rota_endereco_indisponivel);
                            });
                        }
                    });
        } catch (RuntimeException e) {
            if (atual(pedido)) aviso.setText(R.string.rota_endereco_indisponivel);
        }
    }

    private void mostrar(List<Address> resultados, int pedido) {
        if (!atual(pedido)) return;
        List<Address> enderecos = new ArrayList<>();
        List<String> textos = new ArrayList<>();
        for (Address endereco : resultados) {
            if (!endereco.hasLatitude() || !endereco.hasLongitude()) continue;
            String texto = formatar(endereco);
            if (!texto.isEmpty() && texto.length() <= 255 && !textos.contains(texto)) {
                enderecos.add(endereco);
                textos.add(texto);
            }
        }
        aviso.setText(textos.isEmpty() ? R.string.rota_endereco_sem_resultados
                : R.string.rota_endereco_selecione);
        if (textos.isEmpty()) return;
        lista.setAdapter(new ArrayAdapter<>(campo.getContext(),
                android.R.layout.simple_list_item_1, textos));
        lista.setOnItemClickListener((parent, view, posicao, id) -> {
            ++geracao;
            selecionado = enderecos.get(posicao);
            textoSelecionado = textos.get(posicao);
            preenchendo = true;
            campo.setText(textoSelecionado);
            campo.setSelection(campo.length());
            preenchendo = false;
            lista.dismiss();
            aviso.setText(R.string.rota_endereco_selecionado);
        });
        lista.show();
    }

    static String formatar(Address endereco) {
        // O provedor já fornece a linha postal com rua, número, cidade, UF e CEP.
        String linha = endereco.getAddressLine(0);
        return normalizar(linha);
    }

    static String normalizar(String linha) {
        return linha == null ? "" : linha.trim().replaceAll("[\\s\\u00a0]+", " ");
    }

    Address selecionado() {
        return textoSelecionado != null && textoSelecionado.equals(campo.getText().toString())
                ? selecionado : null;
    }

    void fechar() {
        encerrado = true;
        ++geracao;
        main.removeCallbacksAndMessages(null);
        campo.removeTextChangedListener(observador);
        lista.dismiss();
    }
}
