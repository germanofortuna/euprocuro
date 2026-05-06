package com.euprocuro.api.entrypoints.rest.dto.response;

import java.time.Instant;

import com.euprocuro.api.domain.model.ContentEntryType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PublicContentEntryResponse {
    String key;
    ContentEntryType type;
    String locale;
    int version;
    String value;
    String legalSlug;
    boolean requiresUserAcceptance;
    Instant effectiveFrom;
    Instant publishedAt;
}
