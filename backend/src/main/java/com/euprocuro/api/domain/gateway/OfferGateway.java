package com.euprocuro.api.domain.gateway;

import java.util.List;
import java.util.Optional;

import com.euprocuro.api.domain.model.Offer;

public interface OfferGateway {
    Offer save(Offer offer);

    List<Offer> findByInterestPostIdOrderByCreatedAtDesc(String interestPostId);

    List<Offer> findByInterestPostIdInOrderByCreatedAtDesc(List<String> interestPostIds);

    List<Offer> findBySellerIdOrderByCreatedAtDesc(String sellerId);

    Optional<Offer> findById(String offerId);

    long count();
}
