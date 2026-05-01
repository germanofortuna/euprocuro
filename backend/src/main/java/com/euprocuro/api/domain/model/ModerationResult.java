package com.euprocuro.api.domain.model;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ModerationResult {
    boolean flagged;
    List<String> categories;
    Map<String, Double> scores;
    String provider;
}
