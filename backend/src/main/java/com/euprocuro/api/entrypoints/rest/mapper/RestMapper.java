package com.euprocuro.api.entrypoints.rest.mapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.euprocuro.api.application.command.CreateInterestCommand;
import com.euprocuro.api.application.command.CreateOfferCommand;
import com.euprocuro.api.application.command.CreateSellerItemCommand;
import com.euprocuro.api.application.command.BoostInterestCommand;
import com.euprocuro.api.application.command.ForgotPasswordCommand;
import com.euprocuro.api.application.command.LoginCommand;
import com.euprocuro.api.application.command.PurchaseProductCommand;
import com.euprocuro.api.application.command.RegisterUserCommand;
import com.euprocuro.api.application.command.ResetPasswordCommand;
import com.euprocuro.api.application.command.SendConversationMessageCommand;
import com.euprocuro.api.application.command.ShareSellerItemCommand;
import com.euprocuro.api.application.command.UpdateInterestCommand;
import com.euprocuro.api.application.command.UpdateSellerItemCommand;
import com.euprocuro.api.application.view.AuthenticatedSessionView;
import com.euprocuro.api.application.view.CheckoutView;
import com.euprocuro.api.application.view.ConversationMessageView;
import com.euprocuro.api.application.view.DashboardOfferView;
import com.euprocuro.api.application.view.MonetizationAccountView;
import com.euprocuro.api.application.view.MonetizationProductView;
import com.euprocuro.api.application.view.OfferConversationView;
import com.euprocuro.api.application.view.PasswordResetRequestView;
import com.euprocuro.api.application.view.PersonalDashboardView;
import com.euprocuro.api.application.view.SellerItemMatchesView;
import com.euprocuro.api.domain.model.InterestCategory;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.LocationInfo;
import com.euprocuro.api.domain.model.Offer;
import com.euprocuro.api.domain.model.SellerItem;
import com.euprocuro.api.domain.model.UserProfile;
import com.euprocuro.api.entrypoints.rest.dto.request.CreateInterestRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.CreateOfferRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.CreateSellerItemRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.BoostInterestRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.ForgotPasswordRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.LoginRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.PurchaseProductRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.RegisterRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.ResetPasswordRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.SendConversationMessageRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.ShareSellerItemRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.UpdateInterestRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.UpdateSellerItemRequest;
import com.euprocuro.api.entrypoints.rest.dto.response.ActionMessageResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.AuthResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.CategoryOptionResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.CheckoutResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.ConversationMessageResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.DashboardOfferResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.InterestResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.LocationResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.MonetizationAccountResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.MonetizationProductResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.OfferConversationResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.OfferResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.PersonalDashboardResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.SellerItemMatchesResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.SellerItemResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.UserResponse;

public final class RestMapper {

    private RestMapper() {
    }

    public static RegisterUserCommand toCommand(RegisterRequest request) {
        return RegisterUserCommand.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())
                .city(request.getCity())
                .state(request.getState())
                .bio(request.getBio())
                .build();
    }

    public static LoginCommand toCommand(LoginRequest request) {
        return LoginCommand.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build();
    }

    public static ForgotPasswordCommand toCommand(ForgotPasswordRequest request) {
        return ForgotPasswordCommand.builder()
                .email(request.getEmail())
                .build();
    }

    public static ResetPasswordCommand toCommand(ResetPasswordRequest request) {
        return ResetPasswordCommand.builder()
                .token(request.getToken())
                .newPassword(request.getNewPassword())
                .confirmPassword(request.getConfirmPassword())
                .build();
    }

    public static SendConversationMessageCommand toCommand(SendConversationMessageRequest request) {
        return SendConversationMessageCommand.builder()
                .content(request.getContent())
                .build();
    }

    public static PurchaseProductCommand toCommand(PurchaseProductRequest request) {
        return PurchaseProductCommand.builder()
                .productCode(request.getProductCode())
                .paymentMethod(request.getPaymentMethod())
                .build();
    }

    public static BoostInterestCommand toCommand(BoostInterestRequest request) {
        return BoostInterestCommand.builder()
                .boostCode(request.getBoostCode())
                .paymentMethod(request.getPaymentMethod())
                .build();
    }

    public static CreateInterestCommand toCommand(CreateInterestRequest request) {
        return CreateInterestCommand.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .referenceImageUrl(request.getReferenceImageUrl())
                .category(request.getCategory())
                .budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax())
                .city(request.getCity())
                .state(request.getState())
                .neighborhood(request.getNeighborhood())
                .desiredRadiusKm(request.getDesiredRadiusKm())
                .acceptsNationwideOffers(request.isAcceptsNationwideOffers())
                .allowsWhatsappContact(request.isAllowsWhatsappContact())
                .whatsappContact(request.getWhatsappContact())
                .boostEnabled(request.isBoostEnabled())
                .preferredCondition(request.getPreferredCondition())
                .preferredContactMode(request.getPreferredContactMode())
                .tags(request.getTags())
                .build();
    }

    public static UpdateInterestCommand toCommand(UpdateInterestRequest request) {
        return UpdateInterestCommand.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .referenceImageUrl(request.getReferenceImageUrl())
                .category(request.getCategory())
                .budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax())
                .city(request.getCity())
                .state(request.getState())
                .neighborhood(request.getNeighborhood())
                .desiredRadiusKm(request.getDesiredRadiusKm())
                .acceptsNationwideOffers(request.isAcceptsNationwideOffers())
                .allowsWhatsappContact(request.isAllowsWhatsappContact())
                .whatsappContact(request.getWhatsappContact())
                .boostEnabled(request.isBoostEnabled())
                .preferredCondition(request.getPreferredCondition())
                .preferredContactMode(request.getPreferredContactMode())
                .tags(request.getTags())
                .build();
    }

    public static CreateOfferCommand toCommand(CreateOfferRequest request) {
        return CreateOfferCommand.builder()
                .offeredPrice(request.getOfferedPrice())
                .sellerPhone(request.getSellerPhone())
                .message(request.getMessage())
                .includesDelivery(request.isIncludesDelivery())
                .highlights(request.getHighlights())
                .build();
    }

    public static CreateSellerItemCommand toCommand(CreateSellerItemRequest request) {
        return CreateSellerItemCommand.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .referenceImageUrl(request.getReferenceImageUrl())
                .category(request.getCategory())
                .desiredPrice(request.getDesiredPrice())
                .city(request.getCity())
                .state(request.getState())
                .neighborhood(request.getNeighborhood())
                .tags(Optional.ofNullable(request.getTags()).orElse(List.of()))
                .build();
    }

    public static ShareSellerItemCommand toCommand(ShareSellerItemRequest request) {
        return ShareSellerItemCommand.builder()
                .offeredPrice(request.getOfferedPrice())
                .sellerPhone(request.getSellerPhone())
                .message(request.getMessage())
                .includesDelivery(request.isIncludesDelivery())
                .build();
    }

    public static UpdateSellerItemCommand toCommand(UpdateSellerItemRequest request) {
        return UpdateSellerItemCommand.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .referenceImageUrl(request.getReferenceImageUrl())
                .category(request.getCategory())
                .desiredPrice(request.getDesiredPrice())
                .city(request.getCity())
                .state(request.getState())
                .neighborhood(request.getNeighborhood())
                .tags(Optional.ofNullable(request.getTags()).orElse(List.of()))
                .build();
    }

    public static AuthResponse toResponse(AuthenticatedSessionView view) {
        return AuthResponse.builder()
                .token(view.getToken())
                .expiresAt(view.getExpiresAt())
                .user(toResponse(view.getUser()))
                .build();
    }

    public static ActionMessageResponse toResponse(PasswordResetRequestView view) {
        return ActionMessageResponse.builder()
                .message(view.getMessage())
                .previewResetLink(view.getPreviewResetLink())
                .previewToken(view.getPreviewToken())
                .build();
    }

    public static CheckoutResponse toResponse(CheckoutView view) {
        return CheckoutResponse.builder()
                .provider(view.getProvider())
                .paymentMethod(view.getPaymentMethod())
                .productCode(view.getProductCode())
                .checkoutUrl(view.getCheckoutUrl())
                .message(view.getMessage())
                .build();
    }

    public static MonetizationAccountResponse toResponse(MonetizationAccountView view) {
        return MonetizationAccountResponse.builder()
                .sellerCredits(view.getSellerCredits())
                .purchasedCreditsTotal(view.getPurchasedCreditsTotal())
                .subscriptionPlan(view.getSubscriptionPlan())
                .subscriptionActiveUntil(view.getSubscriptionActiveUntil())
                .subscriptionActive(view.isSubscriptionActive())
                .products(view.getProducts().stream().map(RestMapper::toResponse).collect(Collectors.toList()))
                .build();
    }

    public static MonetizationProductResponse toResponse(MonetizationProductView view) {
        return MonetizationProductResponse.builder()
                .code(view.getCode())
                .name(view.getName())
                .description(view.getDescription())
                .type(view.getType())
                .price(view.getPrice())
                .credits(view.getCredits())
                .durationDays(view.getDurationDays())
                .build();
    }

    public static UserResponse toResponse(UserProfile domain) {
        return UserResponse.builder()
                .id(domain.getId())
                .name(domain.getName())
                .email(domain.getEmail())
                .city(domain.getCity())
                .state(domain.getState())
                .bio(domain.getBio())
                .buyerRating(domain.getBuyerRating())
                .sellerRating(domain.getSellerRating())
                .sellerCredits(domain.getSellerCredits())
                .purchasedCreditsTotal(domain.getPurchasedCreditsTotal())
                .subscriptionPlan(domain.getSubscriptionPlan())
                .subscriptionActiveUntil(domain.getSubscriptionActiveUntil())
                .build();
    }

    public static InterestResponse toResponse(InterestPost domain) {
        return InterestResponse.builder()
                .id(domain.getId())
                .ownerId(domain.getOwnerId())
                .ownerName(domain.getOwnerName())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .referenceImageUrl(domain.getReferenceImageUrl())
                .category(domain.getCategory())
                .budgetMin(domain.getBudgetMin())
                .budgetMax(domain.getBudgetMax())
                .location(toResponse(domain.getLocation()))
                .tags(Optional.ofNullable(domain.getTags()).orElse(List.of()))
                .desiredRadiusKm(domain.getDesiredRadiusKm())
                .acceptsNationwideOffers(domain.isAcceptsNationwideOffers())
                .allowsWhatsappContact(domain.isAllowsWhatsappContact())
                .whatsappContact(domain.isAllowsWhatsappContact() ? domain.getWhatsappContact() : null)
                .boostEnabled(domain.isBoostEnabled())
                .boostedUntil(domain.getBoostedUntil())
                .preferredCondition(domain.getPreferredCondition())
                .preferredContactMode(domain.getPreferredContactMode())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public static OfferResponse toResponse(Offer domain) {
        return OfferResponse.builder()
                .id(domain.getId())
                .interestPostId(domain.getInterestPostId())
                .sellerId(domain.getSellerId())
                .sellerName(domain.getSellerName())
                .sellerEmail(domain.getSellerEmail())
                .sellerPhone(domain.getSellerPhone())
                .offeredPrice(domain.getOfferedPrice())
                .message(domain.getMessage())
                .includesDelivery(domain.isIncludesDelivery())
                .highlights(Optional.ofNullable(domain.getHighlights()).orElse(List.of()))
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public static SellerItemResponse toResponse(SellerItem domain) {
        return SellerItemResponse.builder()
                .id(domain.getId())
                .ownerId(domain.getOwnerId())
                .ownerName(domain.getOwnerName())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .referenceImageUrl(domain.getReferenceImageUrl())
                .category(domain.getCategory())
                .desiredPrice(domain.getDesiredPrice())
                .location(toResponse(domain.getLocation()))
                .tags(Optional.ofNullable(domain.getTags()).orElse(List.of()))
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public static SellerItemMatchesResponse toResponse(SellerItemMatchesView view) {
        List<InterestPost> matchingInterests = Optional.ofNullable(view.getMatchingInterests()).orElse(List.of());
        return SellerItemMatchesResponse.builder()
                .item(toResponse(view.getItem()))
                .matchingInterests(matchingInterests.stream().map(RestMapper::toResponse).collect(Collectors.toList()))
                .matchCount(matchingInterests.size())
                .build();
    }

    public static PersonalDashboardResponse toResponse(PersonalDashboardView view) {
        return PersonalDashboardResponse.builder()
                .user(toResponse(view.getUser()))
                .totalActiveInterests(view.getTotalActiveInterests())
                .totalOffersSent(view.getTotalOffersSent())
                .totalOffersReceived(view.getTotalOffersReceived())
                .myInterests(view.getMyInterests().stream().map(RestMapper::toResponse).collect(Collectors.toList()))
                .offersSent(view.getOffersSent().stream().map(RestMapper::toResponse).collect(Collectors.toList()))
                .offersReceived(view.getOffersReceived().stream().map(RestMapper::toResponse).collect(Collectors.toList()))
                .build();
    }

    public static DashboardOfferResponse toResponse(DashboardOfferView view) {
        return DashboardOfferResponse.builder()
                .id(view.getId())
                .interestPostId(view.getInterestPostId())
                .interestTitle(view.getInterestTitle())
                .referenceImageUrl(view.getReferenceImageUrl())
                .buyerId(view.getBuyerId())
                .sellerName(view.getSellerName())
                .sellerEmail(view.getSellerEmail())
                .sellerPhone(view.getSellerPhone())
                .buyerName(view.getBuyerName())
                .offeredPrice(view.getOfferedPrice())
                .message(view.getMessage())
                .includesDelivery(view.isIncludesDelivery())
                .highlights(Optional.ofNullable(view.getHighlights()).orElse(List.of()))
                .status(view.getStatus())
                .createdAt(view.getCreatedAt())
                .latestMessage(view.getLatestMessage())
                .latestMessageSenderId(view.getLatestMessageSenderId())
                .latestMessageAt(view.getLatestMessageAt())
                .build();
    }

    public static OfferConversationResponse toResponse(OfferConversationView view) {
        return OfferConversationResponse.builder()
                .offerId(view.getOfferId())
                .interestPostId(view.getInterestPostId())
                .interestTitle(view.getInterestTitle())
                .buyerId(view.getBuyerId())
                .buyerName(view.getBuyerName())
                .sellerId(view.getSellerId())
                .sellerName(view.getSellerName())
                .sellerEmail(view.getSellerEmail())
                .sellerPhone(view.getSellerPhone())
                .offeredPrice(view.getOfferedPrice())
                .messages(view.getMessages().stream().map(RestMapper::toResponse).collect(Collectors.toList()))
                .build();
    }

    public static ConversationMessageResponse toResponse(ConversationMessageView view) {
        return ConversationMessageResponse.builder()
                .id(view.getId())
                .offerId(view.getOfferId())
                .senderId(view.getSenderId())
                .senderName(view.getSenderName())
                .recipientId(view.getRecipientId())
                .recipientName(view.getRecipientName())
                .content(view.getContent())
                .createdAt(view.getCreatedAt())
                .build();
    }

    public static CategoryOptionResponse toResponse(InterestCategory category) {
        return CategoryOptionResponse.builder()
                .value(category.name())
                .label(category.getLabel())
                .build();
    }

    private static LocationResponse toResponse(LocationInfo location) {
        if (location == null) {
            return null;
        }

        return LocationResponse.builder()
                .city(location.getCity())
                .state(location.getState())
                .neighborhood(location.getNeighborhood())
                .remote(location.isRemote())
                .build();
    }
}
