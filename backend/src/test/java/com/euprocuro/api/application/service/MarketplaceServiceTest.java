package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.euprocuro.api.application.command.CreateInterestCommand;
import com.euprocuro.api.application.command.CreateOfferCommand;
import com.euprocuro.api.application.command.InterestSearchFilter;
import com.euprocuro.api.application.command.UpdateInterestCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ForbiddenException;
import com.euprocuro.api.domain.gateway.EventPublisherGateway;
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
class MarketplaceServiceTest {

    @Mock
    private UserGateway userGateway;
    @Mock
    private InterestGateway interestGateway;
    @Mock
    private OfferGateway offerGateway;
    @Mock
    private EventPublisherGateway eventPublisherGateway;

    @InjectMocks
    private MarketplaceService marketplaceService;

    @Test
    void createInterestShouldPersistNormalizedInterest() {
        when(userGateway.findById("buyer-1")).thenReturn(Optional.of(baseBuyer()));
        when(interestGateway.save(any(InterestPost.class))).thenAnswer(invocation -> {
            InterestPost interest = invocation.getArgument(0);
            interest.setId("interest-1");
            return interest;
        });

        InterestPost result = marketplaceService.createInterest("buyer-1", CreateInterestCommand.builder()
                .title("Violao")
                .description("Busco violao usado")
                .referenceImageUrl("  data:image/png;base64,abc  ")
                .category(InterestCategory.SERVICOS)
                .budgetMin(new BigDecimal("100"))
                .budgetMax(new BigDecimal("500"))
                .city("Campinas")
                .state("SP")
                .neighborhood("Centro")
                .desiredRadiusKm(15)
                .acceptsNationwideOffers(true)
                .boostEnabled(true)
                .preferredCondition("Usado")
                .preferredContactMode("Chat")
                .tags(List.of("violao", "musica"))
                .build());

        assertThat(result.getId()).isEqualTo("interest-1");
        assertThat(result.getReferenceImageUrl()).isEqualTo("data:image/png;base64,abc");
        assertThat(result.getStatus()).isEqualTo(InterestStatus.OPEN);
        assertThat(result.getLocation().getCity()).isEqualTo("Campinas");
        verify(eventPublisherGateway).publish(eq("interest.created"), any(Map.class));
    }

    @Test
    void createInterestShouldRejectInvalidBudgetRange() {
        when(userGateway.findById("buyer-1")).thenReturn(Optional.of(baseBuyer()));

        assertThatThrownBy(() -> marketplaceService.createInterest("buyer-1", CreateInterestCommand.builder()
                .title("Violao")
                .category(InterestCategory.SERVICOS)
                .budgetMin(new BigDecimal("600"))
                .budgetMax(new BigDecimal("500"))
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("minimo");
    }

    @Test
    void updateInterestShouldPersistEditedInterestForOwner() {
        InterestPost existingInterest = baseInterest();
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(existingInterest));
        when(interestGateway.save(any(InterestPost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterestPost result = marketplaceService.updateInterest("buyer-1", "interest-1", UpdateInterestCommand.builder()
                .title("Quero um violao eletrico")
                .description("Procuro modelo conservado")
                .referenceImageUrl(" imagem ")
                .category(InterestCategory.SERVICOS)
                .budgetMin(new BigDecimal("200"))
                .budgetMax(new BigDecimal("700"))
                .city("Campinas")
                .state("SP")
                .neighborhood("Taquaral")
                .desiredRadiusKm(40)
                .acceptsNationwideOffers(true)
                .boostEnabled(true)
                .preferredCondition("Usado")
                .preferredContactMode("Chat")
                .tags(List.of("eletrico"))
                .build());

        assertThat(result.getTitle()).isEqualTo("Quero um violao eletrico");
        assertThat(result.getReferenceImageUrl()).isEqualTo("imagem");
        assertThat(result.isBoostEnabled()).isTrue();
        verify(eventPublisherGateway).publish(eq("interest.updated"), any(Map.class));
    }

    @Test
    void updateInterestShouldRejectDifferentOwner() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(baseInterest()));

        assertThatThrownBy(() -> marketplaceService.updateInterest("other-user", "interest-1", UpdateInterestCommand.builder()
                .title("Novo")
                .category(InterestCategory.SERVICOS)
                .build()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("dono");
    }

    @Test
    void listInterestsShouldFilterAndSortBoostedFirst() {
        InterestPost boosted = baseInterest();
        boosted.setId("1");
        boosted.setBoostEnabled(true);
        boosted.setCreatedAt(Instant.now().minus(2, ChronoUnit.HOURS));

        InterestPost newest = baseInterest();
        newest.setId("2");
        newest.setTitle("Aula de violao");
        newest.setTags(List.of("aula"));
        newest.setBoostEnabled(false);
        newest.setCreatedAt(Instant.now());

        InterestPost closed = baseInterest();
        closed.setId("3");
        closed.setStatus(InterestStatus.CLOSED);

        when(interestGateway.findAll()).thenReturn(List.of(newest, closed, boosted));

        List<InterestPost> results = marketplaceService.listInterests(InterestSearchFilter.builder()
                .category(InterestCategory.SERVICOS)
                .city("Campinas")
                .query("violao")
                .maxBudget(new BigDecimal("500"))
                .openOnly(true)
                .build());

        assertThat(results).extracting(InterestPost::getId).containsExactly("1", "2");
    }

    @Test
    void createOfferShouldRejectClosedInterest() {
        InterestPost interest = baseInterest();
        interest.setStatus(InterestStatus.CLOSED);
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(interest));

        assertThatThrownBy(() -> marketplaceService.createOffer("seller-1", "interest-1", CreateOfferCommand.builder()
                .message("Tenho algo parecido")
                .offeredPrice(new BigDecimal("400"))
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("aberto");
    }

    @Test
    void createOfferShouldRejectOfferFromInterestOwner() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(baseInterest()));
        when(userGateway.findById("buyer-1")).thenReturn(Optional.of(baseBuyer()));

        assertThatThrownBy(() -> marketplaceService.createOffer("buyer-1", "interest-1", CreateOfferCommand.builder()
                .message("Eu mesmo vendo")
                .offeredPrice(new BigDecimal("400"))
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nao pode ofertar para si");
    }

    @Test
    void createOfferShouldPersistOfferAndPublishEvent() {
        InterestPost interest = baseInterest();
        UserProfile seller = UserProfile.builder()
                .id("seller-1")
                .name("Carlos")
                .email("carlos@teste.com")
                .build();

        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(interest));
        when(userGateway.findById("seller-1")).thenReturn(Optional.of(seller));
        when(offerGateway.save(any(Offer.class))).thenAnswer(invocation -> {
            Offer offer = invocation.getArgument(0);
            offer.setId("offer-1");
            return offer;
        });

        Offer result = marketplaceService.createOffer("seller-1", "interest-1", CreateOfferCommand.builder()
                .offeredPrice(new BigDecimal("450"))
                .sellerPhone("11999999999")
                .message("Tenho um violao nessa faixa")
                .includesDelivery(true)
                .highlights(List.of("Conservado"))
                .build());

        assertThat(result.getId()).isEqualTo("offer-1");
        assertThat(result.getStatus()).isEqualTo(OfferStatus.SENT);
        verify(eventPublisherGateway).publish(eq("offer.created"), any(Map.class));
    }

    @Test
    void listOffersByInterestShouldReturnOffersForOwner() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(baseInterest()));
        when(offerGateway.findByInterestPostIdOrderByCreatedAtDesc("interest-1")).thenReturn(List.of(
                Offer.builder().id("offer-1").build(),
                Offer.builder().id("offer-2").build()
        ));

        List<Offer> offers = marketplaceService.listOffersByInterest("buyer-1", "interest-1");

        assertThat(offers).hasSize(2);
    }

    @Test
    void listOffersByInterestShouldRejectNonOwner() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(baseInterest()));

        assertThatThrownBy(() -> marketplaceService.listOffersByInterest("seller-1", "interest-1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("dono");
    }

    private UserProfile baseBuyer() {
        return UserProfile.builder()
                .id("buyer-1")
                .name("Ana")
                .email("ana@teste.com")
                .build();
    }

    private InterestPost baseInterest() {
        return InterestPost.builder()
                .id("interest-1")
                .ownerId("buyer-1")
                .ownerName("Ana")
                .title("Quero um violao")
                .description("Procuro violao usado")
                .category(InterestCategory.SERVICOS)
                .budgetMin(new BigDecimal("100"))
                .budgetMax(new BigDecimal("500"))
                .location(LocationInfo.builder()
                        .city("Campinas")
                        .state("SP")
                        .neighborhood("Centro")
                        .remote(false)
                        .build())
                .tags(List.of("violao"))
                .desiredRadiusKm(20)
                .acceptsNationwideOffers(true)
                .boostEnabled(false)
                .preferredCondition("Usado")
                .preferredContactMode("Chat")
                .status(InterestStatus.OPEN)
                .createdAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();
    }
}
