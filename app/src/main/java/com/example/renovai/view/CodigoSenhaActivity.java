package com.example.renovai.view;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.renovai.ApiClient;
import com.example.renovai.AuthApiService;
import com.example.renovai.EmpresaData;
import com.example.renovai.R;
import com.example.renovai.dto.request.EsqueciSenhaRequest;
import com.example.renovai.dto.request.VerificarCodigoRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tela 1.7.1 — Código de verificação: a pessoa digita o código de 6 dígitos que a API enviou por
 * e-mail (remetente renovai.suporte@gmail.com) e segue para a 1.7.2 (nova senha).
 */
public class CodigoSenhaActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "email";

    private static final int TAMANHO_CODIGO = 6;
    private static final long ESPERA_REENVIO_MS = 60_000;
    private static final int[] IDS_DIGITOS = {
        R.id.edtDigito1, R.id.edtDigito2, R.id.edtDigito3,
        R.id.edtDigito4, R.id.edtDigito5, R.id.edtDigito6
    };

    private final EditText[] digitos = new EditText[TAMANHO_CODIGO];
    private String email;
    private TextView btnReenviar;
    private CountDownTimer timerReenvio;
    /** Evita que o TextWatcher reaja aos setText feitos pelo próprio código. */
    private boolean preenchendo;
    /** Evita conferir o mesmo código duas vezes (ex.: colar + tocar em Continuar). */
    private boolean verificando;

    private final ActivityResultLauncher<Intent> novaSenha =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    r -> {
                        if (r.getResultCode() == RecuperarSenhaActivity.RESULT_CODIGO_INVALIDO) {
                            String erro = r.getData() != null ? r.getData().getStringExtra(RecuperarSenhaActivity.EXTRA_ERRO) : null;
                            limpar();
                            mostrarErro(erro != null ? erro : "Código inválido ou expirado. Solicite um novo código.");
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        getWindow().setStatusBarColor(Color.parseColor("#519059"));
        setContentView(R.layout.activity_codigo_senha);

        email = getIntent().getStringExtra(EXTRA_EMAIL);
        if (email == null || email.isEmpty()) {
            startActivity(new Intent(this, EsqueciSenhaActivity.class));
            finish();
            return;
        }

        com.example.renovai.TelaComTeclado.ajustar(findViewById(R.id.telaCodigo));
        // E-mail em negrito e numa linha só, para não quebrar no meio do endereço
        ((TextView) findViewById(R.id.txtInstrucao)).setText(
                androidx.core.text.HtmlCompat.fromHtml(
                        "Enviamos um código de 6 dígitos para<br><b>" + android.text.TextUtils.htmlEncode(email)
                                + "</b><br><br>Ele vale por 15 minutos. Se não encontrar, confira a caixa de"
                                + " spam (remetente renovai.suporte@gmail.com).",
                        androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY));

        for (int i = 0; i < TAMANHO_CODIGO; i++) digitos[i] = findViewById(IDS_DIGITOS[i]);
        configurarDigitos();

        btnReenviar = findViewById(R.id.btnReenviar);
        btnReenviar.setOnClickListener(v -> reenviar());
        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        findViewById(R.id.btnContinuar).setOnClickListener(v -> continuar());

        iniciarEsperaReenvio();
        digitos[0].requestFocus();
        digitos[0].post(() -> getSystemService(InputMethodManager.class).showSoftInput(digitos[0], InputMethodManager.SHOW_IMPLICIT));
    }

    @Override
    protected void onDestroy() {
        if (timerReenvio != null) timerReenvio.cancel();
        super.onDestroy();
    }

    private void configurarDigitos() {
        for (int i = 0; i < TAMANHO_CODIGO; i++) {
            final int pos = i;
            // Ao focar, o dígito fica selecionado: digitar por cima substitui em vez de somar
            digitos[i].setSelectAllOnFocus(true);
            digitos[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (preenchendo) return;
                    String numeros = s.toString().replaceAll("\\D", "");
                    esconderErro();

                    if (numeros.length() >= TAMANHO_CODIGO) {
                        // Código colado inteiro em qualquer caixinha
                        preencher(numeros.substring(0, TAMANHO_CODIGO));
                        continuar();
                        return;
                    }
                    if (numeros.length() > 1 || !numeros.equals(s.toString())) {
                        // Digitou por cima de um dígito já preenchido: fica só o último
                        preenchendo = true;
                        digitos[pos].setText(numeros.isEmpty() ? "" : numeros.substring(numeros.length() - 1));
                        preenchendo = false;
                        numeros = digitos[pos].getText().toString();
                    }
                    if (numeros.length() == 1) {
                        if (pos < TAMANHO_CODIGO - 1) digitos[pos + 1].requestFocus();
                        else if (codigo().length() == TAMANHO_CODIGO) continuar();
                    }
                }
            });
            // Apagar numa caixinha vazia volta para a anterior
            digitos[i].setOnKeyListener((v, keyCode, e) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && e.getAction() == KeyEvent.ACTION_DOWN
                        && digitos[pos].getText().length() == 0 && pos > 0) {
                    digitos[pos - 1].setText("");
                    digitos[pos - 1].requestFocus();
                    return true;
                }
                return false;
            });
        }
    }

    private void preencher(String numeros) {
        preenchendo = true;
        for (int i = 0; i < TAMANHO_CODIGO; i++) {
            digitos[i].setText(i < numeros.length() ? String.valueOf(numeros.charAt(i)) : "");
        }
        preenchendo = false;
        digitos[Math.min(numeros.length(), TAMANHO_CODIGO - 1)].requestFocus();
    }

    private void limpar() {
        preencher("");
    }

    private String codigo() {
        StringBuilder sb = new StringBuilder();
        for (EditText d : digitos) sb.append(d.getText().toString().trim());
        return sb.toString();
    }

    /** Confere o código na API (sem gastá-lo) antes de liberar a tela de nova senha. */
    private void continuar() {
        if (verificando) return;
        String codigo = codigo();
        if (codigo.length() != TAMANHO_CODIGO) {
            mostrarErro("Digite os 6 dígitos do código.");
            return;
        }
        verificando = true;
        View prog = findViewById(R.id.progressoCodigo);
        View btn = findViewById(R.id.btnContinuar);
        prog.setVisibility(View.VISIBLE);
        btn.setEnabled(false);

        AuthApiService api = ApiClient.createService(AuthApiService.class);
        api.verificarCodigo(new VerificarCodigoRequest(email, codigo)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> c, Response<Void> r) {
                verificando = false;
                prog.setVisibility(View.GONE);
                btn.setEnabled(true);
                // 404: API publicada sem o /verificar-codigo — segue e o /redefinir-senha confere o código
                if (r.isSuccessful() || r.code() == 404) {
                    abrirNovaSenha(codigo);
                } else if (r.code() == 400 || r.code() == 422) {
                    limpar();
                    mostrarErro(EmpresaData.erro(r));
                } else {
                    mostrarErro("Não foi possível conferir o código (erro " + r.code() + ").");
                }
            }
            @Override
            public void onFailure(Call<Void> c, Throwable t) {
                verificando = false;
                prog.setVisibility(View.GONE);
                btn.setEnabled(true);
                mostrarErro("Sem conexão com o servidor.");
            }
        });
    }

    private void abrirNovaSenha(String codigo) {
        Intent intent = new Intent(this, RecuperarSenhaActivity.class);
        intent.putExtra(RecuperarSenhaActivity.EXTRA_EMAIL, email);
        intent.putExtra(RecuperarSenhaActivity.EXTRA_CODIGO, codigo);
        novaSenha.launch(intent);
    }

    private void reenviar() {
        View prog = findViewById(R.id.progressoCodigo);
        prog.setVisibility(View.VISIBLE);
        btnReenviar.setEnabled(false);

        AuthApiService api = ApiClient.createService(AuthApiService.class);
        api.esqueciSenha(new EsqueciSenhaRequest(email)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> c, Response<Void> r) {
                prog.setVisibility(View.GONE);
                if (r.isSuccessful()) {
                    Toast.makeText(CodigoSenhaActivity.this, "Enviamos um novo código para " + email + ".", Toast.LENGTH_LONG).show();
                    limpar();
                    esconderErro();
                    iniciarEsperaReenvio();
                } else {
                    btnReenviar.setEnabled(true);
                    Toast.makeText(CodigoSenhaActivity.this, EmpresaData.erro(r), Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<Void> c, Throwable t) {
                prog.setVisibility(View.GONE);
                btnReenviar.setEnabled(true);
                Toast.makeText(CodigoSenhaActivity.this, "Sem conexão com o servidor.", Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Bloqueia o "Reenviar código" por 60s, para a pessoa não disparar vários e-mails seguidos. */
    private void iniciarEsperaReenvio() {
        if (timerReenvio != null) timerReenvio.cancel();
        btnReenviar.setEnabled(false);
        btnReenviar.setTextColor(Color.parseColor("#9CA3AF"));
        timerReenvio = new CountDownTimer(ESPERA_REENVIO_MS, 1000) {
            @Override
            public void onTick(long restante) {
                btnReenviar.setText("Reenviar código em " + (restante / 1000 + 1) + "s");
            }
            @Override
            public void onFinish() {
                btnReenviar.setText("Reenviar código");
                btnReenviar.setTextColor(Color.parseColor("#519059"));
                btnReenviar.setEnabled(true);
            }
        }.start();
    }

    private void mostrarErro(String mensagem) {
        TextView erro = findViewById(R.id.txtErroCodigo);
        erro.setText(mensagem);
        erro.setVisibility(View.VISIBLE);
    }

    private void esconderErro() {
        findViewById(R.id.txtErroCodigo).setVisibility(View.GONE);
    }
}
