package com.euprocuro.api.application.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.euprocuro.api.application.command.CreateInterestCommand;
import com.euprocuro.api.application.command.CreateOfferCommand;
import com.euprocuro.api.application.command.InterestSearchFilter;
import com.euprocuro.api.application.command.UpdateInterestCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ForbiddenException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.application.usecase.MarketplaceUseCase;
import com.euprocuro.api.domain.gateway.EventPublisherGateway;
import com.euprocuro.api.domain.gateway.EmailGateway;
import com.euprocuro.api.domain.gateway.InterestGateway;
import com.euprocuro.api.domain.gateway.OfferGateway;
import com.euprocuro.api.domain.gateway.RealtimeMessageGateway;
import com.euprocuro.api.domain.gateway.UserGateway;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestSearchCriteria;
import com.euprocuro.api.domain.model.InterestStatus;
import com.euprocuro.api.domain.model.LocationInfo;
import com.euprocuro.api.domain.model.Offer;
import com.euprocuro.api.domain.model.OfferStatus;
import com.euprocuro.api.domain.model.UserProfile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarketplaceService implements MarketplaceUseCase {

    private final UserGateway userGateway;
    private final InterestGateway interestGateway;
    private final OfferGateway offerGateway;
    private final EventPublisherGateway eventPublisherGateway;
    private final EmailGateway emailGateway;
    private final RealtimeMessageGateway realtimeMessageGateway;

    @Value("${application.listings.expiration-days:30}")
    private long listingExpirationDays = 30;

    @Override
    public InterestPost createInterest(String currentUserId, CreateInterestCommand command) {
        UserProfile owner = userGateway.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."));

        validateBudgetRange(command.getBudgetMin(), command.getBudgetMax());
        Instant now = Instant.now();

        InterestPost interestPost = InterestPost.builder()
                .ownerId(owner.getId())
                .ownerName(owner.getName())
                .title(command.getTitle())
                .description(command.getDescription())
                .referenceImageUrl(normalizeReferenceImage(command.getReferenceImageUrl()))
                .category(command.getCategory())
                .budgetMin(command.getBudgetMin())
                .budgetMax(command.getBudgetMax())
                .location(LocationInfo.builder()
                        .city(command.getCity())
                        .state(command.getState())
                        .neighborhood(command.getNeighborhood())
                        .remote(false)
                        .build())
                .tags(Optional.ofNullable(command.getTags()).orElse(List.of()))
                .desiredRadiusKm(command.getDesiredRadiusKm())
                .acceptsNationwideOffers(command.isAcceptsNationwideOffers())
                .allowsWhatsappContact(command.isAllowsWhatsappContact())
                .whatsappContact(command.isAllowsWhatsappContact() ? normalizeReferenceImage(command.getWhatsappContact()) : null)
                .boostEnabled(command.isBoostEnabled())
                .preferredCondition(command.getPreferredCondition())
                .preferredContactMode(command.getPreferredContactMode())
                .status(InterestStatus.OPEN)
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(expiresAt(now))
                .build();

        InterestPost saved = interestGateway.save(interestPost);
        eventPublisherGateway.publish("interest.created", Map.of(
                "interestId", saved.getId(),
                "ownerId", owner.getId(),
                "category", saved.getCategory().name(),
                "budgetMax", saved.getBudgetMax()
        ));
        return saved;
    }

    @Override
    public InterestPost closeInterest(String currentUserId, String interestId) {
        InterestPost existingInterest = getInterest(interestId);
        if (!Objects.equals(existingInterest.getOwnerId(), currentUserId)) {
            throw new ForbiddenException("Apenas o dono do interesse pode desativar esse anuncio.");
        }

        InterestPost closedInterest = existingInterest.toBuilder()
                .status(InterestStatus.CLOSED)
                .updatedAt(Instant.now())
                .build();

        InterestPost saved = interestGateway.save(closedInterest);
        eventPublisherGateway.publish("interest.closed", Map.of(
                "interestId", saved.getId(),
                "ownerId", saved.getOwnerId()
        ));
        return saved;
    }

    @Override
    public void deleteInterest(String currentUserId, String interestId) {
        InterestPost existingInterest = getInterest(interestId);
        if (!Objects.equals(existingInterest.getOwnerId(), currentUserId)) {
            throw new ForbiddenException("Apenas o dono do interesse pode excluir esse anuncio.");
        }

        interestGateway.deleteById(interestId);
        eventPublisherGateway.publish("interest.deleted", Map.of(
                "interestId", interestId,
                "ownerId", currentUserId
        ));
    }

    @Override
    public InterestPost updateInterest(String currentUserId, String interestId, UpdateInterestCommand command) {
        InterestPost existingInterest = getInterest(interestId);
        if (!Objects.equals(existingInterest.getOwnerId(), currentUserId)) {
            throw new ForbiddenException("Apenas o dono do interesse pode editar esse anuncio.");
        }

        validateBudgetRange(command.getBudgetMin(), command.getBudgetMax());

        InterestPost updatedInterest = existingInterest.toBuilder()
                .title(command.getTitle())
                .description(command.getDescription())
                .referenceImageUrl(normalizeReferenceImage(command.getReferenceImageUrl()))
                .category(command.getCategory())
                .budgetMin(command.getBudgetMin())
                .budgetMax(command.getBudgetMax())
                .location(LocationInfo.builder()
                        .city(command.getCity())
                        .state(command.getState())
                        .neighborhood(command.getNeighborhood())
                        .remote(false)
                        .build())
                .tags(Optional.ofNullable(command.getTags()).orElse(List.of()))
                .desiredRadiusKm(command.getDesiredRadiusKm())
                .acceptsNationwideOffers(command.isAcceptsNationwideOffers())
                .allowsWhatsappContact(command.isAllowsWhatsappContact())
                .whatsappContact(command.isAllowsWhatsappContact() ? normalizeReferenceImage(command.getWhatsappContact()) : null)
                .boostEnabled(command.isBoostEnabled())
                .preferredCondition(command.getPreferredCondition())
                .preferredContactMode(command.getPreferredContactMode())
                .updatedAt(Instant.now())
                .build();

        InterestPost saved = interestGateway.save(updatedInterest);
        eventPublisherGateway.publish("interest.updated", Map.of(
                "interestId", saved.getId(),
                "ownerId", saved.getOwnerId(),
                "category", saved.getCategory().name(),
                "budgetMax", saved.getBudgetMax()
        ));
        return saved;
    }

    @Override
    public InterestPost renewInterest(String currentUserId, String interestId) {
        InterestPost existingInterest = getInterest(interestId);
        if (!Objects.equals(existingInterest.getOwnerId(), currentUserId)) {
            throw new ForbiddenException("Apenas o dono do interesse pode renovar esse anuncio.");
        }

        UserProfile owner = userGateway.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."));
        int availableCredits = owner.getSellerCredits() == null ? 0 : owner.getSellerCredits();
        if (availableCredits <= 0) {
            throw new BusinessException("Voce precisa de um credito para renovar este anuncio.");
        }

        userGateway.save(owner.toBuilder()
                .sellerCredits(availableCredits - 1)
                .build());

        Instant now = Instant.now();
        Instant renewalBase = Optional.ofNullable(existingInterest.getExpiresAt())
                .filter(expiration -> expiration.isAfter(now))
                .orElse(now);
        InterestPost renewedInterest = existingInterest.toBuilder()
                .expiresAt(expiresAt(renewalBase))
                .updatedAt(now)
                .build();

        InterestPost saved = interestGateway.save(renewedInterest);
        eventPublisherGateway.publish("interest.renewed", Map.of(
                "interestId", saved.getId(),
                "ownerId", saved.getOwnerId(),
                "expiresAt", saved.getExpiresAt()
        ));
        return saved;
    }

    @Override
    public List<InterestPost> listInterests(InterestSearchFilter filter) {
        return interestGateway.findAll()
                .stream()
                .filter(this::isNotExpired)
                .filter(post -> filter.getCategory() == null || post.getCategory() == filter.getCategory())
                .filter(post -> filter.getCity() == null || filter.getCity().isBlank()
                        || equalsIgnoreCase(post.getLocation() == null ? null : post.getLocation().getCity(), filter.getCity()))
                .filter(post -> filter.getMaxBudget() == null || post.getBudgetMax() == null
                        || post.getBudgetMax().compareTo(filter.getMaxBudget()) <= 0)
                .filter(post -> filter.getQuery() == null || filter.getQuery().isBlank()
                        || containsIgnoreCase(post, filter.getQuery()))
                .filter(post -> !filter.isOpenOnly() || post.getStatus() == InterestStatus.OPEN)
                .sorted(Comparator
                        .comparing(this::isBoostActive).reversed()
                        .thenComparing(InterestPost::getCreatedAt, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public List<InterestPost> listInterests(InterestSearchFilter filter, int offset, int limit) {
        return interestGateway.search(InterestSearchCriteria.builder()
                .category(filter.getCategory())
                .city(filter.getCity())
                .maxBudget(filter.getMaxBudget())
                .query(filter.getQuery())
                .openOnly(filter.isOpenOnly())
                .build(), Math.max(0, offset), Math.max(1, Math.min(limit, 50)))
                .stream()
                .filter(this::isNotExpired)
                .collect(Collectors.toList());
    }

    @Override
    public InterestPost getInterest(String id) {
        InterestPost interest = interestGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interesse nao encontrado."));
        if (isExpired(interest)) {
            interestGateway.deleteById(id);
            throw new ResourceNotFoundException("Interesse expirado.");
        }
        return interest;
    }

    @Override
    public Offer createOffer(String currentUserId, String interestId, CreateOfferCommand command) {
        InterestPost interestPost = getInterest(interestId);
        if (interestPost.getStatus() != InterestStatus.OPEN) {
            throw new BusinessException("Este interesse não está mais aberto.");
        }

        UserProfile seller = userGateway.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendedor nao encontrado."));

        if (Objects.equals(interestPost.getOwnerId(), seller.getId())) {
            throw new BusinessException("O mesmo usuario nao pode ofertar para si.");
        }

        boolean hasActivePlan = seller.getSubscriptionActiveUntil() != null
                && seller.getSubscriptionActiveUntil().isAfter(Instant.now());
        if (!hasActivePlan) {
            int availableCredits = seller.getSellerCredits() == null ? 0 : seller.getSellerCredits();
            if (availableCredits <= 0) {
                throw new BusinessException("Voce precisa de creditos ou de um plano ativo para enviar propostas.");
            }
            seller = userGateway.save(seller.toBuilder()
                    .sellerCredits(availableCredits - 1)
                    .build());
        }

        Offer offer = Offer.builder()
                .interestPostId(interestId)
                .sellerId(seller.getId())
                .sellerName(seller.getName())
                .sellerEmail(seller.getEmail())
                .sellerPhone(command.getSellerPhone())
                .offeredPrice(command.getOfferedPrice())
                .message(command.getMessage())
                .offerImageUrl(normalizeReferenceImage(command.getOfferImageUrl()))
                .includesDelivery(command.isIncludesDelivery())
                .highlights(Optional.ofNullable(command.getHighlights()).orElse(List.of()))
                .status(OfferStatus.SENT)
                .createdAt(Instant.now())
                .build();

        Offer saved = offerGateway.save(offer);
        String sellerName = seller.getName();
        userGateway.findById(interestPost.getOwnerId())
                .ifPresent(owner -> emailGateway.sendOfferReceivedEmail(owner, interestPost.getTitle(), sellerName));
        eventPublisherGateway.publish("offer.created", Map.of(
                "offerId", saved.getId(),
                "interestId", interestId,
                "sellerId", seller.getId(),
                "ownerId", interestPost.getOwnerId(),
                "offeredPrice", saved.getOfferedPrice()
        ));
        realtimeMessageGateway.publishOfferCreated(interestPost.getOwnerId(), saved.getId());
        return saved;
    }

    @Override
    public List<Offer> listOffersByInterest(String currentUserId, String interestId) {
        InterestPost interest = getInterest(interestId);
        if (!Objects.equals(interest.getOwnerId(), currentUserId)) {
            throw new ForbiddenException("Apenas o dono do interesse pode visualizar essas propostas.");
        }

        return offerGateway.findByInterestPostIdOrderByCreatedAtDesc(interestId);
    }

    private void validateBudgetRange(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BusinessException("O orcamento minimo nao pode ser maior que o maximo.");
        }
    }

    private boolean containsIgnoreCase(InterestPost post, String query) {
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        List<String> tags = Optional.ofNullable(post.getTags()).orElse(List.of());
        return safe(post.getTitle()).contains(normalizedQuery)
                || safe(post.getDescription()).contains(normalizedQuery)
                || safe(post.getOwnerName()).contains(normalizedQuery)
                || safe(post.getLocation() == null ? null : post.getLocation().getCity()).contains(normalizedQuery)
                || tags.stream().map(this::safe).anyMatch(tag -> tag.contains(normalizedQuery));
    }

    private boolean isBoostActive(InterestPost post) {
        return post.isBoostEnabled()
                && post.getBoostedUntil() != null
                && post.getBoostedUntil().isAfter(Instant.now());
    }

    private boolean isNotExpired(InterestPost post) {
        return !isExpired(post);
    }

    private boolean isExpired(InterestPost post) {
        Instant expiration = resolveExpiresAt(post.getExpiresAt(), post.getCreatedAt());
        return expiration != null && !expiration.isAfter(Instant.now());
    }

    private Instant expiresAt(Instant createdAt) {
        return createdAt == null ? null : createdAt.plus(safeExpirationDays(), ChronoUnit.DAYS);
    }

    private Instant resolveExpiresAt(Instant expiresAt, Instant createdAt) {
        return expiresAt != null ? expiresAt : expiresAt(createdAt);
    }

    private long safeExpirationDays() {
        return Math.max(1, listingExpirationDays);
    }

    private String normalizeReferenceImage(String referenceImageUrl) {
        if (!StringUtils.hasText(referenceImageUrl)) {
            return null;
        }

        return referenceImageUrl.trim();
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
