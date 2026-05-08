package com.euprocuro.api.application.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.euprocuro.api.domain.gateway.AuditEventGateway;
import com.euprocuro.api.domain.model.AuditEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    public static final String OUTCOME_SUCCESS = "SUCCESS";
    public static final String OUTCOME_FAILURE = "FAILURE";

    private final AuditEventGateway auditEventGateway;

    public void record(String action, String actorUserId, String actorEmail, String resourceType, String resourceId) {
        record(action, actorUserId, actorEmail, resourceType, resourceId, OUTCOME_SUCCESS, Map.of());
    }

    public void record(
            String action,
            String actorUserId,
            String actorEmail,
            String resourceType,
            String resourceId,
            String outcome,
            Map<String, Object> metadata
    ) {
        try {
            auditEventGateway.save(AuditEvent.builder()
                    .occurredAt(Instant.now())
                    .action(action)
                    .actorUserId(actorUserId)
                    .actorEmail(actorEmail)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .outcome(outcome)
                    .metadata(Optional.ofNullable(metadata).orElse(Map.of()))
                    .build());
        } catch (RuntimeException exception) {
            log.warn("Nao foi possivel gravar audit log '{}'. Aplicacao seguira normalmente. {}", action, exception.getMessage());
        }
    }
}
