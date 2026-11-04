package com.renovai.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

class EmailVisualTest {
    @Test void templatePreservaCodigoValidadeEIdentidadeDaReferencia() throws Exception {
        EmailService service = new EmailService("", "", "smtp.gmail.com", 587, "Renovai", "-");
        String html = service.modelo("codigo-redefinicao.html", Map.of("CODIGO", "012345", "VALIDADE", "7"));
        assertThat(html).contains("012345", "7 minutos", "cid:nimo", "#882B4E", "#518F5D", "@media")
                .doesNotContain("{{", "482 916", "data:image", "Confirme seu e-mail");
        assertThat(new ClassPathResource("templates/email/assets/nimo.png").exists()).isTrue();
    }

    @Test void smtpMantemDestinatarioAlternativaTextoEMascoteInlineSemEnvioReal() throws Exception {
        class Captura extends JavaMailSenderImpl {
            MimeMessage enviada;
            @Override public void send(MimeMessage mensagem) { enviada = mensagem; }
        }
        Captura sender = new Captura();
        EmailService service = new EmailService("teste@example.invalid", "senha-so-de-teste", "smtp.gmail.com", 587, "Renovai", "-");
        ReflectionTestUtils.setField(service, "sender", sender);
        service.enviarCodigo("destino@example.invalid", "012345", 7);
        assertThat(sender.enviada).isNotNull();
        assertThat(sender.enviada.getAllRecipients()[0].toString()).isEqualTo("destino@example.invalid");
        assertThat(sender.enviada.getSubject()).contains("012345");
        sender.enviada.saveChanges();
        StringBuilder tipos = new StringBuilder();
        StringBuilder textos = new StringBuilder();
        percorrer(sender.enviada.getContent(), tipos, textos);
        assertThat(tipos.toString()).contains("text/plain", "text/html", "image/png", "<nimo>");
        assertThat(textos.toString()).contains("012345", "7 minutos");
    }

    private void percorrer(Object content, StringBuilder tipos, StringBuilder textos) throws Exception {
        if (content instanceof Multipart parts) {
            for (int i = 0; i < parts.getCount(); i++) {
                BodyPart part = parts.getBodyPart(i);
                tipos.append(part.getContentType());
                String[] cid = part.getHeader("Content-ID");
                if (cid != null) tipos.append(String.join("", cid));
                percorrer(part.getContent(), tipos, textos);
            }
        } else if (content instanceof String text) textos.append(text);
    }
}
