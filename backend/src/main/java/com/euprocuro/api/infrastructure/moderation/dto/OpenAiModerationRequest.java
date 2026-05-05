package com.euprocuro.api.infrastructure.moderation.dto;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OpenAiModerationRequest {
    String model;
    List<Map<String, Object>> input;
}
