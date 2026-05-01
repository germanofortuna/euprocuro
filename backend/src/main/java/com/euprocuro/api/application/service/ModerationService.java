package com.euprocuro.api.application.service;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.euprocuro.api.application.command.ReportInterestCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.application.usecase.ModerationUseCase;
import com.euprocuro.api.domain.gateway.AiModerationGateway;
import com.euprocuro.api.domain.gateway.ContentReportGateway;
import com.euprocuro.api.domain.gateway.InterestGateway;
import com.euprocuro.api.domain.gateway.ModerationRuleGateway;
import com.euprocuro.api.domain.gateway.RealtimeMessageGateway;
import com.euprocuro.api.domain.model.ContentReport;
import com.euprocuro.api.domain.model.ContentReportStatus;
import com.euprocuro.api.domain.model.InterestModeration;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestStatus;
import com.euprocuro.api.domain.model.ModerationContent;
import com.euprocuro.api.domain.model.ModerationResult;
import com.euprocuro.api.domain.model.ModerationRiskLevel;
import com.euprocuro.api.domain.model.ModerationRule;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModerationService implements ModerationUseCase {

    private static final Pattern LINK_PATTERN = Pattern.compile(
            "(?i)(https?://|www\\.|\\b[a-z0-9][a-z0-9-]*(?:\\.[a-z0-9][a-z0-9-]*)+\\b)"
    );

    private final InterestGateway interestGateway;
    private final ModerationRuleGateway moderationRuleGateway;
    private final ContentReportGateway contentReportGateway;
    private final AiModerationGateway aiModerationGateway;
    private final RealtimeMessageGateway realtimeMessageGateway;

    @Value("${application.moderation.local.blocked-terms:}")
    private String defaultBlockedTerms;
    @Value("${application.moderation.ai.review-threshold:0.55}")
    private double reviewThreshold = 0.55;
    @Value("${application.moderation.ai.reject-threshold:0.85}")
    private double rejectThreshold = 0.85;

    @Override
    public void processInterestModeration(String interestId) {
        InterestPost interest = interestGateway.findById(interestId)
                .orElseThrow(() -> new ResourceNotFoundException("Interesse nao encontrado."));
        if (interest.getStatus() != InterestStatus.PENDING) {
            return;
        }

        LocalRuleMatch localRuleMatch = findLocalRuleMatch(interest).orElse(null);
        if (localRuleMatch != null && localRuleMatch.riskLevel == ModerationRiskLevel.HIGH) {
            saveDecision(interest, InterestStatus.REJECTED, ModerationRiskLevel.HIGH, "LOCAL_RULE",
                    List.of("local_rule"), Map.of(localRuleMatch.term, 1.0),
                    localRuleMatch.reason);
            return;
        }

        Optional<ModerationResult> aiResult = aiModerationGateway.moderate(toModerationContent(interest));
        if (aiResult.isEmpty()) {
            if (localRuleMatch != null) {
                saveDecision(interest, InterestStatus.REVIEW_REQUIRED, ModerationRiskLevel.MEDIUM, "LOCAL_RULE",
                        List.of("local_rule"), Map.of(localRuleMatch.term, 1.0),
                        "Este anuncio precisa de revisao manual por uma regra local.");
                return;
            }
            saveDecision(interest, InterestStatus.APPROVED, ModerationRiskLevel.LOW, "LOCAL_ONLY",
                    List.of(), Map.of(), "Anuncio aprovado pelas regras locais.");
            return;
        }

        ModerationResult result = aiResult.get();
        double highestScore = highestScore(result.getScores());
        if (result.isFlagged() || highestScore >= rejectThreshold) {
            saveDecision(interest, InterestStatus.REJECTED, ModerationRiskLevel.HIGH, result.getProvider(),
                    result.getCategories(), result.getScores(), "A moderacao automatica rejeitou este anuncio.");
            return;
        }

        if (highestScore >= reviewThreshold || localRuleMatch != null) {
            saveDecision(interest, InterestStatus.REVIEW_REQUIRED, ModerationRiskLevel.MEDIUM, result.getProvider(),
                    result.getCategories(), result.getScores(), "Este anuncio precisa de revisao manual.");
            return;
        }

        saveDecision(interest, InterestStatus.APPROVED, ModerationRiskLevel.LOW, result.getProvider(),
                result.getCategories(), result.getScores(), "Anuncio aprovado automaticamente.");
    }

    @Override
    public ContentReport reportInterest(String currentUserId, String interestId, ReportInterestCommand command) {
        InterestPost interest = interestGateway.findById(interestId)
                .orElseThrow(() -> new ResourceNotFoundException("Interesse nao encontrado."));
        if (!isPubliclyVisible(interest)) {
            throw new BusinessException("Este anuncio nao esta disponivel para denuncia.");
        }

        ContentReport report = contentReportGateway.save(ContentReport.builder()
                .contentType("INTEREST")
                .contentId(interestId)
                .reportedBy(currentUserId)
                .reason(command.getReason())
                .message(command.getMessage())
                .status(ContentReportStatus.OPEN)
                .createdAt(Instant.now())
                .build());

        interestGateway.save(interest.toBuilder()
                .status(InterestStatus.REPORTED)
                .updatedAt(Instant.now())
                .moderation(mergeModeration(interest.getModeration(), InterestStatus.REPORTED, ModerationRiskLevel.MEDIUM,
                        "USER_REPORT", "Este anuncio recebeu uma denuncia e sera analisado."))
                .build());
        realtimeMessageGateway.publishInterestModerationUpdated(
                interest.getOwnerId(),
                interest.getId(),
                InterestStatus.REPORTED.name(),
                "Seu anuncio recebeu uma denuncia e sera analisado."
        );
        return report;
    }

    private void saveDecision(
            InterestPost interest,
            InterestStatus status,
            ModerationRiskLevel riskLevel,
            String provider,
            List<String> categories,
            Map<String, Double> scores,
            String reason
    ) {
        InterestPost saved = interestGateway.save(interest.toBuilder()
                .status(status)
                .updatedAt(Instant.now())
                .moderation(InterestModeration.builder()
                        .riskLevel(riskLevel)
                        .categories(categories == null ? List.of() : categories)
                        .scores(scores == null ? Map.of() : scores)
                        .reviewRequired(status == InterestStatus.REVIEW_REQUIRED)
                        .provider(provider)
                        .reason(reason)
                        .checkedAt(Instant.now())
                        .build())
                .build());
        realtimeMessageGateway.publishInterestModerationUpdated(saved.getOwnerId(), saved.getId(), status.name(), reason);
    }

    private InterestModeration mergeModeration(
            InterestModeration current,
            InterestStatus status,
            ModerationRiskLevel riskLevel,
            String provider,
            String reason
    ) {
        InterestModeration.InterestModerationBuilder builder = current == null
                ? InterestModeration.builder()
                : current.toBuilder();
        return builder
                .riskLevel(riskLevel)
                .reviewRequired(status == InterestStatus.REVIEW_REQUIRED || status == InterestStatus.REPORTED)
                .provider(provider)
                .reason(reason)
                .checkedAt(Instant.now())
                .build();
    }

    private Optional<LocalRuleMatch> findLocalRuleMatch(InterestPost interest) {
        String content = normalizedContent(interest);
        if (LINK_PATTERN.matcher(content).find()) {
            return Optional.of(new LocalRuleMatch(
                    "link",
                    ModerationRiskLevel.HIGH,
                    "Links nao sao permitidos em anuncios."
            ));
        }
        return allActiveRules().stream()
                .filter(rule -> StringUtils.hasText(rule.getTerm()))
                .filter(rule -> content.contains(normalize(rule.getTerm())))
                .map(rule -> new LocalRuleMatch(
                        rule.getTerm(),
                        Optional.ofNullable(rule.getRiskLevel()).orElse(ModerationRiskLevel.HIGH),
                        "O anuncio contem um termo bloqueado pela plataforma."
                ))
                .max(Comparator.comparing(match -> match.riskLevel));
    }

    private List<ModerationRule> allActiveRules() {
        List<ModerationRule> persistedRules = moderationRuleGateway.findByActiveTrue();
        List<ModerationRule> configuredRules = Arrays.stream(defaultBlockedTerms.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(term -> ModerationRule.builder()
                        .term(term)
                        .riskLevel(ModerationRiskLevel.HIGH)
                        .active(true)
                        .build())
                .collect(Collectors.toList());
        return java.util.stream.Stream.concat(persistedRules.stream(), configuredRules.stream())
                .collect(Collectors.toList());
    }

    private ModerationContent toModerationContent(InterestPost interest) {
        return ModerationContent.builder()
                .title(interest.getTitle())
                .description(interest.getDescription())
                .tags(String.join(", ", Optional.ofNullable(interest.getTags()).orElse(List.of())))
                .imageUrl(interest.getReferenceImageUrl())
                .build();
    }

    private String normalizedContent(InterestPost interest) {
        return normalize(String.join(" ",
                Optional.ofNullable(interest.getTitle()).orElse(""),
                Optional.ofNullable(interest.getDescription()).orElse(""),
                String.join(" ", Optional.ofNullable(interest.getTags()).orElse(List.of()))));
    }

    private boolean isPubliclyVisible(InterestPost interest) {
        return interest.getStatus() == InterestStatus.OPEN
                || interest.getStatus() == InterestStatus.APPROVED
                || interest.getStatus() == InterestStatus.REPORTED;
    }

    private double highestScore(Map<String, Double> scores) {
        return Optional.ofNullable(scores).orElse(Map.of())
                .values()
                .stream()
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static final class LocalRuleMatch {
        private final String term;
        private final ModerationRiskLevel riskLevel;
        private final String reason;

        private LocalRuleMatch(String term, ModerationRiskLevel riskLevel, String reason) {
            this.term = term;
            this.riskLevel = riskLevel;
            this.reason = reason;
        }
    }
}
