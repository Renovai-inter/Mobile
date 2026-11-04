package com.renovai.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renovai.api.exception.RegraDeNegocioException;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Envio de e-mails transacionais (ex.: código de redefinição de senha).
 *
 * <p>Dois caminhos, nesta ordem:
 *
 * <ol>
 *   <li><b>Google Apps Script</b> (gratuito, por HTTPS), ligado por padrão em {@link
 *       #SCRIPT_PADRAO} (pode ser trocado com MAIL_SCRIPT_URL, ou desligado com MAIL_SCRIPT_URL=-).
 *       É o caminho que funciona no plano gratuito do Render, que bloqueia as portas de SMTP
 *       (25, 465 e 587). O script recebe só o destinatário e o código e monta o e-mail com o
 *       modelo do Renovaí; não aceita assunto nem HTML de fora, então a URL pública não serve
 *       para mandar outros e-mails. O código do script fica em docs/email-apps-script.md.
 *   <li><b>SMTP</b> (Gmail com "senha de app"), para rodar local ou num servidor sem bloqueio.
 * </ol>
 *
 * <p>Variáveis do SMTP:
 *
 * <pre>
 * MAIL_USERNAME   conta que envia (padrão renovai.suporte@gmail.com)
 * MAIL_PASSWORD   senha de app do Gmail (16 letras; os espaços são ignorados)
 * MAIL_HOST       opcional, padrão smtp.gmail.com
 * MAIL_PORT       opcional, padrão 587 (STARTTLS)
 * MAIL_FROM_NAME  opcional, padrão "Renovaí"
 * </pre>
 */
@Service
public class EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    /** Web App "Renovai - Envio de e-mail" na conta renovai.suporte@gmail.com. */
    static final String SCRIPT_PADRAO =
            "https://script.google.com/macros/s/AKfycbxVmTLtFaRbOvzNkIaF13Ao82LjJJGtHstMQriLo19ruB4OzO_YCS36j5npXbbrVlmGPg/exec";

    private final String usuario;
    private final String senha;
    private final String host;
    private final int porta;
    private final String nomeRemetente;
    private final String scriptUrl;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http =
            HttpClient.newBuilder()
                    // O Apps Script responde com redirecionamento para o resultado.
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
    private volatile JavaMailSenderImpl sender;

    public EmailService(
            @Value("${mail.username:renovai.suporte@gmail.com}") String usuario,
            @Value("${mail.password:}") String senha,
            @Value("${mail.host:smtp.gmail.com}") String host,
            @Value("${mail.port:587}") int porta,
            @Value("${mail.from-name:Renovaí}") String nomeRemetente,
            @Value("${MAIL_SCRIPT_URL:" + SCRIPT_PADRAO + "}") String scriptUrl) {
        this.usuario = usuario == null ? "" : usuario.trim();
        this.senha = senha == null ? "" : senha.replace(" ", "");
        this.host = host;
        this.porta = porta;
        this.nomeRemetente = nomeRemetente;
        String url = scriptUrl == null ? "" : scriptUrl.trim();
        this.scriptUrl = url.equals("-") ? "" : url;
    }

    public boolean configurado() {
        return scriptConfigurado() || smtpConfigurado();
    }

    private boolean scriptConfigurado() {
        return !scriptUrl.isBlank();
    }

    private boolean smtpConfigurado() {
        return !usuario.isBlank() && !senha.isBlank();
    }

    /**
     * Envia o código de redefinição de senha. Pelo Apps Script só vão o destinatário, o código e a
     * validade (o script monta o e-mail); pelo SMTP, o modelo codigo-redefinicao.html.
     */
    public void enviarCodigo(String para, String codigo, int validadeMinutos) {
        if (!configurado()) {
            throw new RegraDeNegocioException(
                    "O envio de e-mail não está configurado no servidor. Avise o suporte do Renovaí.");
        }
        if (scriptConfigurado()) {
            Map<String, String> corpo = new LinkedHashMap<>();
            corpo.put("para", para);
            corpo.put("codigo", codigo);
            corpo.put("validade", String.valueOf(validadeMinutos));
            enviarPeloScript(para, corpo);
            return;
        }
        String html =
                modelo(
                        "codigo-redefinicao.html",
                        Map.of("CODIGO", codigo, "VALIDADE", String.valueOf(validadeMinutos)));
        String texto =
                "Seu código para redefinir a senha do Renovaí é "
                        + codigo
                        + ".\nEle vale por "
                        + validadeMinutos
                        + " minutos. Se não foi você que pediu, ignore este e-mail.";
        enviarPorSmtp(para, "Seu código do Renovaí: " + codigo, html, texto);
    }

    private void enviarPorSmtp(String para, String assunto, String html, String textoPuro) {
        try {
            JavaMailSenderImpl mail = sender();
            MimeMessage mensagem = mail.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, true, "UTF-8");
            helper.setFrom(new InternetAddress(usuario, nomeRemetente, "UTF-8"));
            helper.setTo(para);
            helper.setSubject(assunto);
            helper.setText(textoPuro, html);
            helper.addInline("nimo", new ClassPathResource("templates/email/assets/nimo.png"), "image/png");
            mail.send(mensagem);
        } catch (Exception e) {
            LOGGER.error("Falha ao enviar e-mail para {}", para, e);
            throw new RegraDeNegocioException(
                    "Não foi possível enviar o e-mail agora. Tente novamente em alguns minutos.");
        }
    }

    /** POST no Web App do Apps Script, que envia pelo Gmail da conta dona do script. */
    private void enviarPeloScript(String para, Map<String, String> corpo) {
        try {
            HttpRequest pedido =
                    HttpRequest.newBuilder(URI.create(scriptUrl))
                            .timeout(Duration.ofSeconds(30))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(corpo)))
                            .build();
            HttpResponse<String> resposta =
                    http.send(pedido, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode resultado = json.readTree(resposta.body());
            if (resposta.statusCode() != 200 || !resultado.path("ok").asBoolean(false)) {
                throw new IllegalStateException(
                        "Apps Script respondeu " + resposta.statusCode() + ": " + resultado.path("erro").asText(""));
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            LOGGER.error("Falha ao enviar e-mail para {} pelo Apps Script", para, e);
            throw new RegraDeNegocioException(
                    "Não foi possível enviar o e-mail agora. Tente novamente em alguns minutos.");
        }
    }

    /** Lê um modelo em src/main/resources/templates/email e troca cada {{CHAVE}} pelo valor. */
    public String modelo(String arquivo, Map<String, String> valores) {
        try (InputStream in = new ClassPathResource("templates/email/" + arquivo).getInputStream()) {
            String html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            for (Map.Entry<String, String> v : valores.entrySet()) {
                html = html.replace("{{" + v.getKey() + "}}", v.getValue());
            }
            return html;
        } catch (IOException e) {
            throw new IllegalStateException("Modelo de e-mail não encontrado: " + arquivo, e);
        }
    }

    private JavaMailSenderImpl sender() {
        JavaMailSenderImpl atual = sender;
        if (atual == null) {
            synchronized (this) {
                if (sender == null) {
                    JavaMailSenderImpl novo = new JavaMailSenderImpl();
                    novo.setHost(host);
                    novo.setPort(porta);
                    novo.setUsername(usuario);
                    novo.setPassword(senha);
                    novo.setDefaultEncoding("UTF-8");
                    Properties props = novo.getJavaMailProperties();
                    props.put("mail.transport.protocol", "smtp");
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.starttls.enable", "true");
                    props.put("mail.smtp.starttls.required", "true");
                    props.put("mail.smtp.connectiontimeout", "10000");
                    props.put("mail.smtp.timeout", "10000");
                    props.put("mail.smtp.writetimeout", "10000");
                    sender = novo;
                }
                atual = sender;
            }
        }
        return atual;
    }
}
