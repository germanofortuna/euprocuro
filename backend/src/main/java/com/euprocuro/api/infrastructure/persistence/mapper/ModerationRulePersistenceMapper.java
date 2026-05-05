package com.euprocuro.api.infrastructure.persistence.mapper;

import com.euprocuro.api.domain.model.ModerationRule;
import com.euprocuro.api.infrastructure.persistence.document.ModerationRuleDocument;

public final class ModerationRulePersistenceMapper {

    private ModerationRulePersistenceMapper() {
    }

    public static ModerationRule toDomain(ModerationRuleDocument document) {
        if (document == null) {
            return null;
        }

        return ModerationRule.builder()
                .id(document.getId())
                .term(document.getTerm())
                .riskLevel(document.getRiskLevel())
                .active(document.isActive())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    public static ModerationRuleDocument toDocument(ModerationRule domain) {
        if (domain == null) {
            return null;
        }

        return ModerationRuleDocument.builder()
                .id(domain.getId())
                .term(domain.getTerm())
                .riskLevel(domain.getRiskLevel())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
