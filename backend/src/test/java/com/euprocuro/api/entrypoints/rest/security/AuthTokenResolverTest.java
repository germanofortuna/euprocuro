package com.euprocuro.api.entrypoints.rest.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class AuthTokenResolverTest {

    private final AuthCookieManager authCookieManager = mock(AuthCookieManager.class);
    private final AuthTokenResolver resolver = new AuthTokenResolver(authCookieManager);

    @Test
    void resolveShouldReadBearerTokenFromAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-123 ");

        assertThat(resolver.resolve(request)).contains("token-123");
    }

    @Test
    void resolveShouldFallbackToCookieWhenHeaderIsMissingOrBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(authCookieManager.resolveToken(request)).thenReturn(Optional.of("cookie-token"));

        assertThat(resolver.resolve(request)).contains("cookie-token");
        verify(authCookieManager).resolveToken(request);
    }

    @Test
    void resolveShouldIgnoreEmptyBearerToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer   ");
        when(authCookieManager.resolveToken(request)).thenReturn(Optional.empty());

        assertThat(resolver.resolve(request)).isEmpty();
    }
}
