package com.euprocuro.api.entrypoints.rest.dto.response;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminOperationalCatalogResponse {
    MonetizationSettingsResponse monetizationSettings;
    List<CategoryOptionResponse> categories;
    List<MonetizationProductResponse> products;
    Instant updatedAt;
}
