package com.euprocuro.api.entrypoints.rest.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.euprocuro.api.application.exception.UnauthorizedException;
import com.euprocuro.api.application.usecase.AuthUseCase;
import com.euprocuro.api.domain.model.UserProfile;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthTokenInterceptor implements HandlerInterceptor {

    private final AuthUseCase authUseCase;
    private final AuthTokenResolver authTokenResolver;

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {
        if (shouldSkipAuthentication(request)) {
            return true;
        }

        String token = authTokenResolver.resolve(request)
                .orElseThrow(() -> new UnauthorizedException("Token de acesso nao informado."));

        UserProfile user = authUseCase.requireAuthenticatedUser(token);

        request.setAttribute(CurrentUserContext.USER_ID_ATTRIBUTE, user.getId());

        return true;
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

        return "/api/categories".equals(uri)
                || "/api/interests".equals(uri)
                || uri.matches("^/api/interests/[^/]+$");
    }
}