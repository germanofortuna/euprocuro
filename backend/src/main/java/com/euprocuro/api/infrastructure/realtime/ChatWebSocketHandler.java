package com.euprocuro.api.infrastructure.realtime;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    public static final String USER_ID_ATTRIBUTE = "userId";

    private final ChatWebSocketSessionRegistry sessionRegistry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = currentUserId(session);
        if (!StringUtils.hasText(userId)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Usuario nao autenticado."));
            return;
        }

        sessionRegistry.register(userId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = currentUserId(session);
        if (StringUtils.hasText(userId)) {
            sessionRegistry.unregister(userId, session);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = currentUserId(session);
        if (StringUtils.hasText(userId)) {
            sessionRegistry.unregister(userId, session);
        }
        session.close(CloseStatus.SERVER_ERROR);
    }

    private String currentUserId(WebSocketSession session) {
        Object value = session.getAttributes().get(USER_ID_ATTRIBUTE);
        return value instanceof String ? (String) value : null;
    }
}
