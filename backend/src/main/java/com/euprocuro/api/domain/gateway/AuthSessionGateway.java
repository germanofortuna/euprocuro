package com.euprocuro.api.domain.gateway;

import java.util.Optional;

import com.euprocuro.api.domain.model.AuthSession;

public interface AuthSessionGateway {
    AuthSession save(AuthSession session);

    Optional<AuthSession> findByToken(String token);

    void deleteByToken(String token);
}
