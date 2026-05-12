package com.euprocuro.api.infrastructure.notification;

import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.euprocuro.api.domain.gateway.EmailGateway;
import com.euprocuro.api.domain.model.UserProfile;

@Component
public class EmailGatewayAdapter implements EmailGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailGatewayAdapter.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final RestTemplate restTemplate;
    private final String fromEmail;
    private final String fromName;
    private final String provider;
    private final String mailerSendApiUrl;
    private final String mailerSendApiKey;
    private final String defaultTemplateId;
    private final String emailVerificationTemplateId;
    private final String passwordResetTemplateId;
    private final String offerReceivedTemplateId;
    private final String conversationMessageTemplateId;
    private final String purchaseConfirmationTemplateId;
    private final String boostActivatedTemplateId;
    private final String ombudsmanTemplateId;
    private final String appUrl;
    private final String termsUrl;
    private final String privacyUrl;
    private final String supportUrl;

    public EmailGatewayAdapter(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            RestTemplateBuilder restTemplateBuilder,
            @Value("${application.email.from:no-reply@euprocuro.local}") String fromEmail,
            @Value("${application.email.from-name:Eu Procuro}") String fromName,
            @Value("${application.email.provider:SMTP}") String provider,
            @Value("${application.email.mailersend.api-url:https://api.mailersend.com/v1/email}") String mailerSendApiUrl,
            @Value("${application.email.mailersend.api-key:}") String mailerSendApiKey,
            @Value("${application.email.mailersend.template-id.default:}") String defaultTemplateId,
            @Value("${application.email.mailersend.template-id.email-verification:}") String emailVerificationTemplateId,
            @Value("${application.email.mailersend.template-id.password-reset:}") String passwordResetTemplateId,
            @Value("${application.email.mailersend.template-id.offer-received:}") String offerReceivedTemplateId,
            @Value("${application.email.mailersend.template-id.conversation-message:}") String conversationMessageTemplateId,
            @Value("${application.email.mailersend.template-id.purchase-confirmation:}") String purchaseConfirmationTemplateId,
            @Value("${application.email.mailersend.template-id.boost-activated:}") String boostActivatedTemplateId,
            @Value("${application.email.mailersend.template-id.ombudsman:}") String ombudsmanTemplateId,
            @Value("${application.email.app-url:${application.auth.reset-base-url:http://localhost:5173}}") String appUrl,
            @Value("${application.email.terms-url:http://localhost:5173#termos-de-uso}") String termsUrl,
            @Value("${application.email.privacy-url:http://localhost:5173#politica-de-privacidade}") String privacyUrl,
            @Value("${application.email.support-url:mailto:suporte@euprocuro.com}") String supportUrl
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.restTemplate = restTemplateBuilder.build();
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.provider = provider;
        this.mailerSendApiUrl = mailerSendApiUrl;
        this.mailerSendApiKey = mailerSendApiKey;
        this.defaultTemplateId = defaultTemplateId;
        this.emailVerificationTemplateId = emailVerificationTemplateId;
        this.passwordResetTemplateId = passwordResetTemplateId;
        this.offerReceivedTemplateId = offerReceivedTemplateId;
        this.conversationMessageTemplateId = conversationMessageTemplateId;
        this.purchaseConfirmationTemplateId = purchaseConfirmationTemplateId;
        this.boostActivatedTemplateId = boostActivatedTemplateId;
        this.ombudsmanTemplateId = ombudsmanTemplateId;
        this.appUrl = appUrl;
        this.termsUrl = termsUrl;
        this.privacyUrl = privacyUrl;
        this.supportUrl = supportUrl;
    }

    @Override
    public boolean sendEmailVerificationEmail(UserProfile user, String verificationLink) {
        String subject = "Eu Procuro - confirme seu e-mail";
        String text = "Ola, " + user.getName() + "!\n\n"
                + "Confirme seu e-mail para aumentar a seguranca da sua conta.\n"
                + "Use o link abaixo para verificar seu cadastro:\n"
                + verificationLink + "\n\n"
                + "Se voce nao criou essa conta, ignore este e-mail.";
        return sendEmail(
                EmailKind.EMAIL_VERIFICATION,
                user,
                subject,
                text,
                "Link de verificacao: " + verificationLink,
                variables(
                        subject,
                        "Confirme seu cadastro no Eu Procuro.",
                        "Seguranca da conta",
                        "Confirme seu e-mail",
                        "Use o botao abaixo para verificar seu cadastro e liberar o acesso com seguranca.",
                        "Acao necessaria",
                        "A verificacao protege sua conta e ajuda a manter a plataforma confiavel.",
                        "Confirmar e-mail",
                        verificationLink,
                        "Se voce nao criou essa conta, ignore este e-mail."
                )
        );
    }

    @Override
    public boolean sendPasswordResetEmail(UserProfile user, String resetLink) {
        String subject = "Eu Procuro - redefinicao de senha";
        String text = "Ola, " + user.getName() + "!\n\n"
                + "Recebemos um pedido para redefinir sua senha.\n"
                + "Use o link abaixo para continuar:\n"
                + resetLink + "\n\n"
                + "Se voce nao solicitou essa alteracao, ignore este e-mail.";
        return sendEmail(
                EmailKind.PASSWORD_RESET,
                user,
                subject,
                text,
                "Link de redefinicao: " + resetLink,
                variables(
                        subject,
                        "Solicitacao de redefinicao de senha.",
                        "Acesso",
                        "Redefina sua senha",
                        "Recebemos um pedido para alterar a senha da sua conta.",
                        "Link seguro",
                        "Este link expira em breve. Use-o apenas se voce solicitou a redefinicao.",
                        "Redefinir senha",
                        resetLink,
                        "Se voce nao solicitou essa alteracao, ignore este e-mail."
                )
        );
    }

    @Override
    public boolean sendOfferReceivedEmail(UserProfile buyer, String interestTitle, String sellerName) {
        String subject = "Eu Procuro - nova proposta recebida";
        String text = "Ola, " + buyer.getName() + "!\n\n"
                + sellerName + " enviou uma proposta para o seu interesse:\n"
                + interestTitle + "\n\n"
                + "Acesse a plataforma para conversar e negociar com seguranca.";
        return sendEmail(
                EmailKind.OFFER_RECEIVED,
                buyer,
                subject,
                text,
                "Nova proposta para: " + interestTitle,
                variables(
                        subject,
                        "Voce recebeu uma nova proposta.",
                        "Nova proposta",
                        "Seu interesse recebeu uma proposta",
                        sellerName + " enviou uma proposta para um interesse publicado por voce.",
                        "Interesse",
                        interestTitle,
                        "Ver proposta",
                        appUrl,
                        "Negocie pela plataforma e evite compartilhar dados sensiveis fora do contexto da proposta."
                )
        );
    }

    @Override
    public boolean sendConversationMessageEmail(
            UserProfile recipient,
            String senderName,
            String interestTitle,
            String messagePreview
    ) {
        String subject = "Eu Procuro - nova mensagem";
        String text = "Ola, " + recipient.getName() + "!\n\n"
                + senderName + " enviou uma nova mensagem sobre:\n"
                + interestTitle + "\n\n"
                + messagePreview + "\n\n"
                + "Entre na plataforma para responder.";
        return sendEmail(
                EmailKind.CONVERSATION_MESSAGE,
                recipient,
                subject,
                text,
                "Nova mensagem: " + messagePreview,
                variables(
                        subject,
                        "Voce recebeu uma nova mensagem.",
                        "Mensagem",
                        "Nova mensagem recebida",
                        senderName + " enviou uma mensagem sobre " + interestTitle + ".",
                        "Previa",
                        messagePreview,
                        "Abrir conversa",
                        appUrl,
                        "Nunca informe senhas, codigos ou dados financeiros por mensagem."
                )
        );
    }

    @Override
    public boolean sendPurchaseConfirmationEmail(UserProfile user, String productName, String paymentMethod) {
        String subject = "Eu Procuro - compra confirmada";
        String text = "Ola, " + user.getName() + "!\n\n"
                + "Sua compra foi confirmada:\n"
                + productName + "\n"
                + "Forma de pagamento: " + paymentMethod + "\n\n"
                + "Seu saldo ou plano ja esta disponivel na plataforma.";
        return sendEmail(
                EmailKind.PURCHASE_CONFIRMATION,
                user,
                subject,
                text,
                "Compra confirmada: " + productName,
                variables(
                        subject,
                        "Pagamento confirmado no Eu Procuro.",
                        "Pagamento",
                        "Compra confirmada",
                        "Seu saldo ou plano ja esta disponivel na plataforma.",
                        "Produto",
                        productName + " - " + paymentMethod,
                        "Abrir plataforma",
                        appUrl,
                        "Se voce nao reconhece esta compra, entre em contato com o suporte."
                )
        );
    }

    @Override
    public boolean sendBoostActivatedEmail(UserProfile user, String interestTitle, String boostedUntil) {
        String subject = "Eu Procuro - boost ativado";
        String text = "Ola, " + user.getName() + "!\n\n"
                + "O boost do seu interesse foi ativado:\n"
                + interestTitle + "\n"
                + "Destaque ativo ate: " + boostedUntil + "\n\n"
                + "Agora ele ganha prioridade na busca e na home.";
        return sendEmail(
                EmailKind.BOOST_ACTIVATED,
                user,
                subject,
                text,
                "Boost ativado ate: " + boostedUntil,
                variables(
                        subject,
                        "Seu interesse agora esta em destaque.",
                        "Boost",
                        "Boost ativado",
                        "O boost do seu interesse foi ativado e ele ganhou prioridade na busca e na home.",
                        "Interesse",
                        interestTitle + " - destaque ativo ate " + boostedUntil,
                        "Ver interesse",
                        appUrl,
                        "Acompanhe as propostas recebidas pela plataforma."
                )
        );
    }

    @Override
    public boolean sendOmbudsmanConfirmationEmail(String name, String email, String protocol, String subjectText) {
        UserProfile recipient = emailRecipient(name, email);
        String subject = "Eu Procuro - manifestacao recebida";
        String text = "Ola, " + recipient.getName() + "!\n\n"
                + "Recebemos sua manifestacao na Ouvidoria.\n"
                + "Protocolo: " + protocol + "\n"
                + "Assunto: " + subjectText + "\n\n"
                + "Responderemos assim que a analise for concluida.";
        return sendEmail(
                EmailKind.OMBUDSMAN,
                recipient,
                subject,
                text,
                "Ouvidoria recebida. Protocolo: " + protocol,
                variables(
                        subject,
                        "Recebemos sua manifestacao na Ouvidoria.",
                        "Ouvidoria",
                        "Manifestacao recebida",
                        "Seu relato foi registrado e sera analisado pela equipe responsavel.",
                        "Protocolo",
                        protocol + " - " + subjectText,
                        "Abrir plataforma",
                        appUrl,
                        "Guarde o protocolo para futuras consultas."
                )
        );
    }

    @Override
    public boolean sendOmbudsmanResponseEmail(String name, String email, String protocol, String subjectText, String response) {
        UserProfile recipient = emailRecipient(name, email);
        String subject = "Eu Procuro - resposta da Ouvidoria";
        String text = "Ola, " + recipient.getName() + "!\n\n"
                + "Sua manifestacao recebeu uma resposta.\n"
                + "Protocolo: " + protocol + "\n"
                + "Assunto: " + subjectText + "\n\n"
                + response;
        return sendEmail(
                EmailKind.OMBUDSMAN,
                recipient,
                subject,
                text,
                "Resposta da Ouvidoria. Protocolo: " + protocol,
                variables(
                        subject,
                        "Sua manifestacao recebeu uma resposta.",
                        "Ouvidoria",
                        "Resposta da Ouvidoria",
                        response,
                        "Protocolo",
                        protocol + " - " + subjectText,
                        "Abrir plataforma",
                        appUrl,
                        "Esta resposta foi enviada pelo canal oficial da plataforma."
                )
        );
    }

    private boolean sendEmail(
            EmailKind kind,
            UserProfile user,
            String subject,
            String text,
            String fallbackLog,
            Map<String, Object> variables
    ) {
        if (isMailerSendApiProvider()) {
            return sendMailerSendTemplate(kind, user, subject, fallbackLog, variables);
        }
        return sendSmtpTextEmail(user, subject, text, fallbackLog);
    }

    private boolean sendSmtpTextEmail(UserProfile user, String subject, String text, String fallbackLog) {
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
            LOGGER.warn(
                    "Falha ao enviar e-mail para {}. Assunto: {}. Motivo: {}",
                    user.getEmail(),
                    subject,
                    exception.getMessage(),
                    exception
            );
            return false;
        }
    }

    private boolean sendMailerSendTemplate(
            EmailKind kind,
            UserProfile user,
            String subject,
            String fallbackLog,
            Map<String, Object> variables
    ) {
        String templateId = templateIdFor(kind);
        if (!StringUtils.hasText(mailerSendApiKey) || !StringUtils.hasText(templateId)) {
            LOGGER.warn("MailerSend API nao configurada. E-mail para {}: {}", user.getEmail(), fallbackLog);
            return false;
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("from", Map.of(
                    "email", fromEmail,
                    "name", fromName
            ));
            body.put("to", List.of(Map.of(
                    "email", user.getEmail(),
                    "name", user.getName()
            )));
            body.put("subject", subject);
            body.put("template_id", templateId);
            body.put("personalization", List.of(Map.of(
                    "email", user.getEmail(),
                    "data", withCommonVariables(user, variables)
            )));

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(mailerSendApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpStatus status = restTemplate.exchange(
                    mailerSendApiUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            ).getStatusCode();
            return status.is2xxSuccessful();
        } catch (RestClientException exception) {
            LOGGER.warn(
                    "Falha ao enviar e-mail via MailerSend API para {}. Assunto: {}. Motivo: {}",
                    user.getEmail(),
                    subject,
                    exception.getMessage(),
                    exception
            );
            return false;
        }
    }

    private Map<String, Object> variables(
            String subject,
            String preheader,
            String eyebrow,
            String headline,
            String bodyText,
            String cardLabel,
            String cardText,
            String ctaLabel,
            String ctaUrl,
            String securityNote
    ) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("subject", subject);
        variables.put("preheader", preheader);
        variables.put("eyebrow", eyebrow);
        variables.put("headline", headline);
        variables.put("body_text", bodyText);
        variables.put("card_label", cardLabel);
        variables.put("card_text", cardText);
        variables.put("cta_label", ctaLabel);
        variables.put("cta_url", ctaUrl);
        variables.put("security_note", securityNote);
        return variables;
    }

    private Map<String, Object> withCommonVariables(UserProfile user, Map<String, Object> variables) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("recipient_name", user.getName());
        data.put("app_url", appUrl);
        data.put("terms_url", termsUrl);
        data.put("privacy_url", privacyUrl);
        data.put("support_url", supportUrl);
        data.put("year", String.valueOf(Year.now().getValue()));
        data.putAll(variables);
        return data;
    }

    private boolean isMailerSendApiProvider() {
        return "MAILERSEND_API".equalsIgnoreCase(provider);
    }

    private String templateIdFor(EmailKind kind) {
        String templateId;
        switch (kind) {
            case EMAIL_VERIFICATION:
                templateId = emailVerificationTemplateId;
                break;
            case PASSWORD_RESET:
                templateId = passwordResetTemplateId;
                break;
            case OFFER_RECEIVED:
                templateId = offerReceivedTemplateId;
                break;
            case CONVERSATION_MESSAGE:
                templateId = conversationMessageTemplateId;
                break;
            case PURCHASE_CONFIRMATION:
                templateId = purchaseConfirmationTemplateId;
                break;
            case BOOST_ACTIVATED:
                templateId = boostActivatedTemplateId;
                break;
            case OMBUDSMAN:
                templateId = ombudsmanTemplateId;
                break;
            default:
                templateId = defaultTemplateId;
                break;
        }
        return StringUtils.hasText(templateId) ? templateId : defaultTemplateId;
    }

    private enum EmailKind {
        EMAIL_VERIFICATION,
        PASSWORD_RESET,
        OFFER_RECEIVED,
        CONVERSATION_MESSAGE,
        PURCHASE_CONFIRMATION,
        BOOST_ACTIVATED
        ,
        OMBUDSMAN
    }

    private UserProfile emailRecipient(String name, String email) {
        return UserProfile.builder()
                .name(StringUtils.hasText(name) ? name : "usuario")
                .email(email)
                .build();
    }
}
