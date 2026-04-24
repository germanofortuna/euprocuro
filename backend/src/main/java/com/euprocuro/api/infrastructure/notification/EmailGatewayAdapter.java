package com.euprocuro.api.infrastructure.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.EmailGateway;
import com.euprocuro.api.domain.model.UserProfile;

@Component
public class EmailGatewayAdapter implements EmailGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailGatewayAdapter.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromEmail;

    public EmailGatewayAdapter(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${application.email.from:no-reply@euprocuro.local}") String fromEmail
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.fromEmail = fromEmail;
    }

    @Override
    public boolean sendPasswordResetEmail(UserProfile user, String resetLink) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            LOGGER.info("SMTP nao configurado. Link de redefinicao para {}: {}", user.getEmail(), resetLink);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Eu Procuro - redefinicao de senha");
            message.setText(
                    "Ola, " + user.getName() + "!\n\n"
                            + "Recebemos um pedido para redefinir sua senha.\n"
                            + "Use o link abaixo para continuar:\n"
                            + resetLink + "\n\n"
                            + "Se voce nao solicitou essa alteracao, ignore este e-mail."
            );
            mailSender.send(message);
            return true;
        } catch (Exception exception) {
            LOGGER.warn("Falha ao enviar e-mail de redefinicao para {}. Link: {}", user.getEmail(), resetLink);
            return false;
        }
    }
}
