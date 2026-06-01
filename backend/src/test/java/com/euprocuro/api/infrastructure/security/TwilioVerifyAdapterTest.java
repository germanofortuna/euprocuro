package com.euprocuro.api.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.domain.model.PhoneVerificationChannel;

class TwilioVerifyAdapterTest {

    private TwilioVerifyAdapter logAdapter(String bypassCode, RestTemplate restTemplate) {
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        when(builder.build()).thenReturn(restTemplate);
        return new TwilioVerifyAdapter(builder, "LOG", "https://verify.twilio.com/v2", "", "", "", bypassCode);
    }

    private TwilioVerifyAdapter twilioAdapter(RestTemplate restTemplate) {
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        when(builder.build()).thenReturn(restTemplate);
        return new TwilioVerifyAdapter(builder, "TWILIO", "https://verify.twilio.com/v2",
                "AC123", "token", "VA123", "");
    }

    @Test
    void logModeShouldNotCallRestTemplateOnStart() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        TwilioVerifyAdapter adapter = logAdapter("00000", restTemplate);

        adapter.startVerification("+5511912345678", PhoneVerificationChannel.SMS);

        verify(restTemplate, never()).exchange(any(String.class), any(), any(), eq(Map.class));
    }

    @Test
    void logModeShouldApproveBypassCode() {
        TwilioVerifyAdapter adapter = logAdapter("00000", mock(RestTemplate.class));

        assertThat(adapter.checkVerification("+5511912345678", "00000")).isTrue();
        assertThat(adapter.checkVerification("+5511912345678", "11111")).isFalse();
    }

    @Test
    void logModeWithoutBypassShouldAcceptNumericCode() {
        TwilioVerifyAdapter adapter = logAdapter("", mock(RestTemplate.class));

        assertThat(adapter.checkVerification("+5511912345678", "12345")).isTrue();
        assertThat(adapter.checkVerification("+5511912345678", "abc")).isFalse();
    }

    @Test
    void checkVerificationShouldRejectBlankCode() {
        TwilioVerifyAdapter adapter = logAdapter("00000", mock(RestTemplate.class));

        assertThat(adapter.checkVerification("+5511912345678", " ")).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void twilioModeShouldPostStartVerification() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(contains("/Verifications"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("status", "pending")));
        TwilioVerifyAdapter adapter = twilioAdapter(restTemplate);

        adapter.startVerification("+5511912345678", PhoneVerificationChannel.SMS);

        verify(restTemplate).exchange(
                contains("/Services/VA123/Verifications"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void twilioModeShouldThrowBusinessExceptionWhenStartFails() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("boom"));
        TwilioVerifyAdapter adapter = twilioAdapter(restTemplate);

        assertThatThrownBy(() -> adapter.startVerification("+5511912345678", PhoneVerificationChannel.SMS))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void twilioModeShouldReturnTrueWhenApproved() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(contains("/VerificationCheck"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("status", "approved")));
        TwilioVerifyAdapter adapter = twilioAdapter(restTemplate);

        assertThat(adapter.checkVerification("+5511912345678", "12345")).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void twilioModeShouldReturnFalseWhenNotApproved() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(contains("/VerificationCheck"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("status", "pending")));
        TwilioVerifyAdapter adapter = twilioAdapter(restTemplate);

        assertThat(adapter.checkVerification("+5511912345678", "12345")).isFalse();
    }
}
