package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.euprocuro.api.application.command.BoostInterestCommand;
import com.euprocuro.api.application.command.PurchaseProductCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ForbiddenException;
import com.euprocuro.api.application.view.CheckoutView;
import com.euprocuro.api.application.view.MonetizationAccountView;
import com.euprocuro.api.domain.gateway.EmailGateway;
import com.euprocuro.api.domain.gateway.EventPublisherGateway;
import com.euprocuro.api.domain.gateway.InterestGateway;
import com.euprocuro.api.domain.gateway.UserGateway;
import com.euprocuro.api.domain.model.InterestCategory;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestStatus;
import com.euprocuro.api.domain.model.UserProfile;

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
