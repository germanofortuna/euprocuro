package com.euprocuro.api.infrastructure.realtime;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class ChatWebSocketSessionRegistry {

    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionsByUserId = new ConcurrentHashMap<>();

    public void register(String userId, WebSocketSession session) {
        sessionsByUserId
                .computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    public void unregister(String userId, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByUserId.get(userId);
        if (sessions == null) {
            return;
        }

        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByUserId.remove(userId, sessions);
        }
    }

    public void sendToUser(String userId, String payload) {
        Set<WebSocketSession> sessions = sessionsByUserId.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        TextMessage message = new TextMessage(payload);
        sessions.removeIf(session -> !session.isOpen());
        sessions.forEach(session -> send(session, message));
    }

    private void send(WebSocketSession session, TextMessage message) {
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (IOException exception) {
            try {
                session.close();
            } catch (IOException ignored) {
                // Session is already unusable; closing is best-effort.
            }
        }
    }
}
