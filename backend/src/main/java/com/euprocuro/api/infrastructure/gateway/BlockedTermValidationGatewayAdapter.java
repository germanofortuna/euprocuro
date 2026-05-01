package com.euprocuro.api.infrastructure.gateway;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.euprocuro.api.domain.gateway.BlockedTermValidationGateway;
import com.euprocuro.api.domain.gateway.ModerationRuleGateway;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.ModerationRiskLevel;
import com.euprocuro.api.domain.model.ModerationRule;
import com.euprocuro.api.domain.model.SellerItem;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BlockedTermValidationGatewayAdapter implements BlockedTermValidationGateway {

    private static final Pattern LINK_PATTERN = Pattern.compile(
            "(?i)(https?://|www\\.|\\b[a-z0-9][a-z0-9-]*(?:\\.[a-z0-9][a-z0-9-]*)+\\b)"
    );

    private final ModerationRuleGateway moderationRuleGateway;

    @Value("${application.moderation.local.blocked-terms:}")
    private String defaultBlockedTerms;

    @Override
    public Optional<BlockedTermValidationResult> validateBlockedTerms(InterestPost interest) {
        String content = normalizedContent(interest);

        // Check for links
        if (LINK_PATTERN.matcher(content).find()) {
            return Optional.of(new BlockedTermValidationResult(
                    "link",
                    "Links nao sao permitidos em anuncios."
            ));
        }

        // Check for blocked terms
        return allActiveRules().stream()
                .filter(rule -> StringUtils.hasText(rule.getTerm()))
                .filter(rule -> content.contains(normalize(rule.getTerm())))
                .map(rule -> new BlockedTermValidationResult(
                        rule.getTerm(),
                        "O anuncio contem um termo bloqueado pela plataforma."
                ))
                .max(Comparator.comparing(match -> "link".equals(match.getTerm()) ? 1 : 0));
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

    @Override
    public Optional<BlockedTermValidationResult> validateBlockedTerms(SellerItem item) {
        String content = normalizedContent(item);

        // Check for links
        if (LINK_PATTERN.matcher(content).find()) {
            return Optional.of(new BlockedTermValidationResult(
                    "link",
                    "Links nao sao permitidos em anuncios."
            ));
        }

        // Check for blocked terms
        return allActiveRules().stream()
                .filter(rule -> StringUtils.hasText(rule.getTerm()))
                .filter(rule -> content.contains(normalize(rule.getTerm())))
                .map(rule -> new BlockedTermValidationResult(
                        rule.getTerm(),
                        "O anuncio contem um termo bloqueado pela plataforma."
                ))
                .max(Comparator.comparing(match -> "link".equals(match.getTerm()) ? 1 : 0));
    }

    private String normalizedContent(SellerItem item) {
        return normalize(String.join(" ",
                Optional.ofNullable(item.getTitle()).orElse(""),
                Optional.ofNullable(item.getDescription()).orElse(""),
                String.join(" ", Optional.ofNullable(item.getTags()).orElse(List.of()))));
    }

    private String normalizedContent(InterestPost interest) {
        return normalize(String.join(" ",
                Optional.ofNullable(interest.getTitle()).orElse(""),
                Optional.ofNullable(interest.getDescription()).orElse(""),
                String.join(" ", Optional.ofNullable(interest.getTags()).orElse(List.of()))));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}





