package com.euprocuro.api.infrastructure.persistence.mapper;

import java.util.ArrayList;

import com.euprocuro.api.domain.model.Offer;
import com.euprocuro.api.infrastructure.persistence.document.OfferDocument;

public final class OfferPersistenceMapper {

    private OfferPersistenceMapper() {
    }

    public static Offer toDomain(OfferDocument document) {
        if (document == null) {
            return null;
        }

        return Offer.builder()
                .id(document.getId())
                .interestPostId(document.getInterestPostId())
                .sellerId(document.getSellerId())
                .sellerName(document.getSellerName())
                .sellerEmail(document.getSellerEmail())
                .sellerPhone(document.getSellerPhone())
                .offeredPrice(document.getOfferedPrice())
                .message(document.getMessage())
                .offerImageUrl(document.getOfferImageUrl())
                .includesDelivery(document.isIncludesDelivery())
                .highlights(document.getHighlights() == null ? new ArrayList<>() : document.getHighlights())
                .status(document.getStatus())
                .createdAt(document.getCreatedAt())
                .build();
    }

    public static OfferDocument toDocument(Offer domain) {
        if (domain == null) {
            return null;
        }

        return OfferDocument.builder()
                .id(domain.getId())
                .interestPostId(domain.getInterestPostId())
                .sellerId(domain.getSellerId())
                .sellerName(domain.getSellerName())
                .sellerEmail(domain.getSellerEmail())
                .sellerPhone(domain.getSellerPhone())
                .offeredPrice(domain.getOfferedPrice())
                .message(domain.getMessage())
                .offerImageUrl(domain.getOfferImageUrl())
                .includesDelivery(domain.isIncludesDelivery())
                .highlights(domain.getHighlights())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
