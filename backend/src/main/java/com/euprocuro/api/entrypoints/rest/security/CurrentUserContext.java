package com.euprocuro.api.entrypoints.rest.security;

import javax.servlet.http.HttpServletRequest;

import com.euprocuro.api.application.exception.UnauthorizedException;

public final class CurrentUserContext {

    public static final String USER_ID_ATTRIBUTE = "authenticatedUserId";
    public static final String TOKEN_ATTRIBUTE = "authenticatedToken";

    private CurrentUserContext() {
    }

    public static String userId(HttpServletRequest request) {
        Object userId = request.getAttribute(USER_ID_ATTRIBUTE);
        if (!(userId instanceof String)) {
            throw new UnauthorizedException("Usuario nao autenticado.");
        }

        return (String) userId;
    }

    public static String token(HttpServletRequest request) {
        Object token = request.getAttribute(TOKEN_ATTRIBUTE);
        if (!(token instanceof String)) {
            throw new UnauthorizedException("Sessao nao autenticada.");
        }

        return (String) token;
    }
}
