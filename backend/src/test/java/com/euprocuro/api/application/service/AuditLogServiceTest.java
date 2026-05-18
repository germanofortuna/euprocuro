package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.euprocuro.api.domain.gateway.AuditEventGateway;
import com.euprocuro.api.domain.model.AuditEvent;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditEventGateway auditEventGateway;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void recordShouldPersistSuccessEventWithEmptyMetadataByDefault() {
        when(auditEventGateway.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditLogService.record("INTEREST_CREATED", "user-1", "user@test.com", "INTEREST", "interest-1");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventGateway).save(captor.capture());
        AuditEvent event = captor.getValue();
        assertThat(event.getOccurredAt()).isNotNull();
        assertThat(event.getAction()).isEqualTo("INTEREST_CREATED");
        assertThat(event.getActorUserId()).isEqualTo("user-1");
        assertThat(event.getActorEmail()).isEqualTo("user@test.com");
        assertThat(event.getResourceType()).isEqualTo("INTEREST");
        assertThat(event.getResourceId()).isEqualTo("interest-1");
        assertThat(event.getOutcome()).isEqualTo(AuditLogService.OUTCOME_SUCCESS);
        assertThat(event.getMetadata()).isEmpty();
    }

    @Test
    void recordShouldPersistFailureEventWithMetadata() {
        when(auditEventGateway.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditLogService.record(
                "LOGIN",
                "user-1",
                "user@test.com",
                "SESSION",
                "session-1",
                AuditLogService.OUTCOME_FAILURE,
                Map.of("reason", "blocked")
        );

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventGateway).save(captor.capture());
        assertThat(captor.getValue().getOutcome()).isEqualTo(AuditLogService.OUTCOME_FAILURE);
        assertThat(captor.getValue().getMetadata()).containsEntry("reason", "blocked");
    }

    @Test
    void recordShouldNotFailBusinessFlowWhenGatewayThrows() {
        when(auditEventGateway.save(any(AuditEvent.class))).thenThrow(new RuntimeException("mongo offline"));

        assertThatCode(() -> auditLogService.record(
                "INTEREST_DELETED",
                "user-1",
                "user@test.com",
                "INTEREST",
                "interest-1",
                AuditLogService.OUTCOME_FAILURE,
                null
        )).doesNotThrowAnyException();
    }
}
