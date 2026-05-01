package com.euprocuro.api.domain.gateway;

import java.util.List;
import java.util.Optional;

import com.euprocuro.api.domain.model.ModerationRule;

public interface ModerationRuleGateway {
    ModerationRule save(ModerationRule rule);

    List<ModerationRule> findAll();

    List<ModerationRule> findByActiveTrue();

    Optional<ModerationRule> findById(String id);

    void deleteById(String id);
}
