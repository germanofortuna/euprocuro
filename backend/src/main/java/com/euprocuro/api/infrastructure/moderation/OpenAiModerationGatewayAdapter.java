package com.euprocuro.api.infrastructure.moderation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.Instant;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.euprocuro.api.application.service.ExternalIntegrationLogService;
import com.euprocuro.api.domain.gateway.AiModerationGateway;
import com.euprocuro.api.domain.model.ModerationContent;
import com.euprocuro.api.domain.model.ModerationResult;
import com.euprocuro.api.infrastructure.moderation.dto.OpenAiModerationRequest;
import com.euprocuro.api.infrastructure.moderation.dto.OpenAiModerationResponse;

import lombok.RequiredArgsConstructor;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiModerationGatewayAdapter implements AiModerationGateway {

    private final OpenAiModerationClient openAiModerationClient;
    private final ExternalIntegrationLogService externalIntegrationLogService;

    @Value("${application.moderation.openai.enabled:false}")
    private boolean enabled;
    @Value("${application.moderation.openai.api-key:}")
    private String apiKey;
    @Value("${application.moderation.openai.model:omni-moderation-latest}")
    private String model;

    @Override
    public Optional<ModerationResult> moderate(ModerationContent content) {
        if (!enabled || !StringUtils.hasText(apiKey)) {
            return Optional.empty();
        }

        OpenAiModerationRequest request = requestBody(content);
        Instant startedAt = externalIntegrationLogService.startedAt();
        try {
            OpenAiModerationResponse response = openAiModerationClient.moderate(
                    "Bearer " + apiKey,
                    request
            );
            externalIntegrationLogService.recordSuccess(
                    "OPEN_AI_MODERATION",
                    null,
                    "POST",
                    "/v1/moderations",
                    Map.of("Authorization", "Bearer " + apiKey),
                    request,
                    200,
                    response,
                    startedAt
            );
            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                return Optional.of(unavailableResult());
            }

            OpenAiModerationResponse.Result result = response.getResults().get(0);
            Map<String, Boolean> categories = result.getCategories();
            Map<String, Double> scores = result.getCategoryScores();
            Map<String, Boolean> matchedCategories = Optional.ofNullable(categories).orElse(Map.of())
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> Boolean.TRUE.equals(entry.getValue())
                    ));

            return Optional.of(ModerationResult.builder()
                    .flagged(result.isFlagged())
                    .categories(matchedCategories)
                    .scores(scores == null ? Collections.emptyMap() : scores)
                    .provider("OPENAI")
                    .build());
        } catch (Exception exception) {
            externalIntegrationLogService.recordFailure(
                    "OPEN_AI_MODERATION",
                    null,
                    "POST",
                    "/v1/moderations",
                    Map.of("Authorization", "Bearer " + apiKey),
                    request,
                    null,
                    null,
                    startedAt,
                    exception
            );
            log.error("Falha ao chamar OpenAI Moderation via Feign. Aplicando fallback local. {}", exception.getMessage());
            return Optional.of(unavailableResult());
        }
    }

    private ModerationResult unavailableResult() {
        return ModerationResult.builder()
                .flagged(false)
                .categories(Map.of("openai_unavailable", false))
                .scores(Map.of("openai_unavailable", 0.55))
                .provider("OPENAI_UNAVAILABLE")
                .build();
    }

    private OpenAiModerationRequest requestBody(ModerationContent content) {
        List<Map<String, Object>> input = new ArrayList<>();
        input.add(Map.of("type", "text", "text", moderationText(content)));
        if (StringUtils.hasText(content.getImageUrl())) {
            input.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", content.getImageUrl())
            ));
        }
        return OpenAiModerationRequest.builder()
                .model(model)
                .input(input)
                .build();
    }

    private String moderationText(ModerationContent content) {
        return String.join("\n",
                "Titulo: " + safe(content.getTitle()),
                "Descricao: " + safe(content.getDescription()),
                "Tags: " + safe(content.getTags()));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
