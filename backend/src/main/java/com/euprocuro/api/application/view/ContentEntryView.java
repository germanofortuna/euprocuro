package com.euprocuro.api.application.view;

import java.time.Instant;

import com.euprocuro.api.domain.model.ContentEntryStatus;
import com.euprocuro.api.domain.model.ContentEntryType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ContentEntryView {
    String id;
    String key;
    ContentEntryType type;
    String locale;
    ContentEntryStatus status;
    int version;
    String draftValue;
    String publishedValue;
    String publicValue;
    String description;
    String screen;
    String legalSlug;
    boolean requiresUserAcceptance;
    Instant effectiveFrom;
    Instant createdAt;
    Instant updatedAt;
    Instant publishedAt;
}
