package com.euprocuro.api.entrypoints.rest.mapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.euprocuro.api.application.command.CreateInterestCommand;
import com.euprocuro.api.application.command.CreateOfferCommand;
import com.euprocuro.api.application.command.CreateSellerItemCommand;
import com.euprocuro.api.application.command.BoostInterestCommand;
import com.euprocuro.api.application.command.CatalogCategoryCommand;
import com.euprocuro.api.application.command.CatalogProductCommand;
import com.euprocuro.api.application.command.FacebookLoginCommand;
import com.euprocuro.api.application.command.FeatureFlagsCommand;
import com.euprocuro.api.application.command.ForgotPasswordCommand;
import com.euprocuro.api.application.command.GoogleLoginCommand;
import com.euprocuro.api.application.command.LoginCommand;
import com.euprocuro.api.application.command.ModerationDecisionCommand;
import com.euprocuro.api.application.command.ModerationSettingsCommand;
import com.euprocuro.api.application.command.MonetizationSettingsCommand;
import com.euprocuro.api.application.command.OperationalFlagsCommand;
import com.euprocuro.api.application.command.OperationalFieldsCommand;
import com.euprocuro.api.application.command.PurchaseProductCommand;
import com.euprocuro.api.application.command.ConfirmPhoneVerificationCommand;
import com.euprocuro.api.application.command.ConfirmRegistrationCommand;
import com.euprocuro.api.application.command.StartPhoneVerificationCommand;
import com.euprocuro.api.application.command.StartRegistrationCommand;
import com.euprocuro.api.application.command.ReportInterestCommand;
import com.euprocuro.api.application.command.ResetPasswordCommand;
import com.euprocuro.api.application.command.SaveContentEntryCommand;
import com.euprocuro.api.application.command.SaveModerationRuleCommand;
import com.euprocuro.api.application.command.SaveOperationalCatalogCommand;
import com.euprocuro.api.application.command.SendConversationMessageCommand;
import com.euprocuro.api.application.command.ShareSellerItemCommand;
import com.euprocuro.api.application.command.StickerDetailsCommand;
import com.euprocuro.api.application.command.UpdateInterestCommand;
import com.euprocuro.api.application.command.UpdateSellerItemCommand;
import com.euprocuro.api.application.service.OperationalCatalogService;
import com.euprocuro.api.application.view.AdminModerationView;
import com.euprocuro.api.application.view.AdminOperationalCatalogView;
import com.euprocuro.api.application.view.AddressLookupView;
import com.euprocuro.api.application.view.AuthenticatedSessionView;
import com.euprocuro.api.application.view.CacheInvalidationView;
import com.euprocuro.api.application.view.CatalogCategoryView;
import com.euprocuro.api.application.view.CheckoutView;
import com.euprocuro.api.application.view.ConversationMessageView;
import com.euprocuro.api.application.view.AdminContentCatalogView;
import com.euprocuro.api.application.view.ContentEntryView;
import com.euprocuro.api.application.view.ContentReportView;
import com.euprocuro.api.application.view.ContentRevisionView;
import com.euprocuro.api.application.view.DashboardOfferView;
import com.euprocuro.api.application.view.MonetizationAccountView;
import com.euprocuro.api.application.view.MonetizationProductView;
import com.euprocuro.api.application.view.ModerationRuleView;
import com.euprocuro.api.application.view.OfferConversationView;
import com.euprocuro.api.application.view.PasswordResetRequestView;
import com.euprocuro.api.application.view.PaymentOrderView;
import com.euprocuro.api.application.view.PersonalDashboardView;
import com.euprocuro.api.application.view.PublicContentCatalogView;
import com.euprocuro.api.application.view.PublicOperationalSettingsView;
import com.euprocuro.api.application.view.RegistrationView;
import com.euprocuro.api.application.view.SellerItemMatchesView;
import com.euprocuro.api.domain.model.InterestModeration;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestStatus;
import com.euprocuro.api.domain.model.LocationInfo;
import com.euprocuro.api.domain.model.ModerationRule;
import com.euprocuro.api.domain.model.Offer;
import com.euprocuro.api.domain.model.SellerItem;
import com.euprocuro.api.domain.model.StickerDetails;
import com.euprocuro.api.domain.model.UserProfile;
import com.euprocuro.api.entrypoints.rest.dto.request.CreateInterestRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.CreateOfferRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.CreateSellerItemRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.BoostInterestRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.ForgotPasswordRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.FacebookLoginRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.GoogleLoginRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.LoginRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.ModerationDecisionRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.PurchaseProductRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.ConfirmPhoneVerificationRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.ConfirmRegistrationRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.StartPhoneVerificationRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.StartRegistrationRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.ReportInterestRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.ResetPasswordRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.SaveContentEntryRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.SaveModerationRuleRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.SaveOperationalCatalogRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.SaveOperationalFlagsRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.SendConversationMessageRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.ShareSellerItemRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.UpdateInterestRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.UpdateSellerItemRequest;
import com.euprocuro.api.entrypoints.rest.dto.response.*;

public final class RestMapper {

    private RestMapper() {
    }

    public static StartRegistrationCommand toCommand(StartRegistrationRequest request, String clientIp) {
        return StartRegistrationCommand.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())
                .phone(request.getPhone())
                .ipAddress(clientIp)
                .termsAccepted(request.isTermsAccepted())
                .termsVersion(request.getTermsVersion())
                .build();
    }

    public static ConfirmRegistrationCommand toCommand(ConfirmRegistrationRequest request, String clientIp) {
        return ConfirmRegistrationCommand.builder()
                .email(request.getEmail())
                .code(request.getCode())
                .ipAddress(clientIp)
                .build();
    }

    public static StartPhoneVerificationCommand toCommand(StartPhoneVerificationRequest request) {
        return StartPhoneVerificationCommand.builder()
                .phone(request.getPhone())
                .build();
    }

    public static ConfirmPhoneVerificationCommand toCommand(ConfirmPhoneVerificationRequest request) {
        return ConfirmPhoneVerificationCommand.builder()
                .phone(request.getPhone())
                .code(request.getCode())
                .build();
    }

    public static LoginCommand toCommand(LoginRequest request) {
        return LoginCommand.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build();
    }

    public static GoogleLoginCommand toGoogleLoginCommand(GoogleLoginRequest request, String clientIp) {
        return GoogleLoginCommand.builder()
                .accessToken(request.getAccessToken())
                .ipAddress(clientIp)
                .build();
    }

    public static FacebookLoginCommand toFacebookLoginCommand(FacebookLoginRequest request, String clientIp) {
        return FacebookLoginCommand.builder()
                .accessToken(request.getAccessToken())
                .ipAddress(clientIp)
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
                .imageUrl(request.getImageUrl())
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

    public static ReportInterestCommand toCommand(ReportInterestRequest request) {
        return ReportInterestCommand.builder()
                .reason(request.getReason())
                .message(request.getMessage())
                .build();
    }

    public static SaveModerationRuleCommand toCommand(SaveModerationRuleRequest request) {
        return SaveModerationRuleCommand.builder()
                .term(request.getTerm())
                .riskLevel(request.getRiskLevel())
                .active(request.isActive())
                .build();
    }

    public static SaveContentEntryCommand toCommand(SaveContentEntryRequest request) {
        return SaveContentEntryCommand.builder()
                .key(request.getKey())
                .type(request.getType())
                .locale(request.getLocale())
                .draftValue(request.getDraftValue())
                .description(request.getDescription())
                .screen(request.getScreen())
                .legalSlug(request.getLegalSlug())
                .requiresUserAcceptance(request.isRequiresUserAcceptance())
                .effectiveFrom(request.getEffectiveFrom())
                .build();
    }

    public static SaveOperationalCatalogCommand toCommand(SaveOperationalCatalogRequest request) {
        return SaveOperationalCatalogCommand.builder()
                .monetizationSettings(MonetizationSettingsCommand.builder()
                        .creditPurchasesEnabled(request.getMonetizationSettings() != null
                                && request.getMonetizationSettings().isCreditPurchasesEnabled())
                        .boostPurchasesEnabled(request.getMonetizationSettings() != null
                                && request.getMonetizationSettings().isBoostPurchasesEnabled())
                        .build())
                .moderationSettings(ModerationSettingsCommand.builder()
                        .userBlockListEnabled(request.getModerationSettings() == null
                                || request.getModerationSettings().isUserBlockListEnabled())
                        .build())
                .categories(Optional.ofNullable(request.getCategories()).orElse(List.of())
                        .stream()
                                .map(category -> CatalogCategoryCommand.builder()
                                .code(firstText(category.getCode(), category.getValue()))
                                .label(category.getLabel())
                                .active(category.isActive())
                                .sortOrder(category.getSortOrder())
                                .build())
                        .collect(Collectors.toList()))
                .products(Optional.ofNullable(request.getProducts()).orElse(List.of())
                        .stream()
                        .map(product -> CatalogProductCommand.builder()
                                .code(product.getCode())
                                .name(product.getName())
                                .description(product.getDescription())
                                .type(product.getType())
                                .price(product.getPrice())
                                .originalPrice(product.getOriginalPrice())
                                .promotional(product.isPromotional())
                                .promotionLabel(product.getPromotionLabel())
                                .credits(product.getCredits())
                                .durationDays(product.getDurationDays())
                                .enabled(product.isEnabled())
                                .sortOrder(product.getSortOrder())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    public static OperationalFlagsCommand toCommand(SaveOperationalFlagsRequest request) {
        return OperationalFlagsCommand.builder()
                .monetizationSettings(MonetizationSettingsCommand.builder()
                        .creditPurchasesEnabled(request.getMonetizationSettings() != null
                                && request.getMonetizationSettings().isCreditPurchasesEnabled())
                        .boostPurchasesEnabled(request.getMonetizationSettings() != null
                                && request.getMonetizationSettings().isBoostPurchasesEnabled())
                        .build())
                .moderationSettings(ModerationSettingsCommand.builder()
                        .userBlockListEnabled(request.getModerationSettings() == null
                                || request.getModerationSettings().isUserBlockListEnabled())
                        .build())
                .featureFlags(FeatureFlagsCommand.builder()
                        .stickersPageEnabled(request.getFeatureFlags() == null
                                ? null
                                : request.getFeatureFlags().getStickersPageEnabled())
                        .sellerProPlanEnabled(request.getFeatureFlags() == null
                                ? null
                                : request.getFeatureFlags().getSellerProPlanEnabled())
                        .captchaEnabled(request.getFeatureFlags() == null
                                ? null
                                : request.getFeatureFlags().getCaptchaEnabled())
                        .build())
                .operationalFields(OperationalFieldsCommand.builder()
                        .initialFreeCredits(request.getOperationalFields() == null
                                ? null
                                : request.getOperationalFields().getInitialFreeCredits())
                        .listingRenewalCredits(request.getOperationalFields() == null
                                ? null
                                : request.getOperationalFields().getListingRenewalCredits())
                        .build())
                .build();
    }

    public static ModerationDecisionCommand toCommand(ModerationDecisionRequest request) {
        return ModerationDecisionCommand.builder()
                .status(request.getStatus())
                .reason(request.getReason())
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
                .postalCode(request.getPostalCode())
                .city(request.getCity())
                .state(request.getState())
                .neighborhood(request.getNeighborhood())
                .country(request.getCountry())
                .desiredRadiusKm(request.getDesiredRadiusKm())
                .stickerDetails(toCommand(request.getStickerDetails()))
                .allowsWhatsappContact(request.isAllowsWhatsappContact())
                .whatsappContact(request.getWhatsappContact())
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
                .postalCode(request.getPostalCode())
                .city(request.getCity())
                .state(request.getState())
                .neighborhood(request.getNeighborhood())
                .country(request.getCountry())
                .desiredRadiusKm(request.getDesiredRadiusKm())
                .stickerDetails(toCommand(request.getStickerDetails()))
                .allowsWhatsappContact(request.isAllowsWhatsappContact())
                .whatsappContact(request.getWhatsappContact())
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
                .offerImageUrl(request.getOfferImageUrl())
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
                .postalCode(request.getPostalCode())
                .city(request.getCity())
                .state(request.getState())
                .neighborhood(request.getNeighborhood())
                .country(request.getCountry())
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
                .postalCode(request.getPostalCode())
                .city(request.getCity())
                .state(request.getState())
                .neighborhood(request.getNeighborhood())
                .country(request.getCountry())
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

    public static ActionMessageResponse toResponse(RegistrationView view) {
        return ActionMessageResponse.builder()
                .message(view.getMessage())
                .build();
    }

    public static MeResponse toMeResponse(AuthenticatedSessionView session) {
        return MeResponse.builder()
                .id(session.getUser().getId())
                .name(session.getUser().getName())
                .email(session.getUser().getEmail())
                .phone(session.getUser().getPhone())
                .phoneVerified(session.getUser().isPhoneVerified())
                .postalCode(session.getUser().getPostalCode())
                .city(session.getUser().getCity())
                .state(session.getUser().getState())
                .neighborhood(session.getUser().getNeighborhood())
                .country(session.getUser().getCountry())
                .credits(session.getUser().getSellerCredits())
                .expiresAt(session.getExpiresAt())
                .build();
    }

    public static MeResponse toMeResponse(UserProfile user) {
        return MeResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .phoneVerified(user.isPhoneVerified())
                .postalCode(user.getPostalCode())
                .city(user.getCity())
                .state(user.getState())
                .neighborhood(user.getNeighborhood())
                .country(user.getCountry())
                .credits(user.getSellerCredits())
                .build();
    }

    public static CheckoutResponse toResponse(CheckoutView view) {
        return CheckoutResponse.builder()
                .provider(view.getProvider())
                .paymentMethod(view.getPaymentMethod())
                .productCode(view.getProductCode())
                .paymentOrderId(view.getPaymentOrderId())
                .providerPreferenceId(view.getProviderPreferenceId())
                .checkoutUrl(view.getCheckoutUrl())
                .status(view.getStatus())
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
                .creditPurchasesEnabled(view.isCreditPurchasesEnabled())
                .boostPurchasesEnabled(view.isBoostPurchasesEnabled())
                .products(view.getProducts().stream().map(RestMapper::toResponse).collect(Collectors.toList()))
                .paymentHistory(Optional.ofNullable(view.getPaymentHistory()).orElse(List.of())
                        .stream()
                        .map(RestMapper::toResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    public static PaymentOrderResponse toResponse(PaymentOrderView view) {
        return PaymentOrderResponse.builder()
                .id(view.getId())
                .productCode(view.getProductCode())
                .productName(view.getProductName())
                .amount(view.getAmount())
                .paymentMethod(view.getPaymentMethod())
                .provider(view.getProvider())
                .status(view.getStatus())
                .providerPaymentId(view.getProviderPaymentId())
                .createdAt(view.getCreatedAt())
                .updatedAt(view.getUpdatedAt())
                .approvedAt(view.getApprovedAt())
                .build();
    }

    public static MonetizationProductResponse toResponse(MonetizationProductView view) {
        return MonetizationProductResponse.builder()
                .code(view.getCode())
                .name(view.getName())
                .description(view.getDescription())
                .type(view.getType())
                .price(view.getPrice())
                .originalPrice(view.getOriginalPrice())
                .promotional(view.isPromotional())
                .promotionLabel(view.getPromotionLabel())
                .credits(view.getCredits())
                .durationDays(view.getDurationDays())
                .enabled(view.isEnabled())
                .sortOrder(view.getSortOrder())
                .build();
    }

    public static UserResponse toResponse(UserProfile domain) {
        return toResponse(domain, false);
    }

    public static UserResponse toResponse(UserProfile domain, boolean isAdmin) {
        return UserResponse.builder()
                .id(domain.getId())
                .name(domain.getName())
                .email(domain.getEmail())
                .phone(domain.getPhone())
                .phoneVerified(domain.isPhoneVerified())
                .postalCode(domain.getPostalCode())
                .city(domain.getCity())
                .state(domain.getState())
                .neighborhood(domain.getNeighborhood())
                .country(domain.getCountry())
                .emailVerified(domain.isEmailVerified())
                .googleLinked(org.springframework.util.StringUtils.hasText(domain.getGoogleSubject()))
                .facebookLinked(org.springframework.util.StringUtils.hasText(domain.getFacebookSubject()))
                .buyerRating(domain.getBuyerRating())
                .sellerRating(domain.getSellerRating())
                .sellerCredits(domain.getSellerCredits())
                .purchasedCreditsTotal(domain.getPurchasedCreditsTotal())
                .subscriptionPlan(domain.getSubscriptionPlan())
                .subscriptionActiveUntil(domain.getSubscriptionActiveUntil())
                .admin(isAdmin)
                .build();
    }

    public static AdminOperationalCatalogResponse toResponse(AdminOperationalCatalogView view) {
        return AdminOperationalCatalogResponse.builder()
                .monetizationSettings(toResponse(view.getMonetizationSettings()))
                .moderationSettings(toResponse(view.getModerationSettings()))
                .featureFlags(toResponse(view.getFeatureFlags()))
                .operationalFields(toResponse(view.getOperationalFields()))
                .categories(Optional.ofNullable(view.getCategories()).orElse(List.of())
                        .stream()
                        .map(RestMapper::toResponse)
                        .collect(Collectors.toList()))
                .products(Optional.ofNullable(view.getProducts()).orElse(List.of())
                        .stream()
                        .map(RestMapper::toResponse)
                        .collect(Collectors.toList()))
                .updatedAt(view.getUpdatedAt())
                .build();
    }

    public static MonetizationSettingsResponse toResponse(com.euprocuro.api.application.view.MonetizationSettingsView view) {
        if (view == null) {
            return MonetizationSettingsResponse.builder().build();
        }
        return MonetizationSettingsResponse.builder()
                .creditPurchasesEnabled(view.isCreditPurchasesEnabled())
                .boostPurchasesEnabled(view.isBoostPurchasesEnabled())
                .build();
    }

    public static ModerationSettingsResponse toResponse(com.euprocuro.api.application.view.ModerationSettingsView view) {
        if (view == null) {
            return ModerationSettingsResponse.builder()
                    .userBlockListEnabled(true)
                    .build();
        }
        return ModerationSettingsResponse.builder()
                .userBlockListEnabled(view.isUserBlockListEnabled())
                .build();
    }

    public static FeatureFlagsResponse toResponse(com.euprocuro.api.application.view.FeatureFlagsView view) {
        if (view == null) {
            return FeatureFlagsResponse.builder()
                    .stickersPageEnabled(true)
                    .sellerProPlanEnabled(false)
                    .captchaEnabled(true)
                    .build();
        }
        return FeatureFlagsResponse.builder()
                .stickersPageEnabled(view.isStickersPageEnabled())
                .sellerProPlanEnabled(view.isSellerProPlanEnabled())
                .captchaEnabled(view.isCaptchaEnabled())
                .build();
    }

    public static OperationalFieldsResponse toResponse(com.euprocuro.api.application.view.OperationalFieldsView view) {
        if (view == null) {
            return OperationalFieldsResponse.builder()
                    .initialFreeCredits(15)
                    .listingRenewalCredits(1)
                    .build();
        }
        return OperationalFieldsResponse.builder()
                .initialFreeCredits(view.getInitialFreeCredits())
                .listingRenewalCredits(view.getListingRenewalCredits())
                .build();
    }

    public static PublicOperationalSettingsResponse toResponse(PublicOperationalSettingsView view) {
        return PublicOperationalSettingsResponse.builder()
                .featureFlags(toResponse(view.getFeatureFlags()))
                .operationalFields(toResponse(view.getOperationalFields()))
                .build();
    }

    public static InterestResponse toResponse(InterestPost domain) {
        return toResponse(domain, true);
    }

    public static InterestResponse toPublicInterestResponse(InterestPost domain) {
        return toResponse(domain, false);
    }

    public static InterestResponse toResponse(InterestPost domain, boolean exposeRestrictedDetails) {
        boolean exposeLocationDetails = exposeRestrictedDetails
                || OperationalCatalogService.STICKERS_CATEGORY_CODE.equals(domain.getCategory());
        boolean exposePostalCode = exposeRestrictedDetails;
        return InterestResponse.builder()
                .id(domain.getId())
                .ownerId(exposeRestrictedDetails ? domain.getOwnerId() : null)
                .ownerName(exposeRestrictedDetails ? domain.getOwnerName() : null)
                .title(domain.getTitle())
                .description(domain.getDescription())
                .referenceImageUrl(exposeRestrictedDetails ? domain.getReferenceImageUrl() : publicReferenceImageUrl(domain.getReferenceImageUrl()))
                .category(domain.getCategory())
                .budgetMin(exposeRestrictedDetails ? domain.getBudgetMin() : null)
                .budgetMax(exposeRestrictedDetails ? domain.getBudgetMax() : null)
                .location(toResponse(domain.getLocation(), exposePostalCode, exposeLocationDetails))
                .tags(Optional.ofNullable(domain.getTags()).orElse(List.of()))
                .stickerDetails(toResponse(domain.getStickerDetails()))
                .desiredRadiusKm(domain.getDesiredRadiusKm())
                .allowsWhatsappContact(exposeRestrictedDetails && domain.isAllowsWhatsappContact())
                .whatsappContact(exposeRestrictedDetails && domain.isAllowsWhatsappContact() ? domain.getWhatsappContact() : null)
                .boostedUntil(domain.getBoostedUntil())
                .preferredCondition(domain.getPreferredCondition())
                .preferredContactMode(domain.getPreferredContactMode())
                .status(exposeRestrictedDetails ? domain.getStatus() : publicInterestStatus(domain.getStatus()))
                .moderation(exposeRestrictedDetails ? toResponse(domain.getModeration()) : null)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .expiresAt(domain.getExpiresAt())
                .build();
    }

    private static InterestStatus publicInterestStatus(InterestStatus status) {
        if (status == InterestStatus.OPEN || status == InterestStatus.APPROVED || status == InterestStatus.REPORTED) {
            return InterestStatus.OPEN;
        }
        return null;
    }

    private static String publicReferenceImageUrl(String referenceImageUrl) {
        if (referenceImageUrl == null) {
            return null;
        }

        String value = referenceImageUrl.trim();
        String normalized = value.toLowerCase();
        if (normalized.startsWith("javascript:")) {
            return null;
        }
        if (normalized.startsWith("data:") && !isPublicImageDataUrl(normalized)) {
            return null;
        }

        return value;
    }

    private static boolean isPublicImageDataUrl(String value) {
        return value.startsWith("data:image/png;base64,")
                || value.startsWith("data:image/jpeg;base64,")
                || value.startsWith("data:image/jpg;base64,")
                || value.startsWith("data:image/webp;base64,");
    }

    public static InterestModerationResponse toResponse(InterestModeration domain) {
        if (domain == null) {
            return null;
        }

        return InterestModerationResponse.builder()
                .riskLevel(domain.getRiskLevel())
                .categories(Optional.ofNullable(domain.getCategories()).orElse(Map.of()))
                .scores(domain.getScores())
                .reviewRequired(domain.isReviewRequired())
                .provider(domain.getProvider())
                .reason(domain.getReason())
                .checkedAt(domain.getCheckedAt())
                .reviewedBy(domain.getReviewedBy())
                .reviewedAt(domain.getReviewedAt())
                .build();
    }

    public static ModerationRuleResponse toResponse(ModerationRule domain) {
        return ModerationRuleResponse.builder()
                .id(domain.getId())
                .term(domain.getTerm())
                .riskLevel(domain.getRiskLevel())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public static ModerationRuleResponse toResponse(ModerationRuleView view) {
        return ModerationRuleResponse.builder()
                .id(view.getId())
                .term(view.getTerm())
                .riskLevel(view.getRiskLevel())
                .active(view.isActive())
                .createdAt(view.getCreatedAt())
                .updatedAt(view.getUpdatedAt())
                .build();
    }

    public static PublicContentCatalogResponse toResponse(PublicContentCatalogView view) {
        return PublicContentCatalogResponse.builder()
                .locale(view.getLocale())
                .version(view.getVersion())
                .entries(Optional.ofNullable(view.getEntries()).orElse(List.of())
                        .stream()
                        .collect(Collectors.toMap(
                                ContentEntryView::getKey,
                                RestMapper::toPublicResponse,
                                (left, right) -> right,
                                java.util.LinkedHashMap::new
                        )))
                .build();
    }

    public static CacheInvalidationResponse toResponse(CacheInvalidationView view) {
        return CacheInvalidationResponse.builder()
                .scope(view.getScope())
                .enabled(view.isEnabled())
                .provider(view.getProvider())
                .entries(view.getEntries())
                .versions(view.getVersions())
                .invalidatedAt(view.getInvalidatedAt())
                .build();
    }

    public static AdminContentCatalogResponse toResponse(AdminContentCatalogView view) {
        return AdminContentCatalogResponse.builder()
                .entries(Optional.ofNullable(view.getEntries()).orElse(List.of())
                        .stream()
                        .map(RestMapper::toResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    public static ContentEntryResponse toResponse(ContentEntryView view) {
        return ContentEntryResponse.builder()
                .id(view.getId())
                .key(view.getKey())
                .type(view.getType())
                .locale(view.getLocale())
                .status(view.getStatus())
                .version(view.getVersion())
                .draftValue(view.getDraftValue())
                .publishedValue(view.getPublishedValue())
                .defaultValue(view.getDefaultValue())
                .defaultValueHash(view.getDefaultValueHash())
                .description(view.getDescription())
                .screen(view.getScreen())
                .legalSlug(view.getLegalSlug())
                .requiresUserAcceptance(view.isRequiresUserAcceptance())
                .defaultUpdateAvailable(view.isDefaultUpdateAvailable())
                .effectiveFrom(view.getEffectiveFrom())
                .createdAt(view.getCreatedAt())
                .updatedAt(view.getUpdatedAt())
                .defaultUpdatedAt(view.getDefaultUpdatedAt())
                .publishedAt(view.getPublishedAt())
                .build();
    }

    public static ContentRevisionResponse toResponse(ContentRevisionView view) {
        return ContentRevisionResponse.builder()
                .id(view.getId())
                .contentEntryId(view.getContentEntryId())
                .key(view.getKey())
                .locale(view.getLocale())
                .version(view.getVersion())
                .snapshotValue(view.getSnapshotValue())
                .publishedAt(view.getPublishedAt())
                .build();
    }

    private static PublicContentEntryResponse toPublicResponse(ContentEntryView view) {
        return PublicContentEntryResponse.builder()
                .key(view.getKey())
                .type(view.getType())
                .locale(view.getLocale())
                .version(view.getVersion())
                .value(view.getPublicValue())
                .legalSlug(view.getLegalSlug())
                .requiresUserAcceptance(view.isRequiresUserAcceptance())
                .effectiveFrom(view.getEffectiveFrom())
                .publishedAt(view.getPublishedAt())
                .build();
    }

    public static ContentReportResponse toResponse(ContentReportView view) {
        return ContentReportResponse.builder()
                .id(view.getId())
                .contentType(view.getContentType())
                .contentId(view.getContentId())
                .contentTitle(view.getContentTitle())
                .contentDescription(view.getContentDescription())
                .contentStatus(view.getContentStatus())
                .reportedBy(view.getReportedBy())
                .reason(view.getReason())
                .message(view.getMessage())
                .status(view.getStatus())
                .createdAt(view.getCreatedAt())
                .reviewedBy(view.getReviewedBy())
                .reviewedAt(view.getReviewedAt())
                .build();
    }

    public static AdminModerationResponse toResponse(AdminModerationView view) {
        return AdminModerationResponse.builder()
                .pendingInterests(Optional.ofNullable(view.getPendingInterests()).orElse(List.of())
                        .stream()
                        .map(RestMapper::toResponse)
                        .collect(Collectors.toList()))
                .rules(Optional.ofNullable(view.getRules()).orElse(List.of())
                        .stream()
                        .map(RestMapper::toResponse)
                        .collect(Collectors.toList()))
                .openReports(Optional.ofNullable(view.getOpenReports()).orElse(List.of())
                        .stream()
                        .map(RestMapper::toResponse)
                        .collect(Collectors.toList()))
                .processedReports(Optional.ofNullable(view.getProcessedReports()).orElse(List.of())
                        .stream()
                        .map(RestMapper::toResponse)
                        .collect(Collectors.toList()))
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
                .offerImageUrl(domain.getOfferImageUrl())
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
                .matchingInterests(matchingInterests.stream().map(RestMapper::toPublicInterestResponse).collect(Collectors.toList()))
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
                .offerImageUrl(view.getOfferImageUrl())
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
                .offerImageUrl(view.getOfferImageUrl())
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
                .imageUrl(view.getImageUrl())
                .createdAt(view.getCreatedAt())
                .build();
    }

    public static CategoryOptionResponse toResponse(CatalogCategoryView category) {
        return CategoryOptionResponse.builder()
                .code(category.getCode())
                .value(category.getCode())
                .label(category.getLabel())
                .active(category.isActive())
                .sortOrder(category.getSortOrder())
                .build();
    }

    private static StickerDetailsCommand toCommand(com.euprocuro.api.entrypoints.rest.dto.request.StickerDetailsRequest request) {
        if (request == null) {
            return null;
        }
        return StickerDetailsCommand.builder()
                .type(request.getType())
                .group(request.getGroup())
                .selection(request.getSelection())
                .numbers(Optional.ofNullable(request.getNumbers()).orElse(List.of()))
                .players(Optional.ofNullable(request.getPlayers()).orElse(List.of()))
                .build();
    }

    private static StickerDetailsResponse toResponse(StickerDetails details) {
        if (details == null) {
            return null;
        }
        return StickerDetailsResponse.builder()
                .type(details.getType())
                .group(details.getGroup())
                .selection(details.getSelection())
                .numbers(Optional.ofNullable(details.getNumbers()).orElse(List.of()))
                .players(Optional.ofNullable(details.getPlayers()).orElse(List.of()))
                .build();
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static LocationResponse toResponse(LocationInfo location) {
        return toResponse(location, true);
    }

    private static LocationResponse toResponse(LocationInfo location, boolean exposeExactDetails) {
        return toResponse(location, exposeExactDetails, exposeExactDetails);
    }

    private static LocationResponse toResponse(LocationInfo location, boolean exposePostalCode, boolean exposeNeighborhood) {
        if (location == null) {
            return null;
        }

        return LocationResponse.builder()
                .postalCode(exposePostalCode ? location.getPostalCode() : null)
                .city(location.getCity())
                .state(location.getState())
                .neighborhood(exposeNeighborhood ? location.getNeighborhood() : null)
                .country(location.getCountry())
                .remote(location.isRemote())
                .build();
    }

    public static AddressLookupResponse toResponse(AddressLookupView view) {
        return AddressLookupResponse.builder()
                .postalCode(view.getPostalCode())
                .city(view.getCity())
                .state(view.getState())
                .neighborhood(view.getNeighborhood())
                .country(view.getCountry())
                .build();
    }
}
