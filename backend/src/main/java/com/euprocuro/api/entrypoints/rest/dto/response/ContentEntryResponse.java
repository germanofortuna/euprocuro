package com.euprocuro.api.entrypoints.rest.dto.response;

import java.time.Instant;

import com.euprocuro.api.domain.model.ContentEntryStatus;
import com.euprocuro.api.domain.model.ContentEntryType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ContentEntryResponse {
    String id;
    String key;
    ContentEntryType type;
    String locale;
    ContentEntryStatus status;
    int version;
    String draftValue;
    String publishedValue;
    String defaultValue;
    String defaultValueHash;
    String description;
    String screen;
    String legalSlug;
    boolean requiresUserAcceptance;
    boolean defaultUpdateAvailable;
    Instant effectiveFrom;
    Instant createdAt;
    Instant updatedAt;
    Instant defaultUpdatedAt;
    Instant publishedAt;
}
