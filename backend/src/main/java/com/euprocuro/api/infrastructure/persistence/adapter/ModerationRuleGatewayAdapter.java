package com.euprocuro.api.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.ModerationRuleGateway;
import com.euprocuro.api.domain.model.ModerationRule;
import com.euprocuro.api.infrastructure.persistence.mapper.ModerationRulePersistenceMapper;
import com.euprocuro.api.infrastructure.persistence.repository.SpringDataModerationRuleRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ModerationRuleGatewayAdapter implements ModerationRuleGateway {

    private final SpringDataModerationRuleRepository repository;

    @Override
    public ModerationRule save(ModerationRule rule) {
        return ModerationRulePersistenceMapper.toDomain(repository.save(ModerationRulePersistenceMapper.toDocument(rule)));
    }

    @Override
    public List<ModerationRule> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"))
                .stream()
                .map(ModerationRulePersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ModerationRule> findByActiveTrue() {
        return repository.findByActiveTrueOrderByUpdatedAtDesc()
                .stream()
                .map(ModerationRulePersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ModerationRule> findById(String id) {
        return repository.findById(id).map(ModerationRulePersistenceMapper::toDomain);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
