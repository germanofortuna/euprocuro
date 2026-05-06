package com.euprocuro.api.entrypoints.rest.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver();

    @Test
    void resolveShouldPreferFirstValidForwardedHeaderIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "unknown, '999.999.999.999', \"203.0.113.10\", 198.51.100.20");
        request.setRemoteAddr("127.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void resolveShouldUseCloudflareHeaderBeforeOtherHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "2001:db8::1");
        request.addHeader("X-Real-IP", "198.51.100.20");

        assertThat(resolver.resolve(request)).isEqualTo("2001:db8::1");
    }

    @Test
    void resolveShouldFallbackToRemoteAddressAndReturnNullWhenInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "unknown");
        request.setRemoteAddr("198.51.100.30");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.30");

        MockHttpServletRequest invalidRequest = new MockHttpServletRequest();
        invalidRequest.setRemoteAddr("not-an-ip");

        assertThat(resolver.resolve(invalidRequest)).isNull();
    }
}
