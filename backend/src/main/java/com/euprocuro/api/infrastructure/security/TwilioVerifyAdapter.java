package com.euprocuro.api.infrastructure.security;

import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.domain.gateway.PhoneVerificationGateway;
import com.euprocuro.api.domain.model.PhoneVerificationChannel;

/**
 * Verificacao de telefone via Twilio Verify (v2). O Twilio gera, envia, expira e conta as
 * tentativas do codigo; aqui apenas disparamos o envio e conferimos o codigo digitado.
 *
 * <p>Quando {@code application.auth.phone-verification.provider} nao for {@code TWILIO} (ou as
 * credenciais nao estiverem configuradas), opera em modo de log: registra o envio e aceita o
 * codigo de bypass de desenvolvimento, mantendo o fluxo testavel localmente sem custo de SMS.
 */
@Component
public class TwilioVerifyAdapter implements PhoneVerificationGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(TwilioVerifyAdapter.class);
    private static final String TWILIO_PROVIDER = "TWILIO";

    private final RestTemplate restTemplate;
    private final String provider;
    private final String apiBaseUrl;
    private final String accountSid;
    private final String authToken;
    private final String verifyServiceSid;
    private final String devBypassCode;

    public TwilioVerifyAdapter(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${application.auth.phone-verification.provider:LOG}") String provider,
            @Value("${application.auth.phone-verification.twilio.api-base-url:https://verify.twilio.com/v2}") String apiBaseUrl,
            @Value("${application.auth.phone-verification.twilio.account-sid:}") String accountSid,
            @Value("${application.auth.phone-verification.twilio.auth-token:}") String authToken,
            @Value("${application.auth.phone-verification.twilio.verify-service-sid:}") String verifyServiceSid,
            @Value("${application.auth.phone-verification.dev-bypass-code:}") String devBypassCode
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.provider = provider;
        this.apiBaseUrl = apiBaseUrl;
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.verifyServiceSid = verifyServiceSid;
        this.devBypassCode = devBypassCode;
    }

    @Override
    public void startVerification(String phoneE164, PhoneVerificationChannel channel) {
        String twilioChannel = twilioChannel(channel);
        if (!isTwilioConfigured()) {
            LOGGER.info("[phone-verification:LOG] Enviaria codigo para {} via {}.{}",
                    phoneE164,
                    twilioChannel,
                    StringUtils.hasText(devBypassCode) ? " Codigo de bypass: " + devBypassCode : "");
            return;
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", phoneE164);
        body.add("Channel", twilioChannel);

        try {
            restTemplate.exchange(
                    apiBaseUrl + "/Services/" + verifyServiceSid + "/Verifications",
                    HttpMethod.POST,
                    new HttpEntity<>(body, formHeaders()),
                    Map.class
            );
        } catch (RestClientException exception) {
            LOGGER.warn("Falha ao enviar codigo de verificacao para {}: {}", phoneE164, exception.getMessage());
            throw new BusinessException("Nao foi possivel enviar o codigo por SMS. Tente novamente em instantes.");
        }
    }

    @Override
    public boolean checkVerification(String phoneE164, String code) {
        if (!StringUtils.hasText(code)) {
            return false;
        }
        if (!isTwilioConfigured()) {
            boolean approved = StringUtils.hasText(devBypassCode)
                    ? code.trim().equals(devBypassCode.trim())
                    : code.trim().matches("\\d{4,8}");
            LOGGER.info("[phone-verification:LOG] Conferindo codigo para {}: {}", phoneE164, approved ? "aprovado" : "recusado");
            return approved;
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", phoneE164);
        body.add("Code", code.trim());

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/Services/" + verifyServiceSid + "/VerificationCheck",
                    HttpMethod.POST,
                    new HttpEntity<>(body, formHeaders()),
                    Map.class
            );
            Map<?, ?> payload = response.getBody();
            return payload != null && "approved".equalsIgnoreCase(String.valueOf(payload.get("status")));
        } catch (HttpClientErrorException exception) {
            // 404/410: verificacao inexistente, expirada ou ja consumida -> codigo invalido.
            return false;
        } catch (RestClientException exception) {
            LOGGER.warn("Falha ao conferir codigo de verificacao para {}: {}", phoneE164, exception.getMessage());
            throw new BusinessException("Nao foi possivel validar o codigo agora. Tente novamente em instantes.");
        }
    }

    private boolean isTwilioConfigured() {
        return TWILIO_PROVIDER.equalsIgnoreCase(provider)
                && StringUtils.hasText(accountSid)
                && StringUtils.hasText(authToken)
                && StringUtils.hasText(verifyServiceSid);
    }

    private HttpHeaders formHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(accountSid, authToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    private String twilioChannel(PhoneVerificationChannel channel) {
        PhoneVerificationChannel resolved = channel == null ? PhoneVerificationChannel.SMS : channel;
        return resolved.name().toLowerCase(Locale.ROOT);
    }
}
