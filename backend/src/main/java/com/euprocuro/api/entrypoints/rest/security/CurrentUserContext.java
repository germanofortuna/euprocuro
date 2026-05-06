package com.euprocuro.api.entrypoints.rest.security;

import java.time.Instant;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import com.euprocuro.api.application.exception.UnauthorizedException;

public final class CurrentUserContext {

    public static final String USER_ID_ATTRIBUTE = "authenticatedUserId";
    public static final String SESSION_EXPIRES_AT_ATTRIBUTE = "authenticatedSessionExpiresAt";

    private CurrentUserContext() {
    }

    public static String userId(HttpServletRequest request) {
        Object userId = request.getAttribute(USER_ID_ATTRIBUTE);

        if (!(userId instanceof String)) {
            throw new UnauthorizedException("Usuario nao autenticado.");
        }

        return (String) userId;
    }

    public static Optional<String> optionalUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(USER_ID_ATTRIBUTE);
        return userId instanceof String ? Optional.of((String) userId) : Optional.empty();
    }

    public static Optional<Instant> optionalSessionExpiresAt(HttpServletRequest request) {
        Object expiresAt = request.getAttribute(SESSION_EXPIRES_AT_ATTRIBUTE);
        return expiresAt instanceof Instant ? Optional.of((Instant) expiresAt) : Optional.empty();
    }
}
