package com.euprocuro.api.infrastructure.moderation.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class OpenAiModerationResponse {
    private List<Result> results;

    @Data
    public static class Result {
        private boolean flagged;
        private Map<String, Boolean> categories;
        @JsonProperty("category_scores")
        private Map<String, Double> categoryScores;
    }
}
