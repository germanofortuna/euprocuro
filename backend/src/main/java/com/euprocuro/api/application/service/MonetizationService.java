package com.euprocuro.api.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.euprocuro.api.application.command.BoostInterestCommand;
import com.euprocuro.api.application.command.PurchaseProductCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ForbiddenException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.application.usecase.MonetizationUseCase;
import com.euprocuro.api.application.view.CheckoutView;
import com.euprocuro.api.application.view.MonetizationAccountView;
import com.euprocuro.api.application.view.MonetizationProductView;
import com.euprocuro.api.application.view.PaymentOrderView;
import com.euprocuro.api.domain.gateway.EmailGateway;
import com.euprocuro.api.domain.gateway.EventPublisherGateway;
import com.euprocuro.api.domain.gateway.InterestGateway;
import com.euprocuro.api.domain.gateway.PaymentCheckoutGateway;
import com.euprocuro.api.domain.gateway.PaymentOrderGateway;
import com.euprocuro.api.domain.gateway.PaymentStatusGateway;
import com.euprocuro.api.domain.gateway.UserGateway;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.MonetizationProductType;
import com.euprocuro.api.domain.model.PaymentOrder;
import com.euprocuro.api.domain.model.PaymentOrderStatus;
import com.euprocuro.api.domain.model.PaymentProviderStatus;
import com.euprocuro.api.domain.model.UserProfile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MonetizationService implements MonetizationUseCase {

    private static final String CREDITS_PAYMENT_METHOD = "CREDITS";

    private final UserGateway userGateway;
    private final InterestGateway interestGateway;
    private final EventPublisherGateway eventPublisherGateway;
    private final EmailGateway emailGateway;
    private final MonetizationCatalog monetizationCatalog;
    private final PaymentOrderGateway paymentOrderGateway;
    private final PaymentCheckoutGateway paymentCheckoutGateway;
    private final PaymentStatusGateway paymentStatusGateway;
    private final PublicCacheService publicCacheService;

    @Value("${application.monetization.provider:MERCADO_PAGO_CHECKOUT_PRO}")
    private String checkoutProvider = "MERCADO_PAGO_CHECKOUT_PRO";
    @Value("${application.monetization.local-checkout.base-url:http://localhost:8080/api/monetization/local-checkout/approve}")
    private String localCheckoutBaseUrl;
    @Value("${application.monetization.local-checkout.enabled:false}")
    private boolean localCheckoutEnabled;

    @Override
    public List<MonetizationProductView> listProducts() {
        return monetizationCatalog.products();
    }

    @Override
    public MonetizationAccountView getAccount(String userId) {
        UserProfile user = requireUser(userId);
        return MonetizationAccountView.builder()
                .sellerCredits(sellerCredits(user))
                .purchasedCreditsTotal(purchasedCreditsTotal(user))
                .subscriptionPlan(user.getSubscriptionPlan())
                .subscriptionActiveUntil(user.getSubscriptionActiveUntil())
                .subscriptionActive(hasActiveSubscription(user))
                .creditPurchasesEnabled(monetizationCatalog.creditPurchasesEnabled())
                .boostPurchasesEnabled(monetizationCatalog.boostPurchasesEnabled())
                .products(listProducts())
                .paymentHistory(paymentHistory(userId))
                .build();
    }

    @Override
    public CheckoutView purchase(String userId, PurchaseProductCommand command) {
        UserProfile user = requireUser(userId);
        MonetizationProductView product = requireProduct(command.getProductCode());

        if (product.getType() == MonetizationProductType.BOOST) {
            throw new BusinessException("Boost deve ser ativado diretamente no interesse.");
        }
        if (!monetizationCatalog.creditPurchasesEnabled()) {
            throw new BusinessException("Compra de creditos e planos esta temporariamente desabilitada.");
        }

        return createPendingCheckout(user, product, normalizePaymentMethod(command.getPaymentMethod()));
    }

    @Override
    public MonetizationAccountView cancelSubscription(String userId) {
        UserProfile user = requireUser(userId);
        if (!hasActiveSubscription(user)) {
            throw new BusinessException("Nenhum plano ativo para cancelar.");
        }

        UserProfile updatedUser = userGateway.save(user.toBuilder()
                .subscriptionPlan(null)
                .subscriptionActiveUntil(Instant.now())
                .build());

        eventPublisherGateway.publish("monetization.subscription.cancelled", Map.of(
                "userId", userId,
                "previousPlan", user.getSubscriptionPlan(),
                "activeUntil", user.getSubscriptionActiveUntil()
        ));

        return MonetizationAccountView.builder()
                .sellerCredits(sellerCredits(updatedUser))
                .purchasedCreditsTotal(purchasedCreditsTotal(updatedUser))
                .subscriptionPlan(updatedUser.getSubscriptionPlan())
                .subscriptionActiveUntil(updatedUser.getSubscriptionActiveUntil())
                .subscriptionActive(false)
                .creditPurchasesEnabled(monetizationCatalog.creditPurchasesEnabled())
                .boostPurchasesEnabled(monetizationCatalog.boostPurchasesEnabled())
                .products(listProducts())
                .paymentHistory(paymentHistory(userId))
                .build();
    }

    @Override
    public void confirmPayment(String providerPaymentId) {
        if (!StringUtils.hasText(providerPaymentId)) {
            return;
        }

        PaymentProviderStatus providerStatus = paymentStatusGateway.findPayment(providerPaymentId);
        if (!StringUtils.hasText(providerStatus.getExternalReference())) {
            throw new BusinessException("Pagamento recebido sem referencia externa.");
        }

        PaymentOrder paymentOrder = paymentOrderGateway.findById(providerStatus.getExternalReference())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de pagamento nao encontrado."));
        PaymentOrderStatus status = mapProviderStatus(providerStatus.getStatus());

        PaymentOrder updatedOrder = paymentOrder.toBuilder()
                .providerPaymentId(providerStatus.getPaymentId())
                .paymentMethod(normalizePaymentMethod(firstText(providerStatus.getPaymentMethod(), paymentOrder.getPaymentMethod())))
                .status(status)
                .updatedAt(Instant.now())
                .approvedAt(status == PaymentOrderStatus.APPROVED ? Instant.now() : paymentOrder.getApprovedAt())
                .build();

        if (paymentOrder.getStatus() == PaymentOrderStatus.APPROVED) {
            paymentOrderGateway.save(updatedOrder);
            return;
        }

        if (status == PaymentOrderStatus.APPROVED) {
            approvePaymentOrder(paymentOrder.toBuilder()
                    .providerPaymentId(providerStatus.getPaymentId())
                    .paymentMethod(normalizePaymentMethod(firstText(providerStatus.getPaymentMethod(), paymentOrder.getPaymentMethod())))
                    .updatedAt(Instant.now())
                    .build(), providerStatus.getPaymentId());
            return;
        }

        paymentOrderGateway.save(updatedOrder);
    }

    @Override
    public void approveLocalCheckout(String paymentOrderId) {
        PaymentOrder paymentOrder = paymentOrderGateway.findById(paymentOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de pagamento nao encontrado."));

        if (!isLocalCheckoutProvider(paymentOrder.getProvider())) {
            throw new ForbiddenException("Este pedido nao pertence ao checkout local.");
        }
        if (!localCheckoutEnabled) {
            throw new ForbiddenException("Checkout local desabilitado.");
        }

        approvePaymentOrder(paymentOrder, paymentOrder.getId());
    }

    @Override
    public CheckoutView boostInterest(String userId, String interestId, BoostInterestCommand command) {
        InterestPost interest = interestGateway.findById(interestId)
                .orElseThrow(() -> new ResourceNotFoundException("Interesse nao encontrado."));
        if (!Objects.equals(interest.getOwnerId(), userId)) {
            throw new ForbiddenException("Apenas o dono do interesse pode impulsionar este anuncio.");
        }
        if (isBoostActive(interest, Instant.now())) {
            throw new BusinessException("Esta procura ja possui um boost ativo. Aguarde o termino para impulsionar novamente.");
        }

        MonetizationProductView product = requireProduct(command.getBoostCode());
        if (product.getType() != MonetizationProductType.BOOST) {
            throw new BusinessException("Produto informado nao e um boost.");
        }
        if (!monetizationCatalog.boostPurchasesEnabled()) {
            throw new BusinessException("Compra de boosts esta temporariamente desabilitada.");
        }

        UserProfile owner = requireUser(userId);
        String paymentMethod = normalizePaymentMethod(command.getPaymentMethod());
        if (CREDITS_PAYMENT_METHOD.equals(paymentMethod)) {
            return activateBoostWithCredits(owner, interestId, product);
        }

        return createPendingCheckout(owner, product, paymentMethod, interestId);
    }

    private boolean isBoostActive(InterestPost interest, Instant now) {
        return interest.getBoostedUntil() != null && interest.getBoostedUntil().isAfter(now);
    }

    private CheckoutView activateBoostWithCredits(UserProfile owner, String interestId, MonetizationProductView product) {
        int creditCost = Optional.ofNullable(product.getCredits()).orElse(0);
        if (creditCost <= 0) {
            throw new BusinessException("Configure o custo em creditos deste boost no CRM.");
        }
        if (Optional.ofNullable(product.getDurationDays()).orElse(0) <= 0) {
            throw new BusinessException("Configure a duracao deste boost no CRM.");
        }
        int currentCredits = sellerCredits(owner);
        if (currentCredits < creditCost) {
            throw new BusinessException("Creditos insuficientes para ativar este boost.");
        }

        UserProfile updatedOwner = userGateway.save(owner.toBuilder()
                .sellerCredits(currentCredits - creditCost)
                .build());
        String creditOrderId = "credits-" + UUID.randomUUID();
        activateBoost(updatedOwner.getId(), interestId, product, CREDITS_PAYMENT_METHOD, creditOrderId);

        eventPublisherGateway.publish("monetization.boost.credits.completed", Map.of(
                "userId", updatedOwner.getId(),
                "interestId", interestId,
                "productCode", product.getCode(),
                "creditsUsed", creditCost,
                "remainingCredits", sellerCredits(updatedOwner)
        ));

        return CheckoutView.builder()
                .provider(CREDITS_PAYMENT_METHOD)
                .paymentMethod(CREDITS_PAYMENT_METHOD)
                .productCode(product.getCode())
                .paymentOrderId(creditOrderId)
                .providerPreferenceId(creditOrderId)
                .checkoutUrl("local://credits")
                .status("APPROVED")
                .message("Boost ativado usando creditos.")
                .build();
    }

    private UserProfile applyProductToUser(UserProfile user, MonetizationProductView product) {
        if (product.getType() == MonetizationProductType.CREDIT_PACK) {
            return user.toBuilder()
                    .sellerCredits(sellerCredits(user) + product.getCredits())
                    .purchasedCreditsTotal(purchasedCreditsTotal(user) + product.getCredits())
                    .build();
        }

        if (product.getType() == MonetizationProductType.SUBSCRIPTION) {
            Instant now = Instant.now();
            Instant currentExpiration = user.getSubscriptionActiveUntil() != null && user.getSubscriptionActiveUntil().isAfter(now)
                    ? user.getSubscriptionActiveUntil()
                    : now;
            return user.toBuilder()
                    .subscriptionPlan(product.getCode())
                    .subscriptionActiveUntil(currentExpiration.plus(product.getDurationDays(), ChronoUnit.DAYS))
                    .build();
        }

        throw new BusinessException("Produto nao suportado para compra direta.");
    }

    private MonetizationProductView requireProduct(String productCode) {
        return monetizationCatalog.findByCode(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Produto de monetizacao nao encontrado."));
    }

    private CheckoutView createPendingCheckout(UserProfile user, MonetizationProductView product, String paymentMethod) {
        return createPendingCheckout(user, product, paymentMethod, null);
    }

    private CheckoutView createPendingCheckout(
            UserProfile user,
            MonetizationProductView product,
            String paymentMethod,
            String boostInterestId
    ) {
        Instant now = Instant.now();
        PaymentOrder paymentOrder = PaymentOrder.builder()
                .id(UUID.randomUUID().toString())
                .userId(user.getId())
                .userEmail(user.getEmail())
                .productCode(product.getCode())
                .productName(product.getName())
                .boostInterestId(boostInterestId)
                .amount(product.getPrice())
                .paymentMethod(paymentMethod)
                .provider(checkoutProvider)
                .status(PaymentOrderStatus.CREATED)
                .createdAt(now)
                .updatedAt(now)
                .build();

        paymentOrder = paymentOrderGateway.save(paymentOrder);
        CheckoutView checkout = isLocalCheckoutProvider()
                ? createLocalCheckout(product, paymentOrder)
                : createMercadoPagoCheckout(user, product, paymentOrder);

        paymentOrderGateway.save(paymentOrder.toBuilder()
                .providerPreferenceId(checkout.getProviderPreferenceId())
                .checkoutUrl(checkout.getCheckoutUrl())
                .status(PaymentOrderStatus.PENDING)
                .updatedAt(Instant.now())
                .build());

        eventPublisherGateway.publish("monetization.purchase.created", Map.of(
                "userId", user.getId(),
                "productCode", product.getCode(),
                "paymentMethod", paymentMethod,
                "provider", checkoutProvider,
                "paymentOrderId", paymentOrder.getId()
        ));

        return checkout;
    }

    private CheckoutView createMercadoPagoCheckout(UserProfile user, MonetizationProductView product, PaymentOrder paymentOrder) {
        try {
            return paymentCheckoutGateway.createCheckout(user, product, paymentOrder);
        } catch (RuntimeException exception) {
            paymentOrderGateway.save(paymentOrder.toBuilder()
                    .status(PaymentOrderStatus.REJECTED)
                    .updatedAt(Instant.now())
                    .build());
            throw new BusinessException(exception.getMessage());
        }
    }

    private CheckoutView createLocalCheckout(MonetizationProductView product, PaymentOrder paymentOrder) {
        if (!localCheckoutEnabled) {
            throw new BusinessException("Checkout local desabilitado.");
        }

        return CheckoutView.builder()
                .provider(paymentOrder.getProvider())
                .paymentMethod(paymentOrder.getPaymentMethod())
                .productCode(product.getCode())
                .paymentOrderId(paymentOrder.getId())
                .providerPreferenceId("local-" + paymentOrder.getId())
                .checkoutUrl(localCheckoutBaseUrl + "/" + paymentOrder.getId())
                .status("PENDING")
                .message("Checkout local criado. Ao abrir o link, o pagamento sera aprovado automaticamente.")
                .build();
    }

    private boolean isLocalCheckoutProvider() {
        return isLocalCheckoutProvider(checkoutProvider);
    }

    private boolean isLocalCheckoutProvider(String provider) {
        return "LOCAL_CHECKOUT_MOCK".equalsIgnoreCase(provider)
                || "LOCAL_MOCK".equalsIgnoreCase(provider)
                || "MERCADO_PAGO_LOCAL_MOCK".equalsIgnoreCase(provider);
    }

    private PaymentOrderStatus mapProviderStatus(String status) {
        if ("approved".equalsIgnoreCase(status)) {
            return PaymentOrderStatus.APPROVED;
        }
        if ("cancelled".equalsIgnoreCase(status)) {
            return PaymentOrderStatus.CANCELLED;
        }
        if ("rejected".equalsIgnoreCase(status)) {
            return PaymentOrderStatus.REJECTED;
        }
        return PaymentOrderStatus.PENDING;
    }

    private void approvePaymentOrder(PaymentOrder paymentOrder, String providerPaymentId) {
        String normalizedPaymentMethod = normalizePaymentMethod(paymentOrder.getPaymentMethod());

        if (paymentOrder.getStatus() == PaymentOrderStatus.APPROVED) {
            paymentOrderGateway.save(paymentOrder.toBuilder()
                .providerPaymentId(providerPaymentId)
                .paymentMethod(normalizedPaymentMethod)
                .updatedAt(Instant.now())
                .build());
            return;
        }

        MonetizationProductView product = requireProduct(paymentOrder.getProductCode());
        if (product.getType() == MonetizationProductType.BOOST) {
            if (!monetizationCatalog.boostPurchasesEnabled()) {
                throw new BusinessException("Compra de boosts esta temporariamente desabilitada.");
            }
            activateBoost(paymentOrder.getUserId(), paymentOrder.getBoostInterestId(), product, normalizedPaymentMethod, paymentOrder.getId());
        } else {
            if (!monetizationCatalog.creditPurchasesEnabled()) {
                throw new BusinessException("Compra de creditos e planos esta temporariamente desabilitada.");
            }
            UserProfile user = requireUser(paymentOrder.getUserId());
            UserProfile updatedUser = userGateway.save(applyProductToUser(user, product));
            emailGateway.sendPurchaseConfirmationEmail(updatedUser, product.getName(), normalizedPaymentMethod);
        }

        eventPublisherGateway.publish("monetization.purchase.completed", Map.of(
                "userId", paymentOrder.getUserId(),
                "productCode", product.getCode(),
                "paymentMethod", normalizedPaymentMethod,
                "provider", paymentOrder.getProvider(),
                "paymentOrderId", paymentOrder.getId()
        ));

        paymentOrderGateway.save(paymentOrder.toBuilder()
                .providerPaymentId(providerPaymentId)
                .paymentMethod(normalizedPaymentMethod)
                .status(PaymentOrderStatus.APPROVED)
                .updatedAt(Instant.now())
                .approvedAt(Instant.now())
                .build());
    }

    private UserProfile requireUser(String userId) {
        return userGateway.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."));
    }

    private int sellerCredits(UserProfile user) {
        return user.getSellerCredits() == null ? 0 : user.getSellerCredits();
    }

    private int purchasedCreditsTotal(UserProfile user) {
        return user.getPurchasedCreditsTotal() == null ? 0 : user.getPurchasedCreditsTotal();
    }

    private boolean hasActiveSubscription(UserProfile user) {
        return user.getSubscriptionActiveUntil() != null && user.getSubscriptionActiveUntil().isAfter(Instant.now());
    }

    private List<PaymentOrderView> paymentHistory(String userId) {
        return paymentOrderGateway.findRecentByUserId(userId)
                .stream()
                .map(this::toPaymentOrderView)
                .collect(Collectors.toList());
    }

    private PaymentOrderView toPaymentOrderView(PaymentOrder paymentOrder) {
        return PaymentOrderView.builder()
                .id(paymentOrder.getId())
                .productCode(paymentOrder.getProductCode())
                .productName(paymentOrder.getProductName())
                .amount(paymentOrder.getAmount())
                .paymentMethod(paymentOrder.getPaymentMethod())
                .provider(paymentOrder.getProvider())
                .status(paymentOrder.getStatus())
                .providerPaymentId(paymentOrder.getProviderPaymentId())
                .createdAt(paymentOrder.getCreatedAt())
                .updatedAt(paymentOrder.getUpdatedAt())
                .approvedAt(paymentOrder.getApprovedAt())
                .build();
    }

    private InterestPost activateBoost(
            String userId,
            String interestId,
            MonetizationProductView product,
            String paymentMethod,
            String paymentOrderId
    ) {
        if (!StringUtils.hasText(interestId)) {
            throw new BusinessException("Pedido de boost sem interesse vinculado.");
        }

        InterestPost interest = interestGateway.findById(interestId)
                .orElseThrow(() -> new ResourceNotFoundException("Interesse nao encontrado."));
        if (!Objects.equals(interest.getOwnerId(), userId)) {
            throw new ForbiddenException("Pedido de boost nao pertence ao dono do interesse.");
        }

        Instant now = Instant.now();
        int durationDays = Optional.ofNullable(product.getDurationDays()).orElse(0);
        if (durationDays <= 0) {
            throw new BusinessException("Configure a duracao deste boost no CRM.");
        }
        Instant boostedUntil = now.plus(durationDays, ChronoUnit.DAYS);

        InterestPost saved = interestGateway.save(interest.toBuilder()
                .boostedUntil(boostedUntil)
                .updatedAt(now)
                .build());
        publicCacheService.invalidate(PublicCacheService.MARKETPLACE);

        UserProfile owner = requireUser(userId);
        emailGateway.sendBoostActivatedEmail(owner, saved.getTitle(), boostedUntil.toString());
        eventPublisherGateway.publish("interest.boosted", Map.of(
                "interestId", interestId,
                "ownerId", userId,
                "boostCode", product.getCode(),
                "boostedUntil", boostedUntil,
                "paymentMethod", paymentMethod,
                "provider", checkoutProvider,
                "paymentOrderId", paymentOrderId
        ));
        return saved;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String normalizePaymentMethod(String paymentMethod) {
        return paymentMethod == null || paymentMethod.isBlank() ? "PIX" : paymentMethod.trim().toUpperCase();
    }
}
