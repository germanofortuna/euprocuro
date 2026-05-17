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
import com.euprocuro.api.domain.gateway.BlockedTermValidationGateway;
import com.euprocuro.api.domain.gateway.InterestGateway;
import com.euprocuro.api.domain.gateway.InterestSearchGateway;
import com.euprocuro.api.domain.gateway.OfferGateway;
import com.euprocuro.api.domain.gateway.RealtimeMessageGateway;
import com.euprocuro.api.domain.gateway.SellerItemGateway;
import com.euprocuro.api.domain.gateway.UserGateway;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestSearchCriteria;
import com.euprocuro.api.domain.model.InterestStatus;
import com.euprocuro.api.domain.model.LocationInfo;
import com.euprocuro.api.domain.model.Offer;
import com.euprocuro.api.domain.model.OfferStatus;
import com.euprocuro.api.domain.model.SellerItem;
import com.euprocuro.api.domain.model.StickerDetails;
import com.euprocuro.api.domain.model.StickerListingType;
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
    private final OperationalCatalogService operationalCatalogService;
    private final InterestSearchGateway interestSearchGateway;
    private final PublicCacheService publicCacheService;
    private final AuditLogService auditLogService;
    private final SellerItemGateway sellerItemGateway;
    private final InterestDeliveryRankingService interestDeliveryRankingService;
    private final BlockedTermValidationGateway blockedTermValidationGateway;

    @Value("${application.listings.expiration-days:30}")
    private long listingExpirationDays = 30;
    @Value("${application.listings.renewal-days:30}")
    private long listingRenewalDays = 30;
    @Value("${application.cache.public.marketplace-ttl-seconds:60}")
    private long marketplaceCacheTtlSeconds = 60;

    @Override
    public InterestPost createInterest(String currentUserId, CreateInterestCommand command) {
        UserProfile owner = userGateway.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."));

        validateBudgetRange(command.getBudgetMin(), command.getBudgetMax());
        String category = operationalCatalogService.requireActiveCategory(command.getCategory());
        StickerDetails stickerDetails = normalizeStickerDetails(category, command.getStickerDetails());
        Instant now = Instant.now();

        InterestPost interestPost = InterestPost.builder()
                .ownerId(owner.getId())
                .ownerName(owner.getName())
                .title(command.getTitle())
                .description(command.getDescription())
                .referenceImageUrl(normalizeReferenceImage(command.getReferenceImageUrl()))
                .category(category)
                .budgetMin(command.getBudgetMin())
                .budgetMax(command.getBudgetMax())
                .location(LocationInfo.builder()
                        .postalCode(normalizePostalCode(command.getPostalCode()))
                        .city(command.getCity())
                        .state(command.getState())
                        .neighborhood(command.getNeighborhood())
                        .country(normalizeCountry(command.getCountry()))
                        .remote(false)
                        .build())
                .tags(Optional.ofNullable(command.getTags()).orElse(List.of()))
                .stickerDetails(stickerDetails)
                .desiredRadiusKm(command.getDesiredRadiusKm())
                .allowsWhatsappContact(command.isAllowsWhatsappContact())
                .whatsappContact(command.isAllowsWhatsappContact() ? normalizeReferenceImage(command.getWhatsappContact()) : null)
                .preferredCondition(command.getPreferredCondition())
                .preferredContactMode(command.getPreferredContactMode())
                .status(InterestStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(expiresAt(now))
                .build();

        InterestPost saved = interestGateway.save(interestPost);
        auditLogService.record("INTEREST_CREATED", owner.getId(), owner.getEmail(), "INTEREST", saved.getId(),
                AuditLogService.OUTCOME_SUCCESS, Map.of("category", saved.getCategory()));
        publicCacheService.invalidate(PublicCacheService.MARKETPLACE);
        eventPublisherGateway.publish("interest.created", Map.of(
                "interestId", saved.getId(),
                "ownerId", owner.getId(),
                "category", saved.getCategory(),
                "budgetMax", saved.getBudgetMax()
        ));
        publishModerationRequest(saved);
        return saved;
    }

    @Override
    public InterestPost closeInterest(String currentUserId, String interestId) {
        InterestPost existingInterest = loadInterest(interestId);
        if (!Objects.equals(existingInterest.getOwnerId(), currentUserId)) {
            throw new ForbiddenException("Apenas o dono do interesse pode desativar esse anuncio.");
        }

        InterestPost closedInterest = existingInterest.toBuilder()
                .status(InterestStatus.CLOSED)
                .updatedAt(Instant.now())
                .build();

        InterestPost saved = interestGateway.save(closedInterest);
        auditLogService.record("INTEREST_CLOSED", currentUserId, null, "INTEREST", saved.getId());
        publicCacheService.invalidate(PublicCacheService.MARKETPLACE);
        eventPublisherGateway.publish("interest.closed", Map.of(
                "interestId", saved.getId(),
                "ownerId", saved.getOwnerId()
        ));
        return saved;
    }

    @Override
    public InterestPost activateInterest(String currentUserId, String interestId) {
        InterestPost existingInterest = loadInterest(interestId);
        if (!Objects.equals(existingInterest.getOwnerId(), currentUserId)) {
            throw new ForbiddenException("Apenas o dono do interesse pode ativar esse anuncio.");
        }

        if (existingInterest.getStatus() != InterestStatus.CLOSED) {
            throw new BusinessException("Apenas anuncios desativados podem ser ativados novamente.");
        }

        InterestPost activatedInterest = existingInterest.toBuilder()
                .status(InterestStatus.PENDING)
                .moderation(null)
                .updatedAt(Instant.now())
                .build();

        InterestPost saved = interestGateway.save(activatedInterest);
        auditLogService.record("INTEREST_ACTIVATED", currentUserId, null, "INTEREST", saved.getId());
        publicCacheService.invalidate(PublicCacheService.MARKETPLACE);
        eventPublisherGateway.publish("interest.activated", Map.of(
                "interestId", saved.getId(),
                "ownerId", saved.getOwnerId()
        ));
        publishModerationRequest(saved);
        return saved;
    }

    @Override
    public void deleteInterest(String currentUserId, String interestId) {
        InterestPost existingInterest = loadInterest(interestId);
        if (!Objects.equals(existingInterest.getOwnerId(), currentUserId)) {
            throw new ForbiddenException("Apenas o dono do interesse pode excluir esse anuncio.");
        }

        interestGateway.deleteById(interestId);
        auditLogService.record("INTEREST_DELETED", currentUserId, null, "INTEREST", interestId);
        publicCacheService.invalidate(PublicCacheService.MARKETPLACE);
        eventPublisherGateway.publish("interest.deleted", Map.of(
                "interestId", interestId,
                "ownerId", currentUserId
        ));
    }

    @Override
    public InterestPost updateInterest(String currentUserId, String interestId, UpdateInterestCommand command) {
        InterestPost existingInterest = loadInterest(interestId);
        if (!Objects.equals(existingInterest.getOwnerId(), currentUserId)) {
            throw new ForbiddenException("Apenas o dono do interesse pode editar esse anuncio.");
        }

        validateBudgetRange(command.getBudgetMin(), command.getBudgetMax());
        String category = operationalCatalogService.requireActiveCategory(command.getCategory());
        StickerDetails stickerDetails = normalizeStickerDetails(category, command.getStickerDetails());

        InterestPost updatedInterest = existingInterest.toBuilder()
                .title(command.getTitle())
                .description(command.getDescription())
                .referenceImageUrl(normalizeReferenceImage(command.getReferenceImageUrl()))
                .category(category)
                .budgetMin(command.getBudgetMin())
                .budgetMax(command.getBudgetMax())
                .location(LocationInfo.builder()
                        .postalCode(normalizePostalCode(command.getPostalCode()))
                        .city(command.getCity())
                        .state(command.getState())
                        .neighborhood(command.getNeighborhood())
                        .country(normalizeCountry(command.getCountry()))
                        .remote(false)
                        .build())
                .tags(Optional.ofNullable(command.getTags()).orElse(List.of()))
                .stickerDetails(stickerDetails)
                .desiredRadiusKm(command.getDesiredRadiusKm())
                .allowsWhatsappContact(command.isAllowsWhatsappContact())
                .whatsappContact(command.isAllowsWhatsappContact() ? normalizeReferenceImage(command.getWhatsappContact()) : null)
                .preferredCondition(command.getPreferredCondition())
                .preferredContactMode(command.getPreferredContactMode())
                .status(InterestStatus.PENDING)
                .moderation(null)
                .updatedAt(Instant.now())
                .build();

        InterestPost saved = interestGateway.save(updatedInterest);
        auditLogService.record("INTEREST_UPDATED", currentUserId, null, "INTEREST", saved.getId(),
                AuditLogService.OUTCOME_SUCCESS, Map.of("category", saved.getCategory()));
        publicCacheService.invalidate(PublicCacheService.MARKETPLACE);
        eventPublisherGateway.publish("interest.updated", Map.of(
                "interestId", saved.getId(),
                "ownerId", saved.getOwnerId(),
                "category", saved.getCategory(),
                "budgetMax", saved.getBudgetMax()
        ));
        publishModerationRequest(saved);
        return saved;
    }

    @Override
    public InterestPost renewInterest(String currentUserId, String interestId) {
        InterestPost existingInterest = loadInterest(interestId);
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
                .expiresAt(renewalExpiresAt(renewalBase))
                .updatedAt(now)
                .build();

        InterestPost saved = interestGateway.save(renewedInterest);
        auditLogService.record("INTEREST_RENEWED", currentUserId, owner.getEmail(), "INTEREST", saved.getId(),
                AuditLogService.OUTCOME_SUCCESS, Map.of("creditsRemaining", availableCredits - 1));
        publicCacheService.invalidate(PublicCacheService.MARKETPLACE);
        eventPublisherGateway.publish("interest.renewed", Map.of(
                "interestId", saved.getId(),
                "ownerId", saved.getOwnerId(),
                "expiresAt", saved.getExpiresAt()
        ));
        return saved;
    }

    @Override
    public List<InterestPost> listInterests(InterestSearchFilter filter) {
        List<InterestPost> candidates = interestGateway.findAll()
                .stream()
                .filter(this::isNotExpired)
                .filter(post -> filter.getCategory() == null || Objects.equals(post.getCategory(), filter.getCategory()))
                .filter(post -> filter.getCity() == null || filter.getCity().isBlank()
                        || safe(post.getLocation() == null ? null : post.getLocation().getCity()).contains(safe(filter.getCity())))
                .filter(post -> filter.getState() == null || filter.getState().isBlank()
                        || equalsIgnoreCase(post.getLocation() == null ? null : post.getLocation().getState(), filter.getState()))
                .filter(post -> filter.getNeighborhood() == null || filter.getNeighborhood().isBlank()
                        || safe(post.getLocation() == null ? null : post.getLocation().getNeighborhood()).contains(safe(filter.getNeighborhood())))
                .filter(post -> filter.getMaxBudget() == null || post.getBudgetMax() == null
                        || post.getBudgetMax().compareTo(filter.getMaxBudget()) <= 0)
                .filter(post -> filter.getQuery() == null || filter.getQuery().isBlank()
                        || containsIgnoreCase(post, filter.getQuery()))
                .filter(post -> matchesStickerFilters(post, filter))
                .filter(this::isVisibleForEnabledFeatures)
                .filter(post -> !filter.isOpenOnly() || isPubliclyVisible(post))
                .filter(post -> isNotOwnedByCurrentUser(post, filter.getCurrentUserId()))
                .collect(Collectors.toList());

        return rankForDelivery(candidates, filter.getCurrentUserId());
    }

    @Override
    public List<InterestPost> listInterests(InterestSearchFilter filter, int offset, int limit) {
        InterestSearchCriteria criteria = InterestSearchCriteria.builder()
                .category(filter.getCategory())
                .city(filter.getCity())
                .state(filter.getState())
                .neighborhood(filter.getNeighborhood())
                .maxBudget(filter.getMaxBudget())
                .query(filter.getQuery())
                .stickerType(filter.getStickerType())
                .stickerGroup(filter.getStickerGroup())
                .stickerSelection(filter.getStickerSelection())
                .stickerNumber(filter.getStickerNumber())
                .openOnly(true)
                .build();
        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.max(1, Math.min(limit, 50));

        if (StringUtils.hasText(filter.getCurrentUserId())) {
            return searchPersonalizedInterests(criteria, safeOffset, safeLimit, filter.getCurrentUserId());
        }

        return publicCacheService.getOrLoad(
                PublicCacheService.MARKETPLACE,
                interestSearchCacheKey(criteria, safeOffset, safeLimit),
                marketplaceCacheTtlSeconds,
                () -> searchInterests(criteria, safeOffset, safeLimit)
        );
    }

    private List<InterestPost> searchPersonalizedInterests(InterestSearchCriteria criteria, int offset, int limit, String currentUserId) {
        int candidateLimit = offset + Math.max(100, limit * 8);
        List<InterestPost> candidateInterests = searchInterests(criteria, 0, candidateLimit)
                .stream()
                .filter(post -> isNotOwnedByCurrentUser(post, currentUserId))
                .collect(Collectors.toList());

        return rankForDelivery(candidateInterests, currentUserId)
                .stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<InterestPost> rankForDelivery(List<InterestPost> interests, String currentUserId) {
        if (!StringUtils.hasText(currentUserId)) {
            return interests.stream()
                    .sorted(Comparator
                            .comparing(this::isBoostActive).reversed()
                            .thenComparing(InterestPost::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        }

        Optional<UserProfile> user = userGateway.findById(currentUserId);
        List<SellerItem> sellerItems = sellerItemGateway.findByOwnerIdOrderByCreatedAtDesc(currentUserId)
                .stream()
                .filter(SellerItem::isActive)
                .collect(Collectors.toList());
        return interestDeliveryRankingService.rank(interests, user, sellerItems);
    }

    private List<InterestPost> searchInterests(InterestSearchCriteria criteria, int offset, int limit) {
        return interestSearchGateway.search(criteria, offset, limit)
                .stream()
                .filter(this::isVisibleForEnabledFeatures)
                .filter(this::isNotExpired)
                .filter(this::isPubliclyVisible)
                .collect(Collectors.toList());
    }

    @Override
    public InterestPost getInterest(String id) {
        InterestPost interest = loadInterest(id);
        if (!isVisibleForEnabledFeatures(interest) || !isPubliclyVisible(interest)) {
            throw new ResourceNotFoundException("Interesse nao encontrado.");
        }
        return interest;
    }

    private InterestPost loadInterest(String id) {
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
        InterestPost interestPost = loadInterest(interestId);
        if (!isPubliclyVisible(interestPost)) {
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

        String message = trimToNull(command.getMessage());
        String offerImageUrl = normalizeReferenceImage(command.getOfferImageUrl());
        if (!StringUtils.hasText(message) && !StringUtils.hasText(offerImageUrl)) {
            throw new BusinessException("A mensagem nao pode estar em branco.");
        }

        Offer offer = Offer.builder()
                .interestPostId(interestId)
                .sellerId(seller.getId())
                .sellerName(seller.getName())
                .sellerEmail(seller.getEmail())
                .sellerPhone(trimToNull(command.getSellerPhone()))
                .offeredPrice(command.getOfferedPrice())
                .message(message)
                .offerImageUrl(offerImageUrl)
                .includesDelivery(command.isIncludesDelivery())
                .highlights(Optional.ofNullable(command.getHighlights()).orElse(List.of()))
                .status(OfferStatus.SENT)
                .createdAt(Instant.now())
                .build();

        Offer saved = offerGateway.save(offer);
        auditLogService.record("OFFER_CREATED", seller.getId(), seller.getEmail(), "OFFER", saved.getId(),
                AuditLogService.OUTCOME_SUCCESS, Map.of("interestId", interestId));
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
        InterestPost interest = loadInterest(interestId);
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

    private StickerDetails normalizeStickerDetails(String category, com.euprocuro.api.application.command.StickerDetailsCommand command) {
        if (!OperationalCatalogService.STICKERS_CATEGORY_CODE.equals(category)) {
            return null;
        }
        if (!operationalCatalogService.stickersPageEnabled()) {
            throw new BusinessException("Pagina de figurinhas esta temporariamente desabilitada.");
        }
        if (command == null || command.getType() == null) {
            throw new BusinessException("Informe se voce procura ou tem figurinhas repetidas.");
        }
        List<String> numbers = Optional.ofNullable(command.getNumbers()).orElse(List.of())
                .stream()
                .map(this::normalizeStickerNumber)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (numbers.isEmpty()) {
            throw new BusinessException("Informe pelo menos uma figurinha.");
        }
        return StickerDetails.builder()
                .type(command.getType())
                .group(normalizeStickerGroup(command.getGroup()))
                .selection(trimToNull(command.getSelection()))
                .numbers(numbers)
                .build();
    }

    private String normalizeStickerGroup(String group) {
        String normalized = trimToNull(group);
        if (normalized == null) {
            return "SPECIAL";
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if ("ESPECIAIS".equals(normalized)) {
            return "SPECIAL";
        }
        return normalized;
    }

    private String normalizeStickerNumber(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private boolean matchesStickerFilters(InterestPost post, InterestSearchFilter filter) {
        if (!hasStickerFilter(filter)) {
            return true;
        }
        StickerDetails details = post.getStickerDetails();
        if (details == null) {
            return false;
        }
        if (StringUtils.hasText(filter.getStickerType())) {
            StickerListingType type = parseStickerType(filter.getStickerType());
            if (type != null && details.getType() != type) {
                return false;
            }
        }
        if (StringUtils.hasText(filter.getStickerGroup())
                && !normalizeStickerGroup(filter.getStickerGroup()).equalsIgnoreCase(safe(details.getGroup()))) {
            return false;
        }
        if (StringUtils.hasText(filter.getStickerSelection())
                && !filter.getStickerSelection().trim().equalsIgnoreCase(safe(details.getSelection()))) {
            return false;
        }
        if (StringUtils.hasText(filter.getStickerNumber())) {
            String normalizedNumber = normalizeStickerNumber(filter.getStickerNumber());
            return Optional.ofNullable(details.getNumbers()).orElse(List.of()).stream()
                    .map(this::normalizeStickerNumber)
                    .anyMatch(normalizedNumber::equals);
        }
        return true;
    }

    private boolean hasStickerFilter(InterestSearchFilter filter) {
        return StringUtils.hasText(filter.getStickerType())
                || StringUtils.hasText(filter.getStickerGroup())
                || StringUtils.hasText(filter.getStickerSelection())
                || StringUtils.hasText(filter.getStickerNumber());
    }

    private StickerListingType parseStickerType(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("FALTANTES".equals(normalized) || "MISSING".equals(normalized)) {
            return StickerListingType.MISSING;
        }
        if ("REPETIDAS".equals(normalized) || "AVAILABLE".equals(normalized)) {
            return StickerListingType.AVAILABLE;
        }
        return null;
    }

    private boolean isVisibleForEnabledFeatures(InterestPost post) {
        return post == null
                || !OperationalCatalogService.STICKERS_CATEGORY_CODE.equals(post.getCategory())
                || operationalCatalogService.stickersPageEnabled();
    }

    private boolean isBoostActive(InterestPost post) {
        return post.getBoostedUntil() != null
                && post.getBoostedUntil().isAfter(Instant.now());
    }

    private boolean isNotExpired(InterestPost post) {
        return !isExpired(post);
    }

    private boolean isNotOwnedByCurrentUser(InterestPost post, String currentUserId) {
        return !StringUtils.hasText(currentUserId) || !Objects.equals(post.getOwnerId(), currentUserId);
    }

    private boolean isPubliclyVisible(InterestPost post) {
        if (post == null || (post.getStatus() != InterestStatus.OPEN && post.getStatus() != InterestStatus.APPROVED)) {
            return false;
        }

        if (post.getModeration() != null
                && (post.getModeration().isFlagged() || post.getModeration().isReviewRequired())) {
            return false;
        }

        return blockedTermValidationGateway.validateBlockedTerms(post).isEmpty();
    }

    private void publishModerationRequest(InterestPost interestPost) {
        eventPublisherGateway.publish("interest.moderation.requested", Map.of(
                "interestId", interestPost.getId(),
                "ownerId", interestPost.getOwnerId()
        ));
    }

    private boolean isExpired(InterestPost post) {
        Instant expiration = resolveExpiresAt(post.getExpiresAt(), post.getCreatedAt());
        return expiration != null && !expiration.isAfter(Instant.now());
    }

    private Instant expiresAt(Instant createdAt) {
        return createdAt == null ? null : createdAt.plus(safeExpirationDays(), ChronoUnit.DAYS);
    }

    private Instant renewalExpiresAt(Instant renewalBase) {
        return renewalBase == null ? null : renewalBase.plus(safeRenewalDays(), ChronoUnit.DAYS);
    }

    private Instant resolveExpiresAt(Instant expiresAt, Instant createdAt) {
        return expiresAt != null ? expiresAt : expiresAt(createdAt);
    }

    private long safeExpirationDays() {
        return Math.max(1, listingExpirationDays);
    }

    private long safeRenewalDays() {
        return Math.max(1, listingRenewalDays);
    }

    private String normalizeReferenceImage(String referenceImageUrl) {
        if (!StringUtils.hasText(referenceImageUrl)) {
            return null;
        }

        return referenceImageUrl.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizePostalCode(String value) {
        String digits = Optional.ofNullable(value).orElse("").replaceAll("\\D", "");
        if (!StringUtils.hasText(digits)) {
            return null;
        }
        if (digits.length() != 8) {
            throw new BusinessException("Informe um CEP valido com 8 digitos.");
        }
        return digits.substring(0, 5) + "-" + digits.substring(5);
    }

    private String normalizeCountry(String value) {
        return StringUtils.hasText(value) ? value.trim() : "Brasil";
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && left.equalsIgnoreCase(right);
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String interestSearchCacheKey(InterestSearchCriteria criteria, int offset, int limit) {
        return String.join("|",
                "category=" + safe(criteria.getCategory()),
                "city=" + safe(criteria.getCity()),
                "state=" + safe(criteria.getState()),
                "neighborhood=" + safe(criteria.getNeighborhood()),
                "query=" + safe(criteria.getQuery()),
                "stickerType=" + safe(criteria.getStickerType()),
                "stickerGroup=" + safe(criteria.getStickerGroup()),
                "stickerSelection=" + safe(criteria.getStickerSelection()),
                "stickerNumber=" + safe(criteria.getStickerNumber()),
                "maxBudget=" + Optional.ofNullable(criteria.getMaxBudget()).map(BigDecimal::toPlainString).orElse(""),
                "openOnly=" + criteria.isOpenOnly(),
                "offset=" + offset,
                "limit=" + limit
        );
    }
}
