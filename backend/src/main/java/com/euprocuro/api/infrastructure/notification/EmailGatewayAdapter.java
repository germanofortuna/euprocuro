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
        return sendEmail(
                user,
                "Eu Procuro - redefinicao de senha",
                "Ola, " + user.getName() + "!\n\n"
                        + "Recebemos um pedido para redefinir sua senha.\n"
                        + "Use o link abaixo para continuar:\n"
                        + resetLink + "\n\n"
                        + "Se voce nao solicitou essa alteracao, ignore este e-mail.",
                "Link de redefinicao: " + resetLink
        );
    }

    @Override
    public boolean sendOfferReceivedEmail(UserProfile buyer, String interestTitle, String sellerName) {
        return sendEmail(
                buyer,
                "Eu Procuro - nova proposta recebida",
                "Ola, " + buyer.getName() + "!\n\n"
                        + sellerName + " enviou uma proposta para o seu interesse:\n"
                        + interestTitle + "\n\n"
                        + "Acesse a plataforma para conversar e negociar com seguranca.",
                "Nova proposta para: " + interestTitle
        );
    }

    @Override
    public boolean sendConversationMessageEmail(
            UserProfile recipient,
            String senderName,
            String interestTitle,
            String messagePreview
    ) {
        return sendEmail(
                recipient,
                "Eu Procuro - nova mensagem",
                "Ola, " + recipient.getName() + "!\n\n"
                        + senderName + " enviou uma nova mensagem sobre:\n"
                        + interestTitle + "\n\n"
                        + messagePreview + "\n\n"
                        + "Entre na plataforma para responder.",
                "Nova mensagem: " + messagePreview
        );
    }

    @Override
    public boolean sendPurchaseConfirmationEmail(UserProfile user, String productName, String paymentMethod) {
        return sendEmail(
                user,
                "Eu Procuro - compra confirmada",
                "Ola, " + user.getName() + "!\n\n"
                        + "Sua compra foi confirmada:\n"
                        + productName + "\n"
                        + "Forma de pagamento: " + paymentMethod + "\n\n"
                        + "Seu saldo ou plano ja esta disponivel na plataforma.",
                "Compra confirmada: " + productName
        );
    }

    @Override
    public boolean sendBoostActivatedEmail(UserProfile user, String interestTitle, String boostedUntil) {
        return sendEmail(
                user,
                "Eu Procuro - boost ativado",
                "Ola, " + user.getName() + "!\n\n"
                        + "O boost do seu interesse foi ativado:\n"
                        + interestTitle + "\n"
                        + "Destaque ativo ate: " + boostedUntil + "\n\n"
                        + "Agora ele ganha prioridade na busca e na home.",
                "Boost ativado ate: " + boostedUntil
        );
    }

    private boolean sendEmail(UserProfile user, String subject, String text, String fallbackLog) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            LOGGER.info("SMTP nao configurado. E-mail para {}: {}", user.getEmail(), fallbackLog);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            return true;
        } catch (Exception exception) {
            LOGGER.warn("Falha ao enviar e-mail para {}. Assunto: {}", user.getEmail(), subject);
            return false;
        }
    }
}
