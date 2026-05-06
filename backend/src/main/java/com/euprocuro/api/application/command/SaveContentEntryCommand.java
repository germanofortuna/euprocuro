package com.euprocuro.api.application.command;

import java.time.Instant;

import com.euprocuro.api.domain.model.ContentEntryType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SaveContentEntryCommand {
    String key;
    ContentEntryType type;
    String locale;
    String draftValue;
    String description;
    String screen;
    String legalSlug;
    boolean requiresUserAcceptance;
    Instant effectiveFrom;
}
