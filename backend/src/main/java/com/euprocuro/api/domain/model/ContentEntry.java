package com.euprocuro.api.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ContentEntry {
    private String id;
    private String key;
    private ContentEntryType type;
    private String locale;
    private ContentEntryStatus status;
    private int version;
    private String draftValue;
    private String publishedValue;
    private String defaultValue;
    private String defaultValueHash;
    private String description;
    private String screen;
    private String legalSlug;
    private boolean requiresUserAcceptance;
    private boolean defaultUpdateAvailable;
    private String ignoredDefaultValueHash;
    private Instant effectiveFrom;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant defaultUpdatedAt;
    private Instant publishedAt;
    private String updatedBy;
    private String publishedBy;
}
