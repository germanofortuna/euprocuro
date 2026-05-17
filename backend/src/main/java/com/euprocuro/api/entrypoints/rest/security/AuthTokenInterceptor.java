package com.euprocuro.api.entrypoints.rest.security;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.euprocuro.api.application.exception.UnauthorizedException;
import com.euprocuro.api.application.service.AuditLogService;
import com.euprocuro.api.application.usecase.AuthUseCase;
import com.euprocuro.api.application.view.AuthenticatedSessionView;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthTokenInterceptor implements HandlerInterceptor {

    private final AuthUseCase authUseCase;
    private final AuthTokenResolver authTokenResolver;
    private final AuthCookieManager authCookieManager;
    private final AuditLogService auditLogService;

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) throws IOException {
        if (shouldSkipAuthentication(request)) {
            return true;
        }

        if (isPublicInterestRead(request)) {
            authTokenResolver.resolve(request)
                    .ifPresent(token -> authenticateOptionalToken(request, response, token));
            return true;
        }

        String token = authTokenResolver.resolve(request).orElse(null);
        if (token == null) {
            recordMissingToken(request);
            return rejectUnauthorized(response, "Sessao encerrada.");
        }

        AuthenticatedSessionView session;
        try {
            session = authUseCase.requireAuthenticatedSession(token);
        } catch (UnauthorizedException exception) {
            authCookieManager.clearSessionCookie(response);
            return rejectUnauthorized(response, exception.getMessage());
        }

        request.setAttribute(CurrentUserContext.USER_ID_ATTRIBUTE, session.getUser().getId());
        request.setAttribute(CurrentUserContext.SESSION_EXPIRES_AT_ATTRIBUTE, session.getExpiresAt());

        if (session.isRenewed()) {
            authCookieManager.writeSessionCookie(response, session.getToken(), session.getExpiresAt());
            response.setHeader("X-Auth-Expires-At", session.getExpiresAt().toString());
        }

        return true;
    }

    private void authenticateOptionalToken(HttpServletRequest request, HttpServletResponse response, String token) {
        try {
            AuthenticatedSessionView session = authUseCase.requireAuthenticatedSession(token);
            request.setAttribute(CurrentUserContext.USER_ID_ATTRIBUTE, session.getUser().getId());
            request.setAttribute(CurrentUserContext.SESSION_EXPIRES_AT_ATTRIBUTE, session.getExpiresAt());

            if (session.isRenewed()) {
                authCookieManager.writeSessionCookie(response, session.getToken(), session.getExpiresAt());
                response.setHeader("X-Auth-Expires-At", session.getExpiresAt().toString());
            }
        } catch (UnauthorizedException exception) {
            request.removeAttribute(CurrentUserContext.USER_ID_ATTRIBUTE);
            request.removeAttribute(CurrentUserContext.SESSION_EXPIRES_AT_ATTRIBUTE);
        }
    }

    private boolean shouldSkipAuthentication(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();

        if ("/api/auth/logout".equals(uri)) {
            return true;
        }

        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        return "/api/categories".equals(uri);
    }

    private boolean isPublicInterestRead(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String uri = request.getRequestURI();
        return "/api/interests".equals(uri) || uri.matches("^/api/interests/[^/]+$");
    }

    private void recordMissingToken(HttpServletRequest request) {
        auditLogService.record(
                "AUTH_MISSING_TOKEN",
                null,
                null,
                "AUTH_SESSION",
                "missing-token",
                AuditLogService.OUTCOME_FAILURE,
                Map.of(
                        "method", request.getMethod(),
                        "path", request.getRequestURI(),
                        "reason", "MISSING_TOKEN"
                )
        );
    }

    private boolean rejectUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"details\":[\"" + message + "\"]}");
        return false;
    }
}
