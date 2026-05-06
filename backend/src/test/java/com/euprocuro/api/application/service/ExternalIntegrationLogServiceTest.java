package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.euprocuro.api.domain.gateway.ExternalIntegrationLogGateway;
import com.euprocuro.api.domain.model.ExternalIntegrationLog;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ExternalIntegrationLogServiceTest {

    @Mock
    private ExternalIntegrationLogGateway externalIntegrationLogGateway;

    private ExternalIntegrationLogService service;

    @BeforeEach
    void setUp() {
        service = new ExternalIntegrationLogService(externalIntegrationLogGateway, new ObjectMapper());
        ReflectionTestUtils.setField(service, "bodyMaxLength", 20);
    }

    @Test
    void recordSuccessShouldSanitizeHeadersSerializeBodiesAndKeepMetadata() {
        Instant startedAt = Instant.now().minusMillis(150);

        service.recordSuccess(
                "MERCADO_PAGO_CREATE_CHECKOUT_PREFERENCE",
                "payment-1",
                "POST",
                "https://api.mercadopago.com/checkout/preferences",
                Map.of(
                        "Authorization", "Bearer token-secreto",
                        "X-Request-Id", "request-123"
                ),
                Map.of("email", "buyer@teste.com", "amount", 10),
                200,
                Map.of("id", "preference-1"),
                startedAt
        );

        ArgumentCaptor<ExternalIntegrationLog> captor = ArgumentCaptor.forClass(ExternalIntegrationLog.class);
        verify(externalIntegrationLogGateway).save(captor.capture());

        ExternalIntegrationLog log = captor.getValue();
        assertThat(log.getOperation()).isEqualTo("MERCADO_PAGO_CREATE_CHECKOUT_PREFERENCE");
        assertThat(log.getCorrelationId()).isEqualTo("payment-1");
        assertThat(log.getRequest().getMethod()).isEqualTo("POST");
        assertThat(log.getRequest().getUrl()).isEqualTo("https://api.mercadopago.com/checkout/preferences");
        assertThat(log.getRequest().getHeaders()).containsEntry("Authorization", "***");
        assertThat(log.getRequest().getHeaders()).containsEntry("X-Request-Id", "request-123");
        assertThat(log.getRequest().getBody()).asString().contains("buyer@teste.com");
        assertThat(log.getResponse().getStatus()).isEqualTo(200);
        assertThat(log.getResponse().getBody()).asString().contains("preference-1");
        assertThat(log.getDurationMs()).isNotNegative();
        assertThat(log.isSuccess()).isTrue();
        assertThat(log.getErrorMessage()).isNull();
    }

    @Test
    void recordFailureShouldStoreErrorAndTruncateLongPayloads() {
        String longPayload = "a".repeat(300);

        service.recordFailure(
                "OPEN_AI_MODERATION",
                "interest-1",
                "POST",
                "/v1/moderations",
                Map.of("OpenAI-Api-Key", "sk-secret"),
                longPayload,
                null,
                longPayload,
                Instant.now().minusMillis(50),
                new RuntimeException(longPayload)
        );

        ArgumentCaptor<ExternalIntegrationLog> captor = ArgumentCaptor.forClass(ExternalIntegrationLog.class);
        verify(externalIntegrationLogGateway).save(captor.capture());

        ExternalIntegrationLog log = captor.getValue();
        assertThat(log.isSuccess()).isFalse();
        assertThat(log.getRequest().getHeaders()).containsEntry("OpenAI-Api-Key", "***");
        assertThat(log.getRequest().getBody()).asString().hasSize(264).endsWith("...[truncated]");
        assertThat(log.getResponse().getBody()).asString().hasSize(264).endsWith("...[truncated]");
        assertThat(log.getErrorMessage()).hasSize(264).endsWith("...[truncated]");
    }

    @Test
    void recordShouldNotPropagateGatewayFailures() {
        when(externalIntegrationLogGateway.save(any(ExternalIntegrationLog.class)))
                .thenThrow(new RuntimeException("mongo indisponivel"));

        assertThatCode(() -> service.recordSuccess(
                "VIA_CEP",
                "99709164",
                "GET",
                "https://viacep.com.br/ws/99709164/json/",
                Map.of(),
                null,
                200,
                Map.of("localidade", "Erechim"),
                Instant.now()
        )).doesNotThrowAnyException();
    }
}
