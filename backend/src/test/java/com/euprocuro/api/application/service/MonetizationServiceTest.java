package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
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
import com.euprocuro.api.domain.model.InterestCategory;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestStatus;
import com.euprocuro.api.domain.model.PaymentOrder;
import com.euprocuro.api.domain.model.PaymentOrderStatus;
import com.euprocuro.api.domain.model.PaymentProviderStatus;
import com.euprocuro.api.domain.model.UserProfile;
import com.euprocuro.api.shared.config.MonetizationCatalogProperties;

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
    @Spy
    private MonetizationCatalog monetizationCatalog = new MonetizationCatalog(new MonetizationCatalogProperties());
    @Mock
    private PaymentOrderGateway paymentOrderGateway;
    @Mock
    private PaymentCheckoutGateway paymentCheckoutGateway;
    @Mock
    private PaymentStatusGateway paymentStatusGateway;

    @InjectMocks
    private MonetizationService monetizationService;

    @Test
    void getAccountShouldReturnCreditsPlanAndProducts() {
        UserProfile user = baseUser().toBuilder()
                .sellerCredits(4)
                .purchasedCreditsTotal(10)
                .subscriptionPlan("SELLER_PRO")
                .subscriptionActiveUntil(Instant.now().plusSeconds(3600))
                .build();
        when(userGateway.findById("user-1")).thenReturn(Optional.of(user));

        MonetizationAccountView result = monetizationService.getAccount("user-1");

        assertThat(result.getSellerCredits()).isEqualTo(4);
        assertThat(result.getPurchasedCreditsTotal()).isEqualTo(10);
        assertThat(result.isSubscriptionActive()).isTrue();
        assertThat(result.getProducts()).isNotEmpty();
    }

    @Test
    void purchaseShouldAddCreditsAndSendEmail() {
        when(userGateway.findById("user-1")).thenReturn(Optional.of(baseUser()));
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutView result = monetizationService.purchase("user-1", PurchaseProductCommand.builder()
                .productCode("CREDITS_10")
                .paymentMethod("pix")
                .build());

        assertThat(result.getProvider()).isEqualTo("LOCAL_MOCK");
        assertThat(result.getProductCode()).isEqualTo("CREDITS_10");
        assertThat(result.getStatus()).isEqualTo("APPROVED");
        verify(userGateway).save(org.mockito.ArgumentMatchers.argThat(user ->
                user.getSellerCredits() == 10 && user.getPurchasedCreditsTotal() == 10
        ));
        verify(emailGateway).sendPurchaseConfirmationEmail(any(UserProfile.class), eq("10 propostas"), eq("PIX"));
        verify(eventPublisherGateway).publish(eq("monetization.purchase.completed"), any(Map.class));
    }

    @Test
    void purchaseShouldActivateSubscription() {
        when(userGateway.findById("user-1")).thenReturn(Optional.of(baseUser()));
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        monetizationService.purchase("user-1", PurchaseProductCommand.builder()
                .productCode("SELLER_PRO")
                .paymentMethod("CREDIT_CARD")
                .build());

        verify(userGateway).save(any(UserProfile.class));
        verify(emailGateway).sendPurchaseConfirmationEmail(any(UserProfile.class), eq("Plano vendedor Pro"), eq("CREDIT_CARD"));
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
                order.getStatus() == PaymentOrderStatus.APPROVED && "123".equals(order.getProviderPaymentId())
        ));
        verify(eventPublisherGateway).publish(eq("monetization.purchase.completed"), any(Map.class));
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
    void boostInterestShouldExtendInterestBoost() {
        InterestPost interest = baseInterest();
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(interest));
        when(interestGateway.save(any(InterestPost.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userGateway.findById("user-1")).thenReturn(Optional.of(baseUser()));

        InterestPost result = monetizationService.boostInterest("user-1", "interest-1", BoostInterestCommand.builder()
                .boostCode("BOOST_3_DAYS")
                .paymentMethod("PIX")
                .build());

        assertThat(result.isBoostEnabled()).isTrue();
        assertThat(result.getBoostedUntil()).isAfter(Instant.now());
        verify(emailGateway).sendBoostActivatedEmail(any(UserProfile.class), eq("Quero um carro"), any(String.class));
        verify(eventPublisherGateway).publish(eq("interest.boosted"), any(Map.class));
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
                .category(InterestCategory.AUTOMOVEIS)
                .status(InterestStatus.OPEN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
