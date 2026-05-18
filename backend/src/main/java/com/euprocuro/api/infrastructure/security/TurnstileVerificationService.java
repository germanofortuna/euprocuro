package com.euprocuro.api.infrastructure.security;

import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.service.OperationalCatalogService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class TurnstileVerificationService {
    private static final int MAX_TOKEN_LENGTH = 2048;
    private static final String SITEVERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private final RestTemplate restTemplate;
    private final OperationalCatalogService operationalCatalogService;

    @Value("${application.security.turnstile.enabled:true}")
    private boolean enabled;

    @Value("${application.security.turnstile.secret-key:${TURNSTILE_SECRET_KEY:}}")
    private String secretKey;

    public TurnstileVerificationService(
            RestTemplateBuilder restTemplateBuilder,
            OperationalCatalogService operationalCatalogService
    ) {
        this.operationalCatalogService = operationalCatalogService;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(4))
                .setReadTimeout(Duration.ofSeconds(8))
                .build();
    }

    public void verify(String token, String remoteIp) {
        if (!enabled || !operationalCatalogService.getFeatureFlags().isCaptchaEnabled() || !StringUtils.hasText(secretKey)) {
            return;
        }
        String normalizedToken = token == null ? "" : token.trim();
        if (!StringUtils.hasText(normalizedToken)) {
            throw new BusinessException("Confirme a verificacao de seguranca para continuar.");
        }
        if (normalizedToken.length() > MAX_TOKEN_LENGTH) {
            throw new BusinessException("A verificacao de seguranca expirou. Tente novamente.");
        }

        SiteverifyResponse response = requestSiteverify(normalizedToken, remoteIp);
        if (response == null || !response.isSuccess()) {
            throw new BusinessException("Nao foi possivel validar a verificacao de seguranca. Tente novamente.");
        }
    }

    private SiteverifyResponse requestSiteverify(String token, String remoteIp) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
        payload.add("secret", secretKey);
        payload.add("response", token);
        if (StringUtils.hasText(remoteIp)) {
            payload.add("remoteip", remoteIp);
        }

        try {
            return restTemplate.postForObject(
                    SITEVERIFY_URL,
                    new HttpEntity<>(payload, headers),
                    SiteverifyResponse.class
            );
        } catch (RestClientException exception) {
            throw new BusinessException("Nao foi possivel validar a verificacao de seguranca. Tente novamente.");
        }
    }

    @Data
    static class SiteverifyResponse {
        private boolean success;
        private String hostname;
        private String action;
        @JsonProperty("error-codes")
        private List<String> errorCodes;
    }
}
