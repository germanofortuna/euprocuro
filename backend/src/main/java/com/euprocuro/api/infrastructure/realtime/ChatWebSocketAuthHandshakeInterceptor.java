package com.euprocuro.api.infrastructure.realtime;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.euprocuro.api.application.usecase.AuthUseCase;
import com.euprocuro.api.domain.model.UserProfile;

@Component
public class ChatWebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthUseCase authUseCase;
    private final String cookieName;

    public ChatWebSocketAuthHandshakeInterceptor(
            AuthUseCase authUseCase,
            @Value("${application.auth.cookie.name:EU_PROCURO_SESSION}") String cookieName
    ) {
        this.authUseCase = authUseCase;
        this.cookieName = cookieName;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        try {
            return resolveToken(request)
                    .map(authUseCase::requireAuthenticatedUser)
                    .map(UserProfile::getId)
                    .filter(StringUtils::hasText)
                    .map(userId -> {
                        attributes.put(ChatWebSocketHandler.USER_ID_ATTRIBUTE, userId);
                        return true;
                    })
                    .orElse(false);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // No-op.
    }

    private Optional<String> resolveToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()).trim());
        }

        String queryToken = resolveQueryParam(request, "token");
        if (StringUtils.hasText(queryToken)) {
            return Optional.of(queryToken);
        }

        return request.getHeaders().getOrEmpty(HttpHeaders.COOKIE)
                .stream()
                .flatMap(headerValue -> Arrays.stream(headerValue.split(";")))
                .map(String::trim)
                .map(cookie -> cookie.split("=", 2))
                .filter(parts -> parts.length == 2 && cookieName.equals(parts[0]))
                .map(parts -> parts[1])
                .filter(StringUtils::hasText)
                .findFirst();
    }

    private String resolveQueryParam(ServerHttpRequest request, String name) {
        String query = request.getURI().getRawQuery();
        if (!StringUtils.hasText(query)) {
            return null;
        }

        return Arrays.stream(query.split("&"))
                .map(value -> value.split("=", 2))
                .filter(parts -> parts.length == 2 && name.equals(decode(parts[0])))
                .map(parts -> decode(parts[1]))
                .findFirst()
                .orElse(null);
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
