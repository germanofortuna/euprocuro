package com.euprocuro.api.entrypoints.rest.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    @Test
    void resolveShouldIgnoreForwardedHeadersFromUntrustedRemoteAddress() {
        ClientIpResolver resolver = new ClientIpResolver("");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10");
        request.addHeader("CF-Connecting-IP", "2001:db8::1");
        request.setRemoteAddr("198.51.100.20");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.20");
    }

    @Test
    void resolveShouldUseRightmostUntrustedForwardedForIpFromTrustedProxy() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.0/24");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "unknown, '999.999.999.999', \"203.0.113.10\", 198.51.100.20");
        request.setRemoteAddr("10.0.0.5");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.20");
    }

    @Test
    void resolveShouldSkipTrustedProxyEntriesInForwardedForChain() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.0/24, 198.51.100.20");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 198.51.100.20");
        request.setRemoteAddr("10.0.0.5");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void resolveShouldUseSingleValueHeaderFromTrustedProxyWhenForwardedForMissing() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.5");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "2001:db8::1");
        request.addHeader("X-Real-IP", "198.51.100.20");
        request.setRemoteAddr("10.0.0.5");

        assertThat(resolver.resolve(request)).isEqualTo("2001:db8::1");
    }

    @Test
    void resolveShouldFallbackToRemoteAddressAndReturnNullWhenInvalid() {
        ClientIpResolver resolver = new ClientIpResolver("");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "unknown");
        request.setRemoteAddr("198.51.100.30");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.30");

        MockHttpServletRequest invalidRequest = new MockHttpServletRequest();
        invalidRequest.setRemoteAddr("not-an-ip");

        assertThat(resolver.resolve(invalidRequest)).isNull();
    }
}
