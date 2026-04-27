package com.euprocuro.api.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.euprocuro.api.application.command.CreateOfferCommand;
import com.euprocuro.api.application.command.CreateSellerItemCommand;
import com.euprocuro.api.application.command.InterestSearchFilter;
import com.euprocuro.api.application.command.ShareSellerItemCommand;
import com.euprocuro.api.application.command.UpdateSellerItemCommand;
import com.euprocuro.api.application.exception.ForbiddenException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.application.usecase.MarketplaceUseCase;
import com.euprocuro.api.application.usecase.SellerItemUseCase;
import com.euprocuro.api.application.view.SellerItemMatchesView;
import com.euprocuro.api.domain.gateway.SellerItemGateway;
import com.euprocuro.api.domain.gateway.UserGateway;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestStatus;
import com.euprocuro.api.domain.model.LocationInfo;
import com.euprocuro.api.domain.model.Offer;
import com.euprocuro.api.domain.model.SellerItem;
import com.euprocuro.api.domain.model.UserProfile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SellerItemService implements SellerItemUseCase {

    private final SellerItemGateway sellerItemGateway;
    private final UserGateway userGateway;
    private final MarketplaceUseCase marketplaceUseCase;

    @Override
    public List<SellerItemMatchesView> listItemsWithMatches(String currentUserId) {
        List<InterestPost> openInterests = marketplaceUseCase.listInterests(InterestSearchFilter.builder()
                .openOnly(true)
                .build());

        return sellerItemGateway.findByOwnerIdOrderByCreatedAtDesc(currentUserId)
                .stream()
                .filter(SellerItem::isActive)
                .map(item -> SellerItemMatchesView.builder()
                        .item(item)
                        .matchingInterests(matchInterests(currentUserId, item, openInterests))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public SellerItem createItem(String currentUserId, CreateSellerItemCommand command) {
        UserProfile user = userGateway.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."));
        Instant now = Instant.now();

        SellerItem item = SellerItem.builder()
                .ownerId(user.getId())
                .ownerName(user.getName())
                .title(command.getTitle())
                .description(command.getDescription())
                .referenceImageUrl(normalize(command.getReferenceImageUrl()))
                .category(command.getCategory())
                .desiredPrice(command.getDesiredPrice())
                .location(LocationInfo.builder()
                        .city(command.getCity())
                        .state(command.getState())
                        .neighborhood(command.getNeighborhood())
                        .remote(false)
                        .build())
                .tags(Optional.ofNullable(command.getTags()).orElse(List.of()))
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return sellerItemGateway.save(item);
    }

    @Override
    public SellerItem updateItem(String currentUserId, String itemId, UpdateSellerItemCommand command) {
        SellerItem item = requireOwnedItem(currentUserId, itemId);
        return sellerItemGateway.save(item.toBuilder()
                .title(command.getTitle())
                .description(command.getDescription())
                .referenceImageUrl(normalize(command.getReferenceImageUrl()))
                .category(command.getCategory())
                .desiredPrice(command.getDesiredPrice())
                .location(LocationInfo.builder()
                        .city(command.getCity())
                        .state(command.getState())
                        .neighborhood(command.getNeighborhood())
                        .remote(false)
                        .build())
                .tags(Optional.ofNullable(command.getTags()).orElse(List.of()))
                .updatedAt(Instant.now())
                .build());
    }

    @Override
    public SellerItem deactivateItem(String currentUserId, String itemId) {
        SellerItem item = requireOwnedItem(currentUserId, itemId);
        return sellerItemGateway.save(item.toBuilder()
                .active(false)
                .updatedAt(Instant.now())
                .build());
    }

    @Override
    public Offer shareItemAsOffer(String currentUserId, String itemId, String interestId, ShareSellerItemCommand command) {
        SellerItem item = requireOwnedItem(currentUserId, itemId);
        CreateOfferCommand offerCommand = CreateOfferCommand.builder()
                .offeredPrice(command.getOfferedPrice() != null ? command.getOfferedPrice() : item.getDesiredPrice())
                .sellerPhone(command.getSellerPhone())
                .message(buildMessage(item, command.getMessage()))
                .offerImageUrl(item.getReferenceImageUrl())
                .includesDelivery(command.isIncludesDelivery())
                .highlights(item.getTags())
                .build();

        return marketplaceUseCase.createOffer(currentUserId, interestId, offerCommand);
    }

    private SellerItem requireOwnedItem(String currentUserId, String itemId) {
        SellerItem item = sellerItemGateway.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item nao encontrado."));
        if (!Objects.equals(item.getOwnerId(), currentUserId)) {
            throw new ForbiddenException("Apenas o dono do item pode executar esta acao.");
        }
        return item;
    }

    private List<InterestPost> matchInterests(String currentUserId, SellerItem item, List<InterestPost> interests) {
        return interests.stream()
                .filter(interest -> interest.getStatus() == InterestStatus.OPEN)
                .filter(interest -> !Objects.equals(interest.getOwnerId(), currentUserId))
                .filter(interest -> interest.getCategory() == item.getCategory())
                .filter(interest -> hasTextMatch(item, interest))
                .collect(Collectors.toList());
    }

    private boolean hasTextMatch(SellerItem item, InterestPost interest) {
        String haystack = (safe(interest.getTitle()) + " " + safe(interest.getDescription()) + " "
                + String.join(" ", Optional.ofNullable(interest.getTags()).orElse(List.of()))).toLowerCase(Locale.ROOT);

        if (tokens(item.getTitle()).stream().anyMatch(haystack::contains)) {
            return true;
        }

        return Optional.ofNullable(item.getTags()).orElse(List.of())
                .stream()
                .map(value -> value.toLowerCase(Locale.ROOT).trim())
                .filter(value -> value.length() >= 3)
                .anyMatch(haystack::contains);
    }

    private List<String> tokens(String value) {
        return List.of(safe(value).toLowerCase(Locale.ROOT).split("\\s+"))
                .stream()
                .map(String::trim)
                .filter(token -> token.length() >= 3)
                .collect(Collectors.toList());
    }

    private String buildMessage(SellerItem item, String customMessage) {
        if (StringUtils.hasText(customMessage)) {
            return customMessage;
        }

        return "Tenho um item que pode atender ao seu interesse: " + item.getTitle() + ". " + item.getDescription();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
