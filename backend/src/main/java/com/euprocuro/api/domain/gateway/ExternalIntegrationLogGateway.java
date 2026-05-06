package com.euprocuro.api.domain.gateway;

import com.euprocuro.api.domain.model.ExternalIntegrationLog;

public interface ExternalIntegrationLogGateway {
    ExternalIntegrationLog save(ExternalIntegrationLog integrationLog);
}
