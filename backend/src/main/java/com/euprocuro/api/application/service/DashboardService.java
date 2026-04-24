package com.euprocuro.api.application.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.application.usecase.DashboardUseCase;
import com.euprocuro.api.application.view.DashboardOfferView;
import com.euprocuro.api.application.view.PersonalDashboardView;
import com.euprocuro.api.domain.gateway.InterestGateway;
import com.euprocuro.api.domain.gateway.OfferGateway;
import com.euprocuro.api.domain.gateway.UserGateway;
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

    @Override
    public PersonalDashboardView getDashboard(String userId) {
        UserProfile user = userGateway.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."));

        List<InterestPost> myInterests = interestGateway.findByOwnerIdOrderByCreatedAtDesc(userId);
        Map<String, InterestPost> interestsById = myInterests.stream()
                .collect(Collectors.toMap(InterestPost::getId, Function.identity()));

        List<Offer> offersReceived = myInterests.isEmpty()
                ? List.of()
                : offerGateway.findByInterestPostIdInOrderByCreatedAtDesc(
                        myInterests.stream().map(InterestPost::getId).collect(Collectors.toList())
                );

        List<Offer> offersSent = offerGateway.findBySellerIdOrderByCreatedAtDesc(userId);
        Map<String, InterestPost> referencedInterests = interestGateway.findAll()
                .stream()
                .collect(Collectors.toMap(InterestPost::getId, Function.identity()));

        return PersonalDashboardView.builder()
                .user(user)
                .totalActiveInterests(myInterests.stream().filter(post -> post.getStatus() == InterestStatus.OPEN).count())
                .totalOffersSent(offersSent.size())
                .totalOffersReceived(offersReceived.size())
                .myInterests(myInterests)
                .offersSent(offersSent.stream()
                        .map(offer -> toView(offer, referencedInterests.get(offer.getInterestPostId())))
                        .collect(Collectors.toList()))
                .offersReceived(offersReceived.stream()
                        .map(offer -> toView(offer, interestsById.get(offer.getInterestPostId())))
                        .collect(Collectors.toList()))
                .build();
    }

    private DashboardOfferView toView(Offer offer, InterestPost interest) {
        return DashboardOfferView.builder()
                .id(offer.getId())
                .interestPostId(offer.getInterestPostId())
                .interestTitle(interest == null ? "Interesse removido" : interest.getTitle())
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
                .build();
    }
}
