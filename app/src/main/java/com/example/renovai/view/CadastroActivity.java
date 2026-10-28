package com.example.renovai.view;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.renovai.R;
import com.example.renovai.SenhaVisivel;
import com.example.renovai.controller.AuthController;
import com.example.renovai.controller.FalhaCadastro;
import com.example.renovai.dto.response.CadastroEmpresaResponse;
import com.google.android.material.card.MaterialCardView;

/**
 * Tela única de cadastro (1.4). A EscolheActivity (1.3) informa em {@link #EXTRA_TIPO} se quem está
 * se cadastrando é uma empresa ou uma cooperativa; a tela troca os rótulos e, para cooperativa,
 * mostra o endereço em partes (CEP, rua, número...), como pede o POST /auth/cadastro-cooperativa.
 */
public class CadastroActivity extends AppCompatActivity {

    public static final String EXTRA_TIPO = "tipo";
    public static final String TIPO_EMPRESA = "EMPRESA";
    public static final String TIPO_COOPERATIVA = "COOPERATIVA";

    private final AuthController authController = new AuthController();
    private boolean cooperativa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);


        setContentView(R.layout.activity_cadastro);

        cooperativa = TIPO_COOPERATIVA.equals(getIntent().getStringExtra(EXTRA_TIPO));
        aplicarTipo();

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        MaterialCardView btnCadastrar = findViewById(R.id.btnCadastrar);
        SenhaVisivel.ativar(findViewById(R.id.edtSenha));
        SenhaVisivel.ativar(findViewById(R.id.edtConfirmarSenha));

        // Voltar para a escolha empresa/cooperativa
        btnVoltar.setOnClickListener(v -> finish());

        btnCadastrar.setOnClickListener(
                v -> {
                    btnCadastrar.setEnabled(false);
                    if (cooperativa) cadastrarCooperativa(btnCadastrar);
                    else cadastrarEmpresa(btnCadastrar);
                });
    }

    private void aplicarTipo() {
        String tipo = cooperativa ? "Cooperativa" : "Empresa";
        ((TextView) findViewById(R.id.tituloCadastro)).setText(cooperativa ? "Cadastre sua cooperativa" : "Cadastre sua empresa");
        ((TextView) findViewById(R.id.labelOrganizacao)).setText(tipo);
        ((EditText) findViewById(R.id.edtOrganizacao)).setHint("Nome da " + tipo.toLowerCase());
        findViewById(R.id.containerEnderecoCooperativa).setVisibility(cooperativa ? View.VISIBLE : View.GONE);
        findViewById(R.id.containerContatoCooperativa).setVisibility(cooperativa ? View.VISIBLE : View.GONE);
        findViewById(R.id.containerTelefone).setVisibility(cooperativa ? View.GONE : View.VISIBLE);
        findViewById(R.id.containerEnderecoEmpresa).setVisibility(cooperativa ? View.GONE : View.VISIBLE);
    }

    /** Valores aceitos pelo CHECK de cooperativas.contato_preferencial no banco. */
    private String contatoPreferencial() {
        int marcado = ((RadioGroup) findViewById(R.id.grupoContato)).getCheckedRadioButtonId();
        if (marcado == R.id.radioTelefone) return "TELEFONE";
        if (marcado == R.id.radioEmail) return "EMAIL";
        return "WHATSAPP";
    }

    private String texto(int id) {
        return ((EditText) findViewById(id)).getText().toString();
    }

    private void mostrarErro(MaterialCardView btnCadastrar, String mensagem) {
        // Fica no logcat também: o Toast some e não deixa rastro do motivo.
        Log.w("CadastroActivity", "Cadastro recusado: " + mensagem);
        btnCadastrar.setEnabled(true);
        Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show();
    }

    /** Pop-up para erros que precisam de explicação; os botões mudam conforme o tipo do erro. */
    private void mostrarFalha(MaterialCardView btnCadastrar, FalhaCadastro falha) {
        Log.w("CadastroActivity", "Cadastro recusado: " + falha.titulo + " - " + falha.mensagem);
        btnCadastrar.setEnabled(true);
        if (isFinishing() || isDestroyed()) return;

        View view = getLayoutInflater().inflate(R.layout.dialog_erro_cadastro, null);
        // cartão branco arredondado do pop-up padrão vem do tema (ThemeOverlay.Renovai.Dialogo)
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(view).create();

        ImageView icone = view.findViewById(R.id.imgErro);
        TextView dica = view.findViewById(R.id.txtDicaErro);
        TextView primario = view.findViewById(R.id.txtPrimarioErro);
        TextView secundario = view.findViewById(R.id.btnSecundarioErro);
        ((TextView) view.findViewById(R.id.txtTituloErro)).setText(falha.titulo);
        ((TextView) view.findViewById(R.id.txtMensagemErro)).setText(falha.mensagem);
        if (falha.dica != null) {
            dica.setText(falha.dica);
            dica.setVisibility(View.VISIBLE);
        }

        Runnable acaoPrimaria = dialog::dismiss;
        Runnable acaoSecundaria = null;
        switch (falha.tipo) {
            case CONTA_EXISTENTE:
                icone.setImageResource(R.drawable.ic_erro_conta);
                primario.setText("Ir para o login");
                acaoPrimaria = this::irParaLogin;
                secundario.setText("Usar outro e-mail");
                acaoSecundaria = () -> focar(R.id.edtEmail);
                break;
            case DADOS_EM_USO:
                icone.setImageResource(R.drawable.ic_erro_conta);
                primario.setText("Revisar meus dados");
                acaoPrimaria = () -> focar(R.id.edtCpf);
                secundario.setText("Já tenho conta: entrar");
                acaoSecundaria = this::irParaLogin;
                break;
            case CONEXAO:
            case SERVIDOR:
                if (falha.tipo == FalhaCadastro.Tipo.CONEXAO) icone.setImageResource(R.drawable.ic_erro_conexao);
                primario.setText("Tentar novamente");
                acaoPrimaria = btnCadastrar::performClick;
                secundario.setText("Fechar");
                acaoSecundaria = () -> {};
                break;
            default:
                primario.setText("Entendi");
        }

        Runnable primariaFinal = acaoPrimaria;
        view.findViewById(R.id.btnPrimarioErro).setOnClickListener(v -> {
            dialog.dismiss();
            primariaFinal.run();
        });
        if (acaoSecundaria != null) {
            Runnable secundariaFinal = acaoSecundaria;
            secundario.setVisibility(View.VISIBLE);
            secundario.setOnClickListener(v -> {
                dialog.dismiss();
                secundariaFinal.run();
            });
        }
        dialog.show();
    }

    private void focar(int id) {
        EditText campo = findViewById(id);
        campo.requestFocus();
        campo.selectAll();
    }

    private void irParaLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void cadastrarEmpresa(MaterialCardView btnCadastrar) {
        authController.cadastrarEmpresa(
                texto(R.id.edtNome),
                texto(R.id.edtCpf),
                texto(R.id.edtEmail),
                texto(R.id.edtTelefone),
                texto(R.id.edtOrganizacao),
                texto(R.id.edtCnpj),
                texto(R.id.edtEndereco),
                texto(R.id.edtSenha),
                texto(R.id.edtConfirmarSenha),
                new AuthController.CadastroEmpresaCallback() {
                    @Override
                    public void onSuccess(CadastroEmpresaResponse resposta) {
                        // Já vem com token: entra direto na área da Empresa, sem pedir login de novo.
                        com.example.renovai.SessionManager.salvarSessao(
                                resposta.getToken(), resposta.getEmail(), resposta.getRole());
                        com.example.renovai.SessionManager.salvarEmpresaId(resposta.getEmpresaId());
                        com.example.renovai.GestorCache.limpar();
                        Toast.makeText(CadastroActivity.this, "Empresa cadastrada com sucesso!", Toast.LENGTH_SHORT).show();
                        com.example.renovai.EmpresaData.precarregar(
                                () -> runOnUiThread(() -> {
                                    Intent intent = new Intent(CadastroActivity.this, EmpresaHomeActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                }));
                    }

                    @Override
                    public void onErro(String mensagem) {
                        mostrarErro(btnCadastrar, mensagem);
                    }

                    @Override
                    public void onFalha(FalhaCadastro falha) {
                        mostrarFalha(btnCadastrar, falha);
                    }
                });
    }

    private void cadastrarCooperativa(MaterialCardView btnCadastrar) {
        authController.cadastrarCooperativa(
                texto(R.id.edtNome),
                texto(R.id.edtCpf),
                texto(R.id.edtEmail),
                contatoPreferencial(),
                texto(R.id.edtOrganizacao),
                texto(R.id.edtCnpj),
                texto(R.id.edtCep),
                texto(R.id.edtLogradouro),
                texto(R.id.edtNumero),
                texto(R.id.edtComplemento),
                texto(R.id.edtBairro),
                texto(R.id.edtCidade),
                texto(R.id.edtSenha),
                texto(R.id.edtConfirmarSenha),
                new AuthController.CadastroCooperativaCallback() {
                    @Override
                    public void onSuccess() {
                        // A API não devolve token no cadastro de cooperativa: segue para o login.
                        Toast.makeText(CadastroActivity.this, "Cooperativa cadastrada! Entre com seu e-mail e senha.", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(CadastroActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }

                    @Override
                    public void onErro(String mensagem) {
                        mostrarErro(btnCadastrar, mensagem);
                    }

                    @Override
                    public void onFalha(FalhaCadastro falha) {
                        mostrarFalha(btnCadastrar, falha);
                    }
                });
    }
}
