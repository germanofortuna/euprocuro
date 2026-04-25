package com.euprocuro.api.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.euprocuro.api.application.command.BoostInterestCommand;
import com.euprocuro.api.application.command.PurchaseProductCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ForbiddenException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.application.usecase.MonetizationUseCase;
import com.euprocuro.api.application.view.CheckoutView;
import com.euprocuro.api.application.view.MonetizationAccountView;
import com.euprocuro.api.application.view.MonetizationProductView;
import com.euprocuro.api.domain.gateway.EventPublisherGateway;
import com.euprocuro.api.domain.gateway.EmailGateway;
import com.euprocuro.api.domain.gateway.InterestGateway;
import com.euprocuro.api.domain.gateway.UserGateway;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.MonetizationProductType;
import com.euprocuro.api.domain.model.UserProfile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MonetizationService implements MonetizationUseCase {

    private final UserGateway userGateway;
    private final InterestGateway interestGateway;
    private final EventPublisherGateway eventPublisherGateway;
    private final EmailGateway emailGateway;

    @Value("${application.monetization.provider:LOCAL_MOCK}")
    private String checkoutProvider = "LOCAL_MOCK";

    @Override
    public List<MonetizationProductView> listProducts() {
        return MonetizationCatalog.products();
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
                .checkoutUrl("local://checkout/" + product.getCode())
                .message("Pagamento simulado aprovado. Em producao, este fluxo sera confirmado por webhook do gateway.")
                .build();
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
        return MonetizationCatalog.findByCode(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Produto de monetizacao nao encontrado."));
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
