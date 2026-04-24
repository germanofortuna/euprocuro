package com.euprocuro.api.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.OfferGateway;
import com.euprocuro.api.domain.model.Offer;
import com.euprocuro.api.infrastructure.persistence.mapper.OfferPersistenceMapper;
import com.euprocuro.api.infrastructure.persistence.repository.SpringDataOfferRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OfferGatewayAdapter implements OfferGateway {

    private final SpringDataOfferRepository repository;

    @Override
    public Offer save(Offer offer) {
        return OfferPersistenceMapper.toDomain(repository.save(OfferPersistenceMapper.toDocument(offer)));
    }

    @Override
    public List<Offer> findByInterestPostIdOrderByCreatedAtDesc(String interestPostId) {
        return repository.findByInterestPostIdOrderByCreatedAtDesc(interestPostId)
                .stream()
                .map(OfferPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Offer> findByInterestPostIdInOrderByCreatedAtDesc(List<String> interestPostIds) {
        return repository.findByInterestPostIdInOrderByCreatedAtDesc(interestPostIds)
                .stream()
                .map(OfferPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Offer> findBySellerIdOrderByCreatedAtDesc(String sellerId) {
        return repository.findBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream()
                .map(OfferPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Offer> findById(String offerId) {
        return repository.findById(offerId).map(OfferPersistenceMapper::toDomain);
    }

    @Override
    public long count() {
        return repository.count();
    }
}
