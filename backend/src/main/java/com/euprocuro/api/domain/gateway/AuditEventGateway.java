package com.euprocuro.api.domain.gateway;

import com.euprocuro.api.domain.model.AuditEvent;

public interface AuditEventGateway {
    AuditEvent save(AuditEvent auditEvent);
}
