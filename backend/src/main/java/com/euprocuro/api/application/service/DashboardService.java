package com.euprocuro.api.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.application.usecase.DashboardUseCase;
import com.euprocuro.api.application.view.DashboardOfferView;
import com.euprocuro.api.application.view.PersonalDashboardView;
import com.euprocuro.api.domain.gateway.ConversationMessageGateway;
import com.euprocuro.api.domain.gateway.InterestGateway;
import com.euprocuro.api.domain.gateway.OfferGateway;
import com.euprocuro.api.domain.gateway.UserGateway;
import com.euprocuro.api.domain.model.ConversationMessage;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestStatus;
import com.euprocuro.api.domain.model.Offer;
import com.euprocuro.api.domain.model.UserProfile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService implements DashboardUseCase {

    private final UserGateway userGateway;
    private final InterestGateway interestGateway;
    private final OfferGateway offerGateway;
    private final ConversationMessageGateway conversationMessageGateway;

    @Value("${application.listings.expiration-days:30}")
    private long listingExpirationDays = 30;

    @Override
    public PersonalDashboardView getDashboard(String userId) {
        UserProfile user = userGateway.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."));

        List<InterestPost> myInterests = interestGateway.findByOwnerIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(this::isNotExpired)
                .collect(Collectors.toList());
        Map<String, InterestPost> interestsById = myInterests.stream()
                .collect(Collectors.toMap(InterestPost::getId, Function.identity()));

        List<Offer> offersReceived = myInterests.isEmpty()
                ? List.of()
                : offerGateway.findByInterestPostIdInOrderByCreatedAtDesc(
                        myInterests.stream().map(InterestPost::getId).collect(Collectors.toList())
                );

        List<Offer> offersSent = offerGateway.findBySellerIdOrderByCreatedAtDesc(userId);
        Map<String, ConversationMessage> latestMessagesByOfferId = latestMessagesByOfferId(offersReceived, offersSent);
        Map<String, InterestPost> referencedInterests = interestGateway.findAll()
                .stream()
                .filter(this::isNotExpired)
                .collect(Collectors.toMap(InterestPost::getId, Function.identity()));

        return PersonalDashboardView.builder()
                .user(user)
                .totalActiveInterests(myInterests.stream().filter(post -> post.getStatus() == InterestStatus.OPEN).count())
                .totalOffersSent(offersSent.size())
                .totalOffersReceived(offersReceived.size())
                .myInterests(myInterests)
                .offersSent(offersSent.stream()
                        .map(offer -> toView(offer, referencedInterests.get(offer.getInterestPostId()),
                                latestMessagesByOfferId.get(offer.getId())))
                        .collect(Collectors.toList()))
                .offersReceived(offersReceived.stream()
                        .map(offer -> toView(offer, interestsById.get(offer.getInterestPostId()),
                                latestMessagesByOfferId.get(offer.getId())))
                        .collect(Collectors.toList()))
                .build();
    }

    private Map<String, ConversationMessage> latestMessagesByOfferId(List<Offer> offersReceived, List<Offer> offersSent) {
        List<String> offerIds = Stream.concat(offersReceived.stream(), offersSent.stream())
                .map(Offer::getId)
                .collect(Collectors.toList());
        if (offerIds.isEmpty()) {
            return Map.of();
        }

        return conversationMessageGateway.findByOfferIdInOrderByCreatedAtAsc(offerIds)
                .stream()
                .collect(Collectors.toMap(
                        ConversationMessage::getOfferId,
                        Function.identity(),
                        (current, next) -> Optional.ofNullable(next.getCreatedAt()).orElse(Instant.EPOCH)
                                .isAfter(Optional.ofNullable(current.getCreatedAt()).orElse(Instant.EPOCH))
                                ? next
                                : current
                ));
    }

    private DashboardOfferView toView(Offer offer, InterestPost interest, ConversationMessage latestMessage) {
        return DashboardOfferView.builder()
                .id(offer.getId())
                .interestPostId(offer.getInterestPostId())
                .interestTitle(interest == null ? "Interesse removido" : interest.getTitle())
                .referenceImageUrl(interest == null ? null : interest.getReferenceImageUrl())
                .offerImageUrl(offer.getOfferImageUrl())
                .buyerId(interest == null ? null : interest.getOwnerId())
                .sellerName(offer.getSellerName())
                .sellerEmail(offer.getSellerEmail())
                .sellerPhone(offer.getSellerPhone())
                .buyerName(interest == null ? null : interest.getOwnerName())
                .offeredPrice(offer.getOfferedPrice())
                .message(offer.getMessage())
                .includesDelivery(offer.isIncludesDelivery())
                .highlights(offer.getHighlights())
                .status(offer.getStatus())
                .createdAt(offer.getCreatedAt())
                .latestMessage(latestMessage == null ? null : latestMessage.getContent())
                .latestMessageSenderId(latestMessage == null ? null : latestMessage.getSenderId())
                .latestMessageAt(latestMessage == null ? null : latestMessage.getCreatedAt())
                .build();
    }

    private boolean isNotExpired(InterestPost interest) {
        Instant expiration = interest.getExpiresAt() != null
                ? interest.getExpiresAt()
                : interest.getCreatedAt() == null
                ? null
                : interest.getCreatedAt().plus(Math.max(1, listingExpirationDays), ChronoUnit.DAYS);
        return expiration == null || expiration.isAfter(Instant.now());
    }
}
