package com.euprocuro.api.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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

    private final UserGateway userGateway;
    private final InterestGateway interestGateway;
    private final EventPublisherGateway eventPublisherGateway;
    private final EmailGateway emailGateway;
    private final MonetizationCatalog monetizationCatalog;
    private final PaymentOrderGateway paymentOrderGateway;
    private final PaymentCheckoutGateway paymentCheckoutGateway;
    private final PaymentStatusGateway paymentStatusGateway;

    @Value("${application.monetization.provider:LOCAL_MOCK}")
    private String checkoutProvider = "LOCAL_MOCK";
    @Value("${application.monetization.local-checkout.base-url:http://localhost:8080/api/monetization/local-checkout/approve}")
    private String localCheckoutBaseUrl;

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
                .products(listProducts())
                .build();
    }

    @Override
    public CheckoutView purchase(String userId, PurchaseProductCommand command) {
        UserProfile user = requireUser(userId);
        MonetizationProductView product = requireProduct(command.getProductCode());

        if (product.getType() == MonetizationProductType.BOOST) {
            throw new BusinessException("Boost deve ser ativado diretamente no interesse.");
        }

        if (isCheckoutProviderWithPendingOrder()) {
            return createPendingCheckout(user, product, normalizePaymentMethod(command.getPaymentMethod()));
        }

        UserProfile updatedUser = applyProductToUser(user, product);
        updatedUser = userGateway.save(updatedUser);
        emailGateway.sendPurchaseConfirmationEmail(
                updatedUser,
                product.getName(),
                normalizePaymentMethod(command.getPaymentMethod())
        );

        eventPublisherGateway.publish("monetization.purchase.completed", Map.of(
                "userId", userId,
                "productCode", product.getCode(),
                "paymentMethod", normalizePaymentMethod(command.getPaymentMethod()),
                "provider", checkoutProvider
        ));

        return CheckoutView.builder()
                .provider(checkoutProvider)
                .paymentMethod(normalizePaymentMethod(command.getPaymentMethod()))
                .productCode(product.getCode())
                .status("APPROVED")
                .checkoutUrl("local://checkout/" + product.getCode())
                .message("Pagamento simulado aprovado. Em producao, este fluxo sera confirmado por webhook do gateway.")
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
                .status(status)
                .updatedAt(Instant.now())
                .approvedAt(status == PaymentOrderStatus.APPROVED ? Instant.now() : paymentOrder.getApprovedAt())
                .build();

        if (paymentOrder.getStatus() == PaymentOrderStatus.APPROVED) {
            paymentOrderGateway.save(updatedOrder);
            return;
        }

        if (status == PaymentOrderStatus.APPROVED) {
            approvePaymentOrder(paymentOrder, providerStatus.getPaymentId());
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

        approvePaymentOrder(paymentOrder, paymentOrder.getId());
    }

    @Override
    public InterestPost boostInterest(String userId, String interestId, BoostInterestCommand command) {
        InterestPost interest = interestGateway.findById(interestId)
                .orElseThrow(() -> new ResourceNotFoundException("Interesse nao encontrado."));
        if (!Objects.equals(interest.getOwnerId(), userId)) {
            throw new ForbiddenException("Apenas o dono do interesse pode impulsionar este anuncio.");
        }

        MonetizationProductView product = requireProduct(command.getBoostCode());
        if (product.getType() != MonetizationProductType.BOOST) {
            throw new BusinessException("Produto informado nao e um boost.");
        }

        Instant now = Instant.now();
        Instant currentExpiration = interest.getBoostedUntil() != null && interest.getBoostedUntil().isAfter(now)
                ? interest.getBoostedUntil()
                : now;
        Instant boostedUntil = currentExpiration.plus(product.getDurationDays(), ChronoUnit.DAYS);

        InterestPost boosted = interest.toBuilder()
                .boostEnabled(true)
                .boostedUntil(boostedUntil)
                .updatedAt(now)
                .build();

        InterestPost saved = interestGateway.save(boosted);
        UserProfile owner = requireUser(userId);
        emailGateway.sendBoostActivatedEmail(owner, saved.getTitle(), boostedUntil.toString());
        eventPublisherGateway.publish("interest.boosted", Map.of(
                "interestId", interestId,
                "ownerId", userId,
                "boostCode", product.getCode(),
                "boostedUntil", boostedUntil,
                "paymentMethod", normalizePaymentMethod(command.getPaymentMethod()),
                "provider", checkoutProvider
        ));
        return saved;
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
        Instant now = Instant.now();
        PaymentOrder paymentOrder = PaymentOrder.builder()
                .id(UUID.randomUUID().toString())
                .userId(user.getId())
                .userEmail(user.getEmail())
                .productCode(product.getCode())
                .productName(product.getName())
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

    private boolean isCheckoutProviderWithPendingOrder() {
        return isLocalCheckoutProvider()
                || "MERCADO_PAGO_CHECKOUT_PRO".equalsIgnoreCase(checkoutProvider)
                || "MERCADO_PAGO".equalsIgnoreCase(checkoutProvider)
                || "MERCADOPAGO".equalsIgnoreCase(checkoutProvider);
    }

    private boolean isLocalCheckoutProvider() {
        return isLocalCheckoutProvider(checkoutProvider);
    }

    private boolean isLocalCheckoutProvider(String provider) {
        return "LOCAL_CHECKOUT_MOCK".equalsIgnoreCase(provider)
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
        if (paymentOrder.getStatus() == PaymentOrderStatus.APPROVED) {
            paymentOrderGateway.save(paymentOrder.toBuilder()
                    .providerPaymentId(providerPaymentId)
                    .updatedAt(Instant.now())
                    .build());
            return;
        }

        MonetizationProductView product = requireProduct(paymentOrder.getProductCode());
        UserProfile user = requireUser(paymentOrder.getUserId());
        UserProfile updatedUser = userGateway.save(applyProductToUser(user, product));
        emailGateway.sendPurchaseConfirmationEmail(updatedUser, product.getName(), paymentOrder.getPaymentMethod());
        eventPublisherGateway.publish("monetization.purchase.completed", Map.of(
                "userId", paymentOrder.getUserId(),
                "productCode", product.getCode(),
                "paymentMethod", paymentOrder.getPaymentMethod(),
                "provider", paymentOrder.getProvider(),
                "paymentOrderId", paymentOrder.getId()
        ));

        paymentOrderGateway.save(paymentOrder.toBuilder()
                .providerPaymentId(providerPaymentId)
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

    private String normalizePaymentMethod(String paymentMethod) {
        return paymentMethod == null || paymentMethod.isBlank() ? "PIX" : paymentMethod.trim().toUpperCase();
    }
}
