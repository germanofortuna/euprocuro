package com.euprocuro.api.entrypoints.rest.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.euprocuro.api.application.exception.UnauthorizedException;
import com.euprocuro.api.application.service.AuditLogService;
import com.euprocuro.api.application.usecase.AuthUseCase;

class AuthTokenInterceptorTest {

    private final AuthUseCase authUseCase = mock(AuthUseCase.class);
    private final AuthTokenResolver authTokenResolver = mock(AuthTokenResolver.class);
    private final AuthCookieManager authCookieManager = mock(AuthCookieManager.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final AuthTokenInterceptor interceptor = new AuthTokenInterceptor(
            authUseCase,
            authTokenResolver,
            authCookieManager,
            auditLogService
    );

    @Test
    void preHandleShouldAuditMissingTokenOnProtectedRoute() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(authTokenResolver.resolve(request)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Sessao encerrada.");

        verify(auditLogService).record(
                eq("AUTH_MISSING_TOKEN"),
                eq(null),
                eq(null),
                eq("AUTH_SESSION"),
                eq("missing-token"),
                eq(AuditLogService.OUTCOME_FAILURE),
                eq(Map.of(
                        "method", "GET",
                        "path", "/api/dashboard",
                        "reason", "MISSING_TOKEN"
                ))
        );
        verifyNoInteractions(authUseCase);
    }
}
