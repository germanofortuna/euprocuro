package com.euprocuro.api.infrastructure.security;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.view.FacebookIdentityView;

@Component
public class FacebookIdentityService {

    private static final String DEBUG_TOKEN_URL = "https://graph.facebook.com/debug_token";
    private static final String USER_INFO_URL = "https://graph.facebook.com/me";

    private final RestTemplate restTemplate;

    @Value("${application.auth.facebook.app-id:${FACEBOOK_APP_ID:}}")
    private String appId;

    @Value("${application.auth.facebook.app-secret:${FACEBOOK_APP_SECRET:}}")
    private String appSecret;

    public FacebookIdentityService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public FacebookIdentityView verify(String accessToken) {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            throw new BusinessException("Login com Facebook indisponivel no momento.");
        }
        if (!StringUtils.hasText(accessToken)) {
            throw new BusinessException("Token do Facebook nao informado.");
        }

        String token = accessToken.trim();
        Map<?, ?> debug = fetchDebugToken(token);
        Map<?, ?> debugData = asMap(debug.get("data"));
        if (debugData == null) {
            throw new BusinessException("Nao foi possivel validar sua conta Facebook.");
        }

        boolean valid = Boolean.parseBoolean(text(debugData.get("is_valid")).toLowerCase(Locale.ROOT));
        if (!valid) {
            throw new BusinessException("Sua sessao do Facebook expirou. Tente novamente.");
        }
        if (!Objects.equals(text(debugData.get("app_id")), appId.trim())) {
            throw new BusinessException("Esta conta Facebook nao pertence a este aplicativo.");
        }

        Map<?, ?> userInfo = fetchUserInfo(token);
        String email = text(userInfo.get("email")).toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(email)) {
            throw new BusinessException("Nao recebemos o e-mail da sua conta Facebook.");
        }

        return FacebookIdentityView.builder()
                .subject(text(userInfo.get("id")))
                .email(email)
                .name(text(userInfo.get("name")))
                .emailVerified(true)
                .build();
    }

    private Map<?, ?> fetchDebugToken(String accessToken) {
        try {
            String appAccessToken = appId.trim() + "|" + appSecret.trim();
            URI uri = URI.create(DEBUG_TOKEN_URL
                    + "?input_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                    + "&access_token=" + URLEncoder.encode(appAccessToken, StandardCharsets.UTF_8));
            Map<?, ?> payload = restTemplate.getForObject(uri, Map.class);
            if (payload == null) {
                throw new BusinessException("Nao foi possivel validar sua conta Facebook.");
            }
            return payload;
        } catch (IllegalArgumentException | RestClientException exception) {
            throw new BusinessException("Nao foi possivel validar sua conta Facebook.");
        }
    }

    private Map<?, ?> fetchUserInfo(String accessToken) {
        try {
            URI uri = URI.create(USER_INFO_URL
                    + "?fields=id,name,email"
                    + "&access_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8));
            Map<?, ?> payload = restTemplate.getForObject(uri, Map.class);
            if (payload == null) {
                throw new BusinessException("Nao foi possivel validar sua conta Facebook.");
            }
            return payload;
        } catch (IllegalArgumentException | RestClientException exception) {
            throw new BusinessException("Nao foi possivel validar sua conta Facebook.");
        }
    }

    private Map<?, ?> asMap(Object value) {
        return value instanceof Map<?, ?> map ? map : null;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
