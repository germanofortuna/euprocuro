package com.euprocuro.api.entrypoints.rest.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthCookieManager {

    private final String cookieName;
    private final boolean secureCookie;
    private final String sameSite;
    private final String cookieDomain;

    public AuthCookieManager(
            @Value("${application.auth.cookie.name:EU_PROCURO_SESSION}") String cookieName,
            @Value("${application.auth.cookie.secure:false}") boolean secureCookie,
            @Value("${application.auth.cookie.same-site:Lax}") String sameSite,
            @Value("${application.auth.cookie.domain:}") String cookieDomain
    ) {
        this.cookieName = cookieName;
        this.secureCookie = secureCookie;
        this.sameSite = sameSite;
        this.cookieDomain = cookieDomain;
    }

    public void writeSessionCookie(HttpServletResponse response, String token, Instant expiresAt) {
        long maxAgeSeconds = Math.max(0, Duration.between(Instant.now(), expiresAt).getSeconds());
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .sameSite(sameSite)
                .maxAge(Duration.ofSeconds(maxAgeSeconds));

        if (StringUtils.hasText(cookieDomain)) {
            builder.domain(cookieDomain);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    public void clearSessionCookie(HttpServletResponse response) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .sameSite(sameSite)
                .maxAge(Duration.ZERO);

        if (StringUtils.hasText(cookieDomain)) {
            builder.domain(cookieDomain);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    public Optional<String> resolveToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .findFirst();
    }
}
