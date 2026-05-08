package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.euprocuro.api.application.command.BoostInterestCommand;
import com.euprocuro.api.application.command.PurchaseProductCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ForbiddenException;
import com.euprocuro.api.application.view.CheckoutView;
import com.euprocuro.api.application.view.MonetizationAccountView;
import com.euprocuro.api.domain.gateway.EmailGateway;
import com.euprocuro.api.domain.gateway.EventPublisherGateway;
import com.euprocuro.api.domain.gateway.InterestGateway;
import com.euprocuro.api.domain.gateway.PaymentCheckoutGateway;
import com.euprocuro.api.domain.gateway.PaymentOrderGateway;
import com.euprocuro.api.domain.gateway.PaymentStatusGateway;
import com.euprocuro.api.domain.gateway.UserGateway;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestStatus;
import com.euprocuro.api.domain.model.PaymentOrder;
import com.euprocuro.api.domain.model.PaymentOrderStatus;
import com.euprocuro.api.domain.model.PaymentProviderStatus;
import com.euprocuro.api.domain.model.MonetizationProductType;
import com.euprocuro.api.domain.model.UserProfile;
import com.euprocuro.api.application.view.MonetizationProductView;

@ExtendWith(MockitoExtension.class)
class MonetizationServiceTest {

    @Mock
    private UserGateway userGateway;
    @Mock
    private InterestGateway interestGateway;
    @Mock
    private EventPublisherGateway eventPublisherGateway;
    @Mock
    private EmailGateway emailGateway;
    @Mock
    private MonetizationCatalog monetizationCatalog;
    @Mock
    private PaymentOrderGateway paymentOrderGateway;
    @Mock
    private PaymentCheckoutGateway paymentCheckoutGateway;
    @Mock
    private PaymentStatusGateway paymentStatusGateway;
    @Mock
    private PublicCacheService publicCacheService;

    @InjectMocks
    private MonetizationService monetizationService;

    @BeforeEach
    void setUpCatalog() {
        ReflectionTestUtils.setField(monetizationService, "checkoutProvider", "LOCAL_CHECKOUT_MOCK");
        ReflectionTestUtils.setField(monetizationService, "localCheckoutEnabled", true);
        List<MonetizationProductView> products = defaultProducts();
        lenient().when(monetizationCatalog.products()).thenReturn(products);
        lenient().when(monetizationCatalog.creditPurchasesEnabled()).thenReturn(true);
        lenient().when(monetizationCatalog.boostPurchasesEnabled()).thenReturn(true);
        lenient().when(monetizationCatalog.findByCode(anyString())).thenReturn(Optional.empty());
        for (MonetizationProductView product : products) {
            lenient().when(monetizationCatalog.findByCode(product.getCode())).thenReturn(Optional.of(product));
        }
    }

    private List<MonetizationProductView> defaultProducts() {
        return List.of(
                MonetizationProductView.builder()
                        .code("CREDITS_10")
                        .name("10 propostas")
                        .description("Pacote para vendedores enviarem propostas avulsas.")
                        .type(MonetizationProductType.CREDIT_PACK)
                        .price(new BigDecimal("9.90"))
                        .credits(10)
                        .enabled(true)
                        .build(),
                MonetizationProductView.builder()
                        .code("CREDITS_30")
                        .name("30 propostas")
                        .description("Mais volume para vendedores frequentes.")
                        .type(MonetizationProductType.CREDIT_PACK)
                        .price(new BigDecimal("24.90"))
                        .credits(30)
                        .enabled(true)
                        .build(),
                MonetizationProductView.builder()
                        .code("SELLER_PRO")
                        .name("Plano vendedor Pro")
                        .description("Propostas ilimitadas por 30 dias neste MVP.")
                        .type(MonetizationProductType.SUBSCRIPTION)
                        .price(new BigDecimal("49.90"))
                        .durationDays(30)
                        .enabled(true)
                        .build(),
                MonetizationProductView.builder()
                        .code("BOOST_3_DAYS")
                        .name("Boost 3 dias")
                        .description("Impulsiona o interesse na busca e na home.")
                        .type(MonetizationProductType.BOOST)
                        .price(new BigDecimal("9.90"))
                        .durationDays(3)
                        .enabled(true)
                        .build()
        );
    }

    @Test
    void getAccountShouldReturnCreditsPlanAndProducts() {
        PaymentOrder paymentOrder = PaymentOrder.builder()
                .id("order-1")
                .productCode("CREDITS_10")
                .productName("10 propostas")
                .amount(new BigDecimal("9.90"))
                .paymentMethod("PIX")
                .provider("LOCAL_MOCK")
                .status(PaymentOrderStatus.APPROVED)
                .providerPaymentId("provider-1")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .approvedAt(Instant.now())
                .build();
        UserProfile user = baseUser().toBuilder()
                .sellerCredits(4)
                .purchasedCreditsTotal(10)
                .subscriptionPlan("SELLER_PRO")
                .subscriptionActiveUntil(Instant.now().plusSeconds(3600))
                .build();
        when(userGateway.findById("user-1")).thenReturn(Optional.of(user));
        when(paymentOrderGateway.findRecentByUserId("user-1")).thenReturn(java.util.List.of(paymentOrder));

        MonetizationAccountView result = monetizationService.getAccount("user-1");

        assertThat(result.getSellerCredits()).isEqualTo(4);
        assertThat(result.getPurchasedCreditsTotal()).isEqualTo(10);
        assertThat(result.isSubscriptionActive()).isTrue();
        assertThat(result.getProducts()).isNotEmpty();
        assertThat(result.getPaymentHistory()).hasSize(1);
        assertThat(result.getPaymentHistory().get(0).getProductName()).isEqualTo("10 propostas");
    }

    @Test
    void purchaseShouldCreatePendingLocalCheckoutWithoutAddingCredits() {
        when(userGateway.findById("user-1")).thenReturn(Optional.of(baseUser()));
        when(paymentOrderGateway.save(any(PaymentOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutView result = monetizationService.purchase("user-1", PurchaseProductCommand.builder()
                .productCode("CREDITS_10")
                .paymentMethod("pix")
                .build());

        assertThat(result.getProvider()).isEqualTo("LOCAL_CHECKOUT_MOCK");
        assertThat(result.getProductCode()).isEqualTo("CREDITS_10");
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getCheckoutUrl()).contains("/local-checkout/approve/");
        verify(userGateway, never()).save(any(UserProfile.class));
        verify(emailGateway, never()).sendPurchaseConfirmationEmail(any(UserProfile.class), anyString(), anyString());
        verify(eventPublisherGateway).publish(eq("monetization.purchase.created"), any(Map.class));
    }

    @Test
    void purchaseShouldCreatePendingSubscriptionCheckout() {
        when(userGateway.findById("user-1")).thenReturn(Optional.of(baseUser()));
        when(paymentOrderGateway.save(any(PaymentOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutView result = monetizationService.purchase("user-1", PurchaseProductCommand.builder()
                .productCode("SELLER_PRO")
                .paymentMethod("CREDIT_CARD")
                .build());

        assertThat(result.getStatus()).isEqualTo("PENDING");
        verify(userGateway, never()).save(any(UserProfile.class));
        verify(emailGateway, never()).sendPurchaseConfirmationEmail(any(UserProfile.class), anyString(), anyString());
    }

    @Test
    void purchaseShouldNotExtendExistingActiveSubscriptionBeforePaymentApproval() {
        UserProfile user = baseUser().toBuilder()
                .subscriptionActiveUntil(Instant.now().plus(10, ChronoUnit.DAYS))
                .build();
        when(userGateway.findById("user-1")).thenReturn(Optional.of(user));
        when(paymentOrderGateway.save(any(PaymentOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        monetizationService.purchase("user-1", PurchaseProductCommand.builder()
                .productCode("SELLER_PRO")
                .paymentMethod("PIX")
                .build());

        verify(userGateway, never()).save(any(UserProfile.class));
    }

    @Test
    void cancelSubscriptionShouldEndActivePlan() {
        UserProfile user = baseUser().toBuilder()
                .subscriptionPlan("SELLER_PRO")
                .subscriptionActiveUntil(Instant.now().plusSeconds(3600))
                .build();
        when(userGateway.findById("user-1")).thenReturn(Optional.of(user));
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MonetizationAccountView result = monetizationService.cancelSubscription("user-1");

        assertThat(result.isSubscriptionActive()).isFalse();
        verify(userGateway).save(org.mockito.ArgumentMatchers.argThat(savedUser ->
                savedUser.getSubscriptionPlan() == null
                        && savedUser.getSubscriptionActiveUntil().isBefore(Instant.now().plusSeconds(5))
        ));
        verify(eventPublisherGateway).publish(eq("monetization.subscription.cancelled"), any(Map.class));
    }

    @Test
    void cancelSubscriptionShouldRejectUserWithoutActivePlan() {
        when(userGateway.findById("user-1")).thenReturn(Optional.of(baseUser()));

        assertThatThrownBy(() -> monetizationService.cancelSubscription("user-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Nenhum plano ativo");
        verify(userGateway, never()).save(any(UserProfile.class));
    }

    @Test
    void purchaseShouldRejectBoostProduct() {
        when(userGateway.findById("user-1")).thenReturn(Optional.of(baseUser()));

        assertThatThrownBy(() -> monetizationService.purchase("user-1", PurchaseProductCommand.builder()
                .productCode("BOOST_3_DAYS")
                .paymentMethod("PIX")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Boost");
    }

    @Test
    void purchaseShouldRejectWhenCreditPurchasesAreDisabled() {
        when(monetizationCatalog.creditPurchasesEnabled()).thenReturn(false);
        when(userGateway.findById("user-1")).thenReturn(Optional.of(baseUser()));

        assertThatThrownBy(() -> monetizationService.purchase("user-1", PurchaseProductCommand.builder()
                .productCode("CREDITS_10")
                .paymentMethod("PIX")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("creditos");
        verify(paymentOrderGateway, never()).save(any(PaymentOrder.class));
    }

    @Test
    void mercadoPagoPurchaseShouldCreateCheckoutWithoutAddingCreditsImmediately() {
        ReflectionTestUtils.setField(monetizationService, "checkoutProvider", "MERCADO_PAGO_CHECKOUT_PRO");
        when(userGateway.findById("user-1")).thenReturn(Optional.of(baseUser()));
        when(paymentOrderGateway.save(any(PaymentOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentCheckoutGateway.createCheckout(any(UserProfile.class), any(), any(PaymentOrder.class)))
                .thenReturn(CheckoutView.builder()
                        .provider("MERCADO_PAGO_CHECKOUT_PRO")
                        .paymentMethod("PIX")
                        .productCode("CREDITS_10")
                        .paymentOrderId("order-1")
                        .providerPreferenceId("pref-1")
                        .checkoutUrl("https://sandbox.mercadopago.com/checkout")
                        .status("PENDING")
                        .message("Checkout criado.")
                        .build());

        CheckoutView result = monetizationService.purchase("user-1", PurchaseProductCommand.builder()
                .productCode("CREDITS_10")
                .paymentMethod("PIX")
                .build());

        assertThat(result.getCheckoutUrl()).contains("mercadopago");
        assertThat(result.getStatus()).isEqualTo("PENDING");
        verify(userGateway, never()).save(any(UserProfile.class));
        verify(emailGateway, never()).sendPurchaseConfirmationEmail(any(), any(), any());
        verify(eventPublisherGateway).publish(eq("monetization.purchase.created"), any(Map.class));
    }

    @Test
    void mercadoPagoPurchaseShouldMarkOrderRejectedWhenCheckoutGatewayFails() {
        ReflectionTestUtils.setField(monetizationService, "checkoutProvider", "MERCADO_PAGO_CHECKOUT_PRO");
        when(userGateway.findById("user-1")).thenReturn(Optional.of(baseUser()));
        when(paymentOrderGateway.save(any(PaymentOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentCheckoutGateway.createCheckout(any(UserProfile.class), any(), any(PaymentOrder.class)))
                .thenThrow(new RuntimeException("gateway fora"));

        assertThatThrownBy(() -> monetizationService.purchase("user-1", PurchaseProductCommand.builder()
                .productCode("CREDITS_10")
                .paymentMethod("PIX")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("gateway fora");

        verify(paymentOrderGateway).save(org.mockito.ArgumentMatchers.argThat(order ->
                order.getStatus() == PaymentOrderStatus.REJECTED
        ));
    }

    @Test
    void localCheckoutPurchaseShouldCreatePendingOrderWithLocalApprovalUrl() {
        ReflectionTestUtils.setField(monetizationService, "checkoutProvider", "LOCAL_CHECKOUT_MOCK");
        ReflectionTestUtils.setField(
                monetizationService,
                "localCheckoutBaseUrl",
                "http://localhost:8080/api/monetization/local-checkout/approve"
        );
        when(userGateway.findById("user-1")).thenReturn(Optional.of(baseUser()));
        when(paymentOrderGateway.save(any(PaymentOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutView result = monetizationService.purchase("user-1", PurchaseProductCommand.builder()
                .productCode("CREDITS_10")
                .paymentMethod("PIX")
                .build());

        assertThat(result.getProvider()).isEqualTo("LOCAL_CHECKOUT_MOCK");
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getCheckoutUrl()).contains("/local-checkout/approve/");
        verify(userGateway, never()).save(any(UserProfile.class));
        verify(eventPublisherGateway).publish(eq("monetization.purchase.created"), any(Map.class));
    }

    @Test
    void approveLocalCheckoutShouldReleaseCredits() {
        PaymentOrder paymentOrder = PaymentOrder.builder()
                .id("order-1")
                .userId("user-1")
                .productCode("CREDITS_10")
                .productName("10 propostas")
                .paymentMethod("PIX")
                .provider("LOCAL_CHECKOUT_MOCK")
                .status(PaymentOrderStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(paymentOrderGateway.findById("order-1")).thenReturn(Optional.of(paymentOrder));
        when(userGateway.findById("user-1")).thenReturn(Optional.of(baseUser()));
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        monetizationService.approveLocalCheckout("order-1");

        verify(userGateway).save(org.mockito.ArgumentMatchers.argThat(user ->
                user.getSellerCredits() == 10 && user.getPurchasedCreditsTotal() == 10
        ));
        verify(paymentOrderGateway).save(org.mockito.ArgumentMatchers.argThat(order ->
                order.getStatus() == PaymentOrderStatus.APPROVED && "order-1".equals(order.getProviderPaymentId())
        ));
    }

    @Test
    void approveLocalCheckoutShouldRejectNonLocalProvider() {
        when(paymentOrderGateway.findById("order-1")).thenReturn(Optional.of(PaymentOrder.builder()
                .id("order-1")
                .provider("MERCADO_PAGO_CHECKOUT_PRO")
                .status(PaymentOrderStatus.PENDING)
                .build()));

        assertThatThrownBy(() -> monetizationService.approveLocalCheckout("order-1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("checkout local");
    }


    @Test
    void approveLocalCheckoutShouldRejectWhenLocalCheckoutIsDisabled() {
        ReflectionTestUtils.setField(monetizationService, "localCheckoutEnabled", false);
        when(paymentOrderGateway.findById("order-1")).thenReturn(Optional.of(PaymentOrder.builder()
                .id("order-1")
                .provider("LOCAL_CHECKOUT_MOCK")
                .status(PaymentOrderStatus.PENDING)
                .build()));

        assertThatThrownBy(() -> monetizationService.approveLocalCheckout("order-1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Checkout local desabilitado");
    }

    @Test
    void confirmPaymentShouldIgnoreBlankPaymentId() {
        monetizationService.confirmPayment(" ");

        verifyNoInteractions(paymentStatusGateway);
    }

    @Test
    void confirmPaymentShouldReleaseCreditsWhenMercadoPagoApprovesPayment() {
        PaymentOrder paymentOrder = PaymentOrder.builder()
                .id("order-1")
                .userId("user-1")
                .productCode("CREDITS_10")
                .productName("10 propostas")
                .paymentMethod("PIX")
                .provider("MERCADO_PAGO_CHECKOUT_PRO")
                .status(PaymentOrderStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(paymentStatusGateway.findPayment("123")).thenReturn(PaymentProviderStatus.builder()
                .paymentId("123")
                .status("approved")
                .externalReference("order-1")
                .paymentMethod("pix")
                .build());
        when(paymentOrderGateway.findById("order-1")).thenReturn(Optional.of(paymentOrder));
        when(userGateway.findById("user-1")).thenReturn(Optional.of(baseUser()));
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        monetizationService.confirmPayment("123");

        verify(userGateway).save(org.mockito.ArgumentMatchers.argThat(user ->
                user.getSellerCredits() == 10 && user.getPurchasedCreditsTotal() == 10
        ));
        verify(paymentOrderGateway).save(org.mockito.ArgumentMatchers.argThat(order ->
                order.getStatus() == PaymentOrderStatus.APPROVED
                        && "123".equals(order.getProviderPaymentId())
                        && "PIX".equals(order.getPaymentMethod())
        ));
        verify(eventPublisherGateway).publish(eq("monetization.purchase.completed"), any(Map.class));
    }

    @Test
    void confirmPaymentShouldRejectPaymentWithoutExternalReference() {
        when(paymentStatusGateway.findPayment("123")).thenReturn(PaymentProviderStatus.builder()
                .paymentId("123")
                .status("approved")
                .externalReference(" ")
                .build());

        assertThatThrownBy(() -> monetizationService.confirmPayment("123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("referencia externa");
    }

    @Test
    void confirmPaymentShouldMapCancelledProviderStatus() {
        PaymentOrder paymentOrder = PaymentOrder.builder()
                .id("order-1")
                .userId("user-1")
                .productCode("CREDITS_10")
                .paymentMethod("PIX")
                .provider("MERCADO_PAGO_CHECKOUT_PRO")
                .status(PaymentOrderStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(paymentStatusGateway.findPayment("123")).thenReturn(PaymentProviderStatus.builder()
                .paymentId("123")
                .status("cancelled")
                .externalReference("order-1")
                .build());
        when(paymentOrderGateway.findById("order-1")).thenReturn(Optional.of(paymentOrder));

        monetizationService.confirmPayment("123");

        verify(paymentOrderGateway).save(org.mockito.ArgumentMatchers.argThat(order ->
                order.getStatus() == PaymentOrderStatus.CANCELLED
        ));
    }

    @Test
    void confirmPaymentShouldMapUnknownProviderStatusAsPendingAndFallbackPaymentMethod() {
        PaymentOrder paymentOrder = PaymentOrder.builder()
                .id("order-1")
                .userId("user-1")
                .productCode("CREDITS_10")
                .paymentMethod(null)
                .provider("MERCADO_PAGO_CHECKOUT_PRO")
                .status(PaymentOrderStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(paymentStatusGateway.findPayment("123")).thenReturn(PaymentProviderStatus.builder()
                .paymentId("123")
                .status("in_process")
                .externalReference("order-1")
                .paymentMethod(" ")
                .build());
        when(paymentOrderGateway.findById("order-1")).thenReturn(Optional.of(paymentOrder));

        monetizationService.confirmPayment("123");

        verify(paymentOrderGateway).save(org.mockito.ArgumentMatchers.argThat(order ->
                order.getStatus() == PaymentOrderStatus.PENDING && "PIX".equals(order.getPaymentMethod())
        ));
    }

    @Test
    void confirmPaymentShouldOnlyUpdateOrderWhenPaymentIsRejected() {
        PaymentOrder paymentOrder = PaymentOrder.builder()
                .id("order-1")
                .userId("user-1")
                .productCode("CREDITS_10")
                .paymentMethod("PIX")
                .provider("MERCADO_PAGO_CHECKOUT_PRO")
                .status(PaymentOrderStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(paymentStatusGateway.findPayment("123")).thenReturn(PaymentProviderStatus.builder()
                .paymentId("123")
                .status("rejected")
                .externalReference("order-1")
                .build());
        when(paymentOrderGateway.findById("order-1")).thenReturn(Optional.of(paymentOrder));

        monetizationService.confirmPayment("123");

        verify(userGateway, never()).save(any(UserProfile.class));
        verify(paymentOrderGateway).save(org.mockito.ArgumentMatchers.argThat(order ->
                order.getStatus() == PaymentOrderStatus.REJECTED && "123".equals(order.getProviderPaymentId())
        ));
    }

    @Test
    void confirmPaymentShouldNotReleaseCreditsTwiceForApprovedOrder() {
        PaymentOrder paymentOrder = PaymentOrder.builder()
                .id("order-1")
                .userId("user-1")
                .productCode("CREDITS_10")
                .paymentMethod("PIX")
                .provider("MERCADO_PAGO_CHECKOUT_PRO")
                .status(PaymentOrderStatus.APPROVED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(paymentStatusGateway.findPayment("123")).thenReturn(PaymentProviderStatus.builder()
                .paymentId("123")
                .status("approved")
                .externalReference("order-1")
                .build());
        when(paymentOrderGateway.findById("order-1")).thenReturn(Optional.of(paymentOrder));

        monetizationService.confirmPayment("123");

        verify(userGateway, never()).save(any(UserProfile.class));
        verify(paymentOrderGateway).save(org.mockito.ArgumentMatchers.argThat(order ->
                order.getStatus() == PaymentOrderStatus.APPROVED && "123".equals(order.getProviderPaymentId())
        ));
    }

    @Test
    void approveLocalCheckoutShouldOnlyUpdateAlreadyApprovedOrder() {
        PaymentOrder paymentOrder = PaymentOrder.builder()
                .id("order-1")
                .userId("user-1")
                .productCode("CREDITS_10")
                .paymentMethod("pix")
                .provider("LOCAL_CHECKOUT_MOCK")
                .status(PaymentOrderStatus.APPROVED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(paymentOrderGateway.findById("order-1")).thenReturn(Optional.of(paymentOrder));

        monetizationService.approveLocalCheckout("order-1");

        verify(userGateway, never()).save(any(UserProfile.class));
        verify(paymentOrderGateway).save(org.mockito.ArgumentMatchers.argThat(order ->
                order.getStatus() == PaymentOrderStatus.APPROVED
                        && "order-1".equals(order.getProviderPaymentId())
                        && "PIX".equals(order.getPaymentMethod())
        ));
    }

    @Test
    void boostInterestShouldCreatePendingCheckoutWithoutExtendingBoost() {
        InterestPost interest = baseInterest();
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(interest));
        when(userGateway.findById("user-1")).thenReturn(Optional.of(baseUser()));
        when(paymentOrderGateway.save(any(PaymentOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutView result = monetizationService.boostInterest("user-1", "interest-1", BoostInterestCommand.builder()
                .boostCode("BOOST_3_DAYS")
                .paymentMethod("PIX")
                .build());

        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getProductCode()).isEqualTo("BOOST_3_DAYS");
        verify(paymentOrderGateway).save(org.mockito.ArgumentMatchers.argThat(order ->
                "BOOST_3_DAYS".equals(order.getProductCode()) && "interest-1".equals(order.getBoostInterestId())
        ));
        verify(interestGateway, never()).save(any(InterestPost.class));
        verify(emailGateway, never()).sendBoostActivatedEmail(any(UserProfile.class), anyString(), anyString());
        verify(eventPublisherGateway).publish(eq("monetization.purchase.created"), any(Map.class));
    }

    @Test
    void boostInterestShouldNotExtendExistingBoostWindowBeforePaymentApproval() {
        InterestPost interest = baseInterest().toBuilder()
                .boostedUntil(Instant.now().plus(2, ChronoUnit.DAYS))
                .build();
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(interest));
        when(userGateway.findById("user-1")).thenReturn(Optional.of(baseUser()));
        when(paymentOrderGateway.save(any(PaymentOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        monetizationService.boostInterest("user-1", "interest-1", BoostInterestCommand.builder()
                .boostCode("BOOST_3_DAYS")
                .paymentMethod("PIX")
                .build());

        verify(interestGateway, never()).save(any(InterestPost.class));
    }

    @Test
    void boostInterestShouldCreateCheckoutAndWaitForPaymentWhenProviderIsExternal() {
        ReflectionTestUtils.setField(monetizationService, "checkoutProvider", "MERCADO_PAGO_CHECKOUT_PRO");
        InterestPost interest = baseInterest();
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(interest));
        when(userGateway.findById("user-1")).thenReturn(Optional.of(baseUser()));
        when(paymentOrderGateway.save(any(PaymentOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentCheckoutGateway.createCheckout(any(UserProfile.class), any(MonetizationProductView.class), any(PaymentOrder.class)))
                .thenAnswer(invocation -> {
                    PaymentOrder order = invocation.getArgument(2);
                    return CheckoutView.builder()
                            .provider("MERCADO_PAGO_CHECKOUT_PRO")
                            .paymentMethod(order.getPaymentMethod())
                            .productCode(order.getProductCode())
                            .paymentOrderId(order.getId())
                            .providerPreferenceId("pref-1")
                            .checkoutUrl("https://mercadopago.example/checkout")
                            .status("PENDING")
                            .message("Checkout criado.")
                            .build();
                });

        CheckoutView result = monetizationService.boostInterest("user-1", "interest-1", BoostInterestCommand.builder()
                .boostCode("BOOST_3_DAYS")
                .paymentMethod("PIX")
                .build());

        assertThat(result.getCheckoutUrl()).contains("mercadopago");
        verify(interestGateway, never()).save(any(InterestPost.class));
        verify(paymentOrderGateway).save(org.mockito.ArgumentMatchers.argThat(order ->
                "interest-1".equals(order.getBoostInterestId()) && order.getStatus() == PaymentOrderStatus.CREATED
        ));
    }

    @Test
    void confirmPaymentShouldActivateBoostAfterApprovedPayment() {
        PaymentOrder paymentOrder = PaymentOrder.builder()
                .id("order-boost")
                .userId("user-1")
                .productCode("BOOST_3_DAYS")
                .boostInterestId("interest-1")
                .paymentMethod("PIX")
                .provider("MERCADO_PAGO_CHECKOUT_PRO")
                .status(PaymentOrderStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(paymentStatusGateway.findPayment("123")).thenReturn(PaymentProviderStatus.builder()
                .paymentId("123")
                .status("approved")
                .externalReference("order-boost")
                .paymentMethod("pix")
                .build());
        when(paymentOrderGateway.findById("order-boost")).thenReturn(Optional.of(paymentOrder));
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(baseInterest()));
        when(interestGateway.save(any(InterestPost.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userGateway.findById("user-1")).thenReturn(Optional.of(baseUser()));

        monetizationService.confirmPayment("123");

        verify(interestGateway).save(org.mockito.ArgumentMatchers.argThat(savedInterest ->
                savedInterest.getBoostedUntil().isAfter(Instant.now())
        ));
        verify(paymentOrderGateway).save(org.mockito.ArgumentMatchers.argThat(order ->
                order.getStatus() == PaymentOrderStatus.APPROVED && "123".equals(order.getProviderPaymentId())
        ));
    }

    @Test
    void boostInterestShouldRejectDifferentOwner() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(baseInterest()));

        assertThatThrownBy(() -> monetizationService.boostInterest("other-user", "interest-1", BoostInterestCommand.builder()
                .boostCode("BOOST_3_DAYS")
                .paymentMethod("PIX")
                .build()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("dono");
    }

    @Test
    void boostInterestShouldRejectNonBoostProduct() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(baseInterest()));

        assertThatThrownBy(() -> monetizationService.boostInterest("user-1", "interest-1", BoostInterestCommand.builder()
                .boostCode("CREDITS_10")
                .paymentMethod("PIX")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("boost");
    }

    @Test
    void boostInterestShouldRejectWhenBoostPurchasesAreDisabled() {
        when(monetizationCatalog.boostPurchasesEnabled()).thenReturn(false);
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(baseInterest()));

        assertThatThrownBy(() -> monetizationService.boostInterest("user-1", "interest-1", BoostInterestCommand.builder()
                .boostCode("BOOST_3_DAYS")
                .paymentMethod("PIX")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("boosts");
        verify(paymentOrderGateway, never()).save(any(PaymentOrder.class));
    }

    private UserProfile baseUser() {
        return UserProfile.builder()
                .id("user-1")
                .name("Ana")
                .email("ana@teste.com")
                .sellerCredits(0)
                .build();
    }

    private InterestPost baseInterest() {
        return InterestPost.builder()
                .id("interest-1")
                .ownerId("user-1")
                .ownerName("Ana")
                .title("Quero um carro")
                .category("AUTOMOVEIS")
                .status(InterestStatus.OPEN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
