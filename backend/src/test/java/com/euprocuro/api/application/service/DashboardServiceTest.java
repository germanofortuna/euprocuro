package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.application.view.PersonalDashboardView;
import com.euprocuro.api.domain.gateway.InterestGateway;
import com.euprocuro.api.domain.gateway.OfferGateway;
import com.euprocuro.api.domain.gateway.UserGateway;
import com.euprocuro.api.domain.model.InterestCategory;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestStatus;
import com.euprocuro.api.domain.model.LocationInfo;
import com.euprocuro.api.domain.model.Offer;
import com.euprocuro.api.domain.model.OfferStatus;
import com.euprocuro.api.domain.model.UserProfile;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private UserGateway userGateway;
    @Mock
    private InterestGateway interestGateway;
    @Mock
    private OfferGateway offerGateway;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getDashboardShouldAggregateInterestAndOfferData() {
        UserProfile user = UserProfile.builder().id("buyer-1").name("Ana").email("ana@teste.com").build();
        InterestPost myInterest = InterestPost.builder()
                .id("interest-1")
                .ownerId("buyer-1")
                .ownerName("Ana")
                .title("Quero um violao")
                .category(InterestCategory.SERVICOS)
                .location(LocationInfo.builder().city("Campinas").state("SP").build())
                .status(InterestStatus.OPEN)
                .createdAt(Instant.now())
                .build();
        Offer receivedOffer = Offer.builder()
                .id("offer-r1")
                .interestPostId("interest-1")
                .sellerId("seller-1")
                .sellerName("Carlos")
                .sellerEmail("carlos@teste.com")
                .offeredPrice(new BigDecimal("450"))
                .message("Tenho um violao")
                .includesDelivery(true)
                .highlights(List.of("Conservado"))
                .status(OfferStatus.SENT)
                .createdAt(Instant.now())
                .build();
        Offer sentOffer = Offer.builder()
                .id("offer-s1")
                .interestPostId("removed-interest")
                .sellerId("buyer-1")
                .sellerName("Ana")
                .sellerEmail("ana@teste.com")
                .offeredPrice(new BigDecimal("300"))
                .message("Tenho interesse")
                .status(OfferStatus.SENT)
                .createdAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        when(userGateway.findById("buyer-1")).thenReturn(Optional.of(user));
        when(interestGateway.findByOwnerIdOrderByCreatedAtDesc("buyer-1")).thenReturn(List.of(myInterest));
        when(offerGateway.findByInterestPostIdInOrderByCreatedAtDesc(List.of("interest-1"))).thenReturn(List.of(receivedOffer));
        when(offerGateway.findBySellerIdOrderByCreatedAtDesc("buyer-1")).thenReturn(List.of(sentOffer));
        when(interestGateway.findAll()).thenReturn(List.of(myInterest));

        PersonalDashboardView result = dashboardService.getDashboard("buyer-1");

        assertThat(result.getTotalActiveInterests()).isEqualTo(1);
        assertThat(result.getTotalOffersReceived()).isEqualTo(1);
        assertThat(result.getTotalOffersSent()).isEqualTo(1);
        assertThat(result.getOffersReceived()).hasSize(1);
        assertThat(result.getOffersSent()).extracting("interestTitle").containsExactly("Interesse removido");
    }

    @Test
    void getDashboardShouldRejectUnknownUser() {
        when(userGateway.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getDashboard("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuario");
    }
}
