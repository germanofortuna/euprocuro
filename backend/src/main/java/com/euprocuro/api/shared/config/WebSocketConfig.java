package com.euprocuro.api.shared.config;

import java.util.Arrays;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.euprocuro.api.infrastructure.realtime.ChatWebSocketAuthHandshakeInterceptor;
import com.euprocuro.api.infrastructure.realtime.ChatWebSocketHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ChatWebSocketAuthHandshakeInterceptor chatWebSocketAuthHandshakeInterceptor;

    @Value("${application.security.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    @Value("${application.security.default-allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String defaultAllowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(chatWebSocketAuthHandshakeInterceptor)
                .setAllowedOrigins(parseAllowedOrigins());
    }

    private String[] parseAllowedOrigins() {
        return Stream.concat(splitOrigins(defaultAllowedOrigins), splitOrigins(allowedOrigins))
                .distinct()
                .toArray(String[]::new);
    }

    private Stream<String> splitOrigins(String origins) {
        if (origins == null || origins.trim().isEmpty()) {
            return Stream.empty();
        }

        return Arrays.stream(origins.split(","))
                .map(String::trim)
                .map(value -> value.replaceAll("/+$", ""))
                .filter(value -> !value.isEmpty());
    }
}
