package com.euprocuro.api.infrastructure.security;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.view.GoogleIdentityView;

@Component
public class GoogleIdentityService {

    private static final String TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo?access_token=";
    private static final String USER_INFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";

    private final RestTemplate restTemplate;

    @Value("${application.auth.google.client-id:${GOOGLE_CLIENT_ID:}}")
    private String clientId;

    public GoogleIdentityService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public GoogleIdentityView verify(String accessToken) {
        if (!StringUtils.hasText(clientId)) {
            throw new BusinessException("Login com Google indisponivel no momento.");
        }
        if (!StringUtils.hasText(accessToken)) {
            throw new BusinessException("Token do Google nao informado.");
        }

        String token = accessToken.trim();
        Map<?, ?> tokenInfo = fetchTokenInfo(token);
        String audience = text(tokenInfo.get("aud"));
        if (!Objects.equals(audience, clientId.trim())) {
            throw new BusinessException("Esta conta Google nao pertence a este aplicativo.");
        }

        Map<?, ?> userInfo = fetchUserInfo(token);
        String email = text(userInfo.get("email")).toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(email)) {
            throw new BusinessException("Nao recebemos o e-mail da sua conta Google.");
        }

        boolean emailVerified = Boolean.parseBoolean(text(userInfo.get("email_verified")).toLowerCase(Locale.ROOT));
        if (!emailVerified) {
            throw new BusinessException("Use uma conta Google com e-mail verificado.");
        }

        return GoogleIdentityView.builder()
                .subject(text(userInfo.get("sub")))
                .email(email)
                .name(text(userInfo.get("name")))
                .emailVerified(true)
                .build();
    }

    private Map<?, ?> fetchTokenInfo(String accessToken) {
        try {
            URI uri = URI.create(TOKEN_INFO_URL + URLEncoder.encode(accessToken, StandardCharsets.UTF_8));
            Map<?, ?> payload = restTemplate.getForObject(uri, Map.class);
            if (payload == null) {
                throw new BusinessException("Nao foi possivel validar sua conta Google.");
            }
            return payload;
        } catch (IllegalArgumentException | RestClientException exception) {
            throw new BusinessException("Nao foi possivel validar sua conta Google.");
        }
    }

    private Map<?, ?> fetchUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            ResponseEntity<Map> response = restTemplate.exchange(
                    URI.create(USER_INFO_URL),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            Map<?, ?> payload = response.getBody();
            if (payload == null) {
                throw new BusinessException("Nao foi possivel validar sua conta Google.");
            }
            return payload;
        } catch (RestClientException exception) {
            throw new BusinessException("Nao foi possivel validar sua conta Google.");
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
