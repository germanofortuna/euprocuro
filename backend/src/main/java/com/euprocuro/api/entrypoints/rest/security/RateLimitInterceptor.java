package com.euprocuro.api.entrypoints.rest.security;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.euprocuro.api.application.exception.TooManyRequestsException;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final boolean enabled;
    private final long windowSeconds;
    private final int maxRequests;
    private final Map<String, Deque<Long>> requestsByKey = new ConcurrentHashMap<>();

    public RateLimitInterceptor(
            @Value("${application.security.rate-limit.enabled:true}") boolean enabled,
            @Value("${application.security.rate-limit.window-seconds:300}") long windowSeconds,
            @Value("${application.security.rate-limit.max-requests:25}") int maxRequests
    ) {
        this.enabled = enabled;
        this.windowSeconds = windowSeconds;
        this.maxRequests = maxRequests;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!enabled || !isSensitiveRoute(request)) {
            return true;
        }

        String key = resolveClientIp(request) + ":" + request.getMethod() + ":" + normalizePath(request.getRequestURI());
        long now = Instant.now().getEpochSecond();
        long windowStart = now - windowSeconds;

        Deque<Long> timestamps = requestsByKey.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxRequests) {
                throw new TooManyRequestsException("Muitas tentativas em pouco tempo. Aguarde alguns minutos e tente novamente.");
            }

            timestamps.addLast(now);
        }

        return true;
    }

    private boolean isSensitiveRoute(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        if ("POST".equalsIgnoreCase(method)) {
            return "/api/auth/login".equals(uri)
                    || "/api/auth/register".equals(uri)
                    || "/api/auth/forgot-password".equals(uri)
                    || "/api/auth/reset-password".equals(uri)
                    || uri.matches("^/api/offers/[^/]+/messages$");
        }

        return false;
    }

    private String normalizePath(String path) {
        if (path.matches("^/api/offers/[^/]+/messages$")) {
            return "/api/offers/:offerId/messages";
        }
        return path;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
