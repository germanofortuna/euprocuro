package com.euprocuro.api.entrypoints.rest.security;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthTokenResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthCookieManager authCookieManager;

    public Optional<String> resolve(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();

            if (StringUtils.hasText(token)) {
                return Optional.of(token);
            }
        }

        return authCookieManager.resolveToken(request);
    }
}