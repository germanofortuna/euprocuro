package com.euprocuro.api.entrypoints.rest.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import com.euprocuro.api.application.exception.UnauthorizedException;
import com.euprocuro.api.application.usecase.AuthUseCase;
import com.euprocuro.api.domain.model.UserProfile;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthTokenInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthUseCase authUseCase;
    private final AuthCookieManager authCookieManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublicRequest(request)) {
            return true;
        }

        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            throw new UnauthorizedException("Token de acesso nao informado.");
        }

        UserProfile user = authUseCase.requireAuthenticatedUser(token);
        request.setAttribute(CurrentUserContext.USER_ID_ATTRIBUTE, user.getId());
        request.setAttribute(CurrentUserContext.TOKEN_ATTRIBUTE, token);
        return true;
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }

        return authCookieManager.resolveToken(request).orElse(null);
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String uri = request.getRequestURI();
        return "/api/categories".equals(uri)
                || "/api/interests".equals(uri)
                || uri.matches("^/api/interests/[^/]+$");
    }
}
