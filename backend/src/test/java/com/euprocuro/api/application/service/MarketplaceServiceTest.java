package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.euprocuro.api.application.command.CreateInterestCommand;
import com.euprocuro.api.application.command.CreateOfferCommand;
import com.euprocuro.api.application.command.InterestSearchFilter;
import com.euprocuro.api.application.command.UpdateInterestCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ForbiddenException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.domain.gateway.EmailGateway;
import com.euprocuro.api.domain.gateway.EventPublisherGateway;
import com.euprocuro.api.domain.gateway.InterestGateway;
import com.euprocuro.api.domain.gateway.InterestSearchGateway;
import com.euprocuro.api.domain.gateway.OfferGateway;
import com.euprocuro.api.domain.gateway.RealtimeMessageGateway;
import com.euprocuro.api.domain.gateway.SellerItemGateway;
import com.euprocuro.api.domain.gateway.UserGateway;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestSearchCriteria;
import com.euprocuro.api.domain.model.InterestStatus;
import com.euprocuro.api.domain.model.LocationInfo;
import com.euprocuro.api.domain.model.Offer;
import com.euprocuro.api.domain.model.OfferStatus;
import com.euprocuro.api.domain.model.SellerItem;
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
    @Mock
    private EmailGateway emailGateway;
    @Mock
    private RealtimeMessageGateway realtimeMessageGateway;
    @Mock
    private OperationalCatalogService operationalCatalogService;
    @Mock
    private InterestSearchGateway interestSearchGateway;
    @Mock
    private PublicCacheService publicCacheService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private SellerItemGateway sellerItemGateway;
    @Spy
    private InterestDeliveryRankingService interestDeliveryRankingService;

    @InjectMocks
    private MarketplaceService marketplaceService;

    @BeforeEach
    void setUpCache() {
        lenient().when(publicCacheService.getOrLoad(anyString(), anyString(), anyLong(), any()))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(3)).get());
    }

    @Test
    void createInterestShouldPersistNormalizedInterest() {
        when(userGateway.findById("buyer-1")).thenReturn(Optional.of(baseBuyer()));
        when(operationalCatalogService.requireActiveCategory("SERVICOS")).thenReturn("SERVICOS");
        when(interestGateway.save(any(InterestPost.class))).thenAnswer(invocation -> {
            InterestPost interest = invocation.getArgument(0);
            interest.setId("interest-1");
            return interest;
        });

        InterestPost result = marketplaceService.createInterest("buyer-1", CreateInterestCommand.builder()
                .title("Violao")
                .description("Busco violao usado")
                .referenceImageUrl("  data:image/png;base64,abc  ")
                .category("SERVICOS")
                .budgetMin(new BigDecimal("100"))
                .budgetMax(new BigDecimal("500"))
                .city("Campinas")
                .state("SP")
                .neighborhood("Centro")
                .desiredRadiusKm(15)
                .preferredCondition("Usado")
                .preferredContactMode("Chat")
                .tags(List.of("violao", "musica"))
                .build());

        assertThat(result.getId()).isEqualTo("interest-1");
        assertThat(result.getReferenceImageUrl()).isEqualTo("data:image/png;base64,abc");
        assertThat(result.getStatus()).isEqualTo(InterestStatus.PENDING);
        assertThat(result.getLocation().getCity()).isEqualTo("Campinas");
        assertThat(result.getExpiresAt()).isAfter(result.getCreatedAt().plus(29, ChronoUnit.DAYS));
        assertThat(result.getExpiresAt()).isBefore(result.getCreatedAt().plus(31, ChronoUnit.DAYS));
        verify(eventPublisherGateway).publish(eq("interest.created"), any(Map.class));
        verify(eventPublisherGateway).publish(eq("interest.moderation.requested"), any(Map.class));
    }

    @Test
    void createInterestShouldRejectInvalidBudgetRange() {
        when(userGateway.findById("buyer-1")).thenReturn(Optional.of(baseBuyer()));

        assertThatThrownBy(() -> marketplaceService.createInterest("buyer-1", CreateInterestCommand.builder()
                .title("Violao")
                .category("SERVICOS")
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
        when(operationalCatalogService.requireActiveCategory("SERVICOS")).thenReturn("SERVICOS");
        when(interestGateway.save(any(InterestPost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterestPost result = marketplaceService.updateInterest("buyer-1", "interest-1", UpdateInterestCommand.builder()
                .title("Quero um violao eletrico")
                .description("Procuro modelo conservado")
                .referenceImageUrl(" imagem ")
                .category("SERVICOS")
                .budgetMin(new BigDecimal("200"))
                .budgetMax(new BigDecimal("700"))
                .city("Campinas")
                .state("SP")
                .neighborhood("Taquaral")
                .desiredRadiusKm(40)
                .preferredCondition("Usado")
                .preferredContactMode("Chat")
                .tags(List.of("eletrico"))
                .build());

        assertThat(result.getTitle()).isEqualTo("Quero um violao eletrico");
        assertThat(result.getReferenceImageUrl()).isEqualTo("imagem");
        assertThat(result.getStatus()).isEqualTo(InterestStatus.PENDING);
        verify(eventPublisherGateway).publish(eq("interest.updated"), any(Map.class));
        verify(eventPublisherGateway).publish(eq("interest.moderation.requested"), any(Map.class));
    }

    @Test
    void renewInterestShouldConsumeOneCreditAndExtendExpiration() {
        InterestPost existingInterest = baseInterest();
        existingInterest.setExpiresAt(Instant.now().plus(5, ChronoUnit.DAYS));
        ReflectionTestUtils.setField(marketplaceService, "listingExpirationDays", 5L);
        ReflectionTestUtils.setField(marketplaceService, "listingRenewalDays", 30L);
        UserProfile owner = baseBuyer().toBuilder()
                .sellerCredits(2)
                .build();
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(existingInterest));
        when(userGateway.findById("buyer-1")).thenReturn(Optional.of(owner));
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(interestGateway.save(any(InterestPost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterestPost result = marketplaceService.renewInterest("buyer-1", "interest-1");

        assertThat(result.getExpiresAt()).isAfter(existingInterest.getExpiresAt().plus(29, ChronoUnit.DAYS));
        verify(userGateway).save(any(UserProfile.class));
        verify(eventPublisherGateway).publish(eq("interest.renewed"), any(Map.class));
    }

    @Test
    void renewInterestShouldRejectOwnerWithoutCredits() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(baseInterest()));
        when(userGateway.findById("buyer-1")).thenReturn(Optional.of(baseBuyer().toBuilder()
                .sellerCredits(0)
                .build()));

        assertThatThrownBy(() -> marketplaceService.renewInterest("buyer-1", "interest-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("credito");
    }

    @Test
    void updateInterestShouldRejectDifferentOwner() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(baseInterest()));

        assertThatThrownBy(() -> marketplaceService.updateInterest("other-user", "interest-1", UpdateInterestCommand.builder()
                .title("Novo")
                .category("SERVICOS")
                .build()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("dono");
    }

    @Test
    void listInterestsShouldFilterAndSortBoostedFirst() {
        InterestPost boosted = baseInterest();
        boosted.setId("1");
        boosted.setBoostedUntil(Instant.now().plus(1, ChronoUnit.DAYS));
        boosted.setCreatedAt(Instant.now().minus(2, ChronoUnit.HOURS));

        InterestPost newest = baseInterest();
        newest.setId("2");
        newest.setTitle("Aula de violao");
        newest.setTags(List.of("aula"));
        newest.setCreatedAt(Instant.now());

        InterestPost closed = baseInterest();
        closed.setId("3");
        closed.setStatus(InterestStatus.CLOSED);

        InterestPost expired = baseInterest();
        expired.setId("4");
        expired.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));

        when(interestGateway.findAll()).thenReturn(List.of(newest, closed, boosted, expired));

        List<InterestPost> results = marketplaceService.listInterests(InterestSearchFilter.builder()
                .category("SERVICOS")
                .city("Campinas")
                .query("violao")
                .maxBudget(new BigDecimal("500"))
                .openOnly(true)
                .build());

        assertThat(results).extracting(InterestPost::getId).containsExactly("1", "2");
    }

    @Test
    void getInterestShouldDeleteAndRejectExpiredInterest() {
        InterestPost expired = baseInterest();
        expired.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> marketplaceService.getInterest("interest-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("expirado");
        verify(interestGateway).deleteById("interest-1");
    }

    @Test
    void getInterestShouldReturnPubliclyVisibleInterest() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(baseInterest()));

        InterestPost result = marketplaceService.getInterest("interest-1");

        assertThat(result.getId()).isEqualTo("interest-1");
        assertThat(result.getStatus()).isEqualTo(InterestStatus.OPEN);
    }

    @Test
    void getInterestShouldRejectNonPublicInterest() {
        InterestPost pending = baseInterest();
        pending.setStatus(InterestStatus.PENDING);
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> marketplaceService.getInterest("interest-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Interesse nao encontrado");
    }

    @Test
    void listInterestsShouldMatchQueryByDescriptionOwnerNameAndCity() {
        InterestPost byDescription = baseInterest();
        byDescription.setId("description");
        byDescription.setTitle("Titulo sem termo");
        byDescription.setDescription("Procuro teclado mecanico");
        byDescription.setOwnerName("Ana");
        byDescription.setLocation(LocationInfo.builder().city("Campinas").state("SP").build());

        InterestPost byOwner = baseInterest();
        byOwner.setId("owner");
        byOwner.setTitle("Titulo generico");
        byOwner.setDescription("Descricao generica");
        byOwner.setOwnerName("Carlos Mecanico");
        byOwner.setLocation(LocationInfo.builder().city("Valinhos").state("SP").build());

        InterestPost byCity = baseInterest();
        byCity.setId("city");
        byCity.setTitle("Outro titulo");
        byCity.setDescription("Outra descricao");
        byCity.setOwnerName("Maria");
        byCity.setLocation(LocationInfo.builder().city("Mecanico").state("SP").build());

        InterestPost noMatch = baseInterest();
        noMatch.setId("no-match");
        noMatch.setTitle("Violao");
        noMatch.setDescription("Instrumento musical");
        noMatch.setOwnerName("Joao");
        noMatch.setLocation(null);
        noMatch.setTags(null);

        when(interestGateway.findAll()).thenReturn(List.of(byDescription, byOwner, byCity, noMatch));

        List<InterestPost> results = marketplaceService.listInterests(InterestSearchFilter.builder()
                .query("mecanico")
                .openOnly(true)
                .build());

        assertThat(results).extracting(InterestPost::getId)
                .containsExactlyInAnyOrder("city", "owner", "description");
    }

    @Test
    void listInterestsShouldHideCurrentUserOwnInterestsBeforeRanking() {
        InterestPost ownInterest = baseInterest().toBuilder()
                .id("own")
                .ownerId("seller-1")
                .createdAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        InterestPost otherInterest = baseInterest().toBuilder()
                .id("other")
                .ownerId("buyer-2")
                .createdAt(Instant.now())
                .build();

        when(interestGateway.findAll()).thenReturn(List.of(ownInterest, otherInterest));
        when(userGateway.findById("seller-1")).thenReturn(Optional.of(UserProfile.builder().id("seller-1").build()));
        when(sellerItemGateway.findByOwnerIdOrderByCreatedAtDesc("seller-1")).thenReturn(List.of());

        List<InterestPost> results = marketplaceService.listInterests(InterestSearchFilter.builder()
                .openOnly(true)
                .currentUserId("seller-1")
                .build());

        assertThat(results).extracting(InterestPost::getId).containsExactly("other");
    }

    @Test
    void createInterestShouldNormalizePostalCodeWithDigitsOnly() {
        when(userGateway.findById("buyer-1")).thenReturn(Optional.of(baseBuyer()));
        when(operationalCatalogService.requireActiveCategory("SERVICOS")).thenReturn("SERVICOS");
        when(interestGateway.save(any(InterestPost.class))).thenAnswer(invocation -> {
            InterestPost interest = invocation.getArgument(0);
            interest.setId("interest-cep");
            return interest;
        });

        InterestPost result = marketplaceService.createInterest("buyer-1", CreateInterestCommand.builder()
                .title("Violao")
                .description("Busco violao usado")
                .category("SERVICOS")
                .budgetMax(new BigDecimal("500"))
                .postalCode("13010-111")
                .city("Campinas")
                .state("SP")
                .build());

        assertThat(result.getLocation().getPostalCode()).isEqualTo("13010-111");
    }

    @Test
    void createInterestShouldRejectInvalidPostalCode() {
        when(userGateway.findById("buyer-1")).thenReturn(Optional.of(baseBuyer()));
        when(operationalCatalogService.requireActiveCategory("SERVICOS")).thenReturn("SERVICOS");

        assertThatThrownBy(() -> marketplaceService.createInterest("buyer-1", CreateInterestCommand.builder()
                .title("Violao")
                .description("Busco violao usado")
                .category("SERVICOS")
                .postalCode("123")
                .city("Campinas")
                .state("SP")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CEP valido");
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
    void createOfferShouldRejectSellerWithoutCreditsOrPlan() {
        UserProfile seller = UserProfile.builder()
                .id("seller-1")
                .name("Carlos")
                .email("carlos@teste.com")
                .sellerCredits(0)
                .build();

        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(baseInterest()));
        when(userGateway.findById("seller-1")).thenReturn(Optional.of(seller));

        assertThatThrownBy(() -> marketplaceService.createOffer("seller-1", "interest-1", CreateOfferCommand.builder()
                .message("Tenho um item")
                .offeredPrice(new BigDecimal("400"))
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("creditos");
    }

    @Test
    void createOfferShouldAllowActivePlanWithoutCredits() {
        InterestPost interest = baseInterest();
        UserProfile seller = UserProfile.builder()
                .id("seller-1")
                .name("Carlos")
                .email("carlos@teste.com")
                .sellerCredits(0)
                .subscriptionActiveUntil(Instant.now().plus(3, ChronoUnit.DAYS))
                .build();

        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(interest));
        when(userGateway.findById("seller-1")).thenReturn(Optional.of(seller));
        when(userGateway.findById("buyer-1")).thenReturn(Optional.of(baseBuyer()));
        when(offerGateway.save(any(Offer.class))).thenAnswer(invocation -> {
            Offer offer = invocation.getArgument(0);
            offer.setId("offer-1");
            return offer;
        });

        Offer result = marketplaceService.createOffer("seller-1", "interest-1", CreateOfferCommand.builder()
                .message("Tenho um item")
                .offeredPrice(new BigDecimal("400"))
                .build());

        assertThat(result.getSellerId()).isEqualTo("seller-1");
    }

    @Test
    void createOfferShouldPersistOfferAndPublishEvent() {
        InterestPost interest = baseInterest();
        UserProfile seller = UserProfile.builder()
                .id("seller-1")
                .name("Carlos")
                .email("carlos@teste.com")
                .sellerCredits(5)
                .build();

        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(interest));
        when(userGateway.findById("seller-1")).thenReturn(Optional.of(seller));
        when(userGateway.findById("buyer-1")).thenReturn(Optional.of(baseBuyer()));
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
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
    void createOfferShouldAllowMissingSellerPhone() {
        InterestPost interest = baseInterest();
        UserProfile seller = UserProfile.builder()
                .id("seller-1")
                .name("Carlos")
                .email("carlos@teste.com")
                .sellerCredits(5)
                .build();

        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(interest));
        when(userGateway.findById("seller-1")).thenReturn(Optional.of(seller));
        when(userGateway.findById("buyer-1")).thenReturn(Optional.of(baseBuyer()));
        when(userGateway.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(offerGateway.save(any(Offer.class))).thenAnswer(invocation -> {
            Offer offer = invocation.getArgument(0);
            offer.setId("offer-1");
            return offer;
        });

        Offer result = marketplaceService.createOffer("seller-1", "interest-1", CreateOfferCommand.builder()
                .offeredPrice(new BigDecimal("450"))
                .sellerPhone("   ")
                .message("Tenho um violao nessa faixa")
                .build());

        assertThat(result.getSellerPhone()).isNull();
        verify(offerGateway).save(argThat(offer -> offer.getSellerPhone() == null));
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

    @Test
    void closeInterestShouldMarkOwnedInterestAsClosed() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(baseInterest()));
        when(interestGateway.save(any(InterestPost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterestPost result = marketplaceService.closeInterest("buyer-1", "interest-1");

        assertThat(result.getStatus()).isEqualTo(InterestStatus.CLOSED);
        verify(eventPublisherGateway).publish(eq("interest.closed"), any(Map.class));
    }

    @Test
    void closeInterestShouldRejectDifferentOwner() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(baseInterest()));

        assertThatThrownBy(() -> marketplaceService.closeInterest("other-user", "interest-1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("dono do interesse");
    }

    @Test
    void activateInterestShouldSendClosedInterestBackToModeration() {
        InterestPost closed = baseInterest();
        closed.setStatus(InterestStatus.CLOSED);
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(closed));
        when(interestGateway.save(any(InterestPost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterestPost result = marketplaceService.activateInterest("buyer-1", "interest-1");

        assertThat(result.getStatus()).isEqualTo(InterestStatus.PENDING);
        assertThat(result.getModeration()).isNull();
        verify(eventPublisherGateway).publish(eq("interest.activated"), any(Map.class));
        verify(eventPublisherGateway).publish(eq("interest.moderation.requested"), any(Map.class));
    }

    @Test
    void activateInterestShouldRejectDifferentOwner() {
        InterestPost closed = baseInterest();
        closed.setStatus(InterestStatus.CLOSED);
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> marketplaceService.activateInterest("other-user", "interest-1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("dono do interesse");
    }

    @Test
    void deleteInterestShouldRemoveOwnedInterest() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(baseInterest()));

        marketplaceService.deleteInterest("buyer-1", "interest-1");

        verify(interestGateway).deleteById("interest-1");
        verify(eventPublisherGateway).publish(eq("interest.deleted"), any(Map.class));
    }

    @Test
    void deleteInterestShouldRejectDifferentOwner() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(baseInterest()));

        assertThatThrownBy(() -> marketplaceService.deleteInterest("other-user", "interest-1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("dono do interesse");
    }

    @Test
    void renewInterestShouldRejectDifferentOwner() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(baseInterest()));

        assertThatThrownBy(() -> marketplaceService.renewInterest("other-user", "interest-1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("dono do interesse");
    }

    @Test
    void renewInterestShouldRejectMissingOwner() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(baseInterest()));
        when(userGateway.findById("buyer-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> marketplaceService.renewInterest("buyer-1", "interest-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuario nao encontrado");
    }

    @Test
    void listInterestsWithPaginationShouldUseSearchGatewayAndRemoveExpiredResults() {
        InterestPost active = baseInterest();
        active.setId("active");
        InterestPost expired = baseInterest();
        expired.setId("expired");
        expired.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));

        when(interestSearchGateway.search(any(InterestSearchCriteria.class), eq(0), eq(50)))
                .thenReturn(List.of(active, expired));

        List<InterestPost> result = marketplaceService.listInterests(InterestSearchFilter.builder()
                .category("SERVICOS")
                .city("Campinas")
                .query("violao")
                .maxBudget(new BigDecimal("500"))
                .openOnly(true)
                .build(), -5, 999);

        assertThat(result).extracting(InterestPost::getId).containsExactly("active");
    }

    @Test
    void listInterestsWithCurrentUserShouldPrioritizeLocationAndSellerItemMatches() {
        UserProfile currentUser = UserProfile.builder()
                .id("seller-1")
                .city("Erechim")
                .state("RS")
                .country("Brasil")
                .build();
        SellerItem sellerItem = SellerItem.builder()
                .id("item-1")
                .ownerId("seller-1")
                .title("Celta 2012")
                .description("Carro conservado")
                .category("AUTOMOVEIS")
                .location(LocationInfo.builder().city("Erechim").state("RS").country("Brasil").build())
                .tags(List.of("celta", "chevrolet"))
                .active(true)
                .build();
        InterestPost genericRecent = baseInterest().toBuilder()
                .id("generic")
                .title("Procuro apartamento")
                .description("Preciso alugar imovel")
                .category("IMOVEIS")
                .location(LocationInfo.builder().city("Porto Alegre").state("RS").country("Brasil").build())
                .tags(List.of("apartamento"))
                .createdAt(Instant.now())
                .build();
        InterestPost matchingOlder = baseInterest().toBuilder()
                .id("matching")
                .title("Procuro Celta 2012")
                .description("Busco Chevrolet Celta em Erechim")
                .category("AUTOMOVEIS")
                .location(LocationInfo.builder().city("Erechim").state("RS").country("Brasil").build())
                .tags(List.of("celta"))
                .createdAt(Instant.now().minus(20, ChronoUnit.DAYS))
                .build();

        when(userGateway.findById("seller-1")).thenReturn(Optional.of(currentUser));
        when(sellerItemGateway.findByOwnerIdOrderByCreatedAtDesc("seller-1")).thenReturn(List.of(sellerItem));
        when(interestSearchGateway.search(any(InterestSearchCriteria.class), eq(0), eq(100)))
                .thenReturn(List.of(genericRecent, matchingOlder));

        List<InterestPost> result = marketplaceService.listInterests(InterestSearchFilter.builder()
                .openOnly(true)
                .currentUserId("seller-1")
                .build(), 0, 10);

        assertThat(result).extracting(InterestPost::getId).containsExactly("matching", "generic");
    }

    @Test
    void listInterestsWithCurrentUserShouldHideOwnInterestsBeforePagination() {
        UserProfile currentUser = UserProfile.builder()
                .id("seller-1")
                .city("Erechim")
                .state("RS")
                .country("Brasil")
                .build();
        InterestPost ownInterest = baseInterest().toBuilder()
                .id("own")
                .ownerId("seller-1")
                .createdAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        InterestPost firstVisible = baseInterest().toBuilder()
                .id("first-visible")
                .ownerId("buyer-2")
                .createdAt(Instant.now())
                .build();
        InterestPost secondVisible = baseInterest().toBuilder()
                .id("second-visible")
                .ownerId("buyer-3")
                .createdAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        when(userGateway.findById("seller-1")).thenReturn(Optional.of(currentUser));
        when(sellerItemGateway.findByOwnerIdOrderByCreatedAtDesc("seller-1")).thenReturn(List.of());
        when(interestSearchGateway.search(any(InterestSearchCriteria.class), eq(0), eq(100)))
                .thenReturn(List.of(ownInterest, firstVisible, secondVisible));

        List<InterestPost> result = marketplaceService.listInterests(InterestSearchFilter.builder()
                .openOnly(true)
                .currentUserId("seller-1")
                .build(), 0, 2);

        assertThat(result).extracting(InterestPost::getId)
                .containsExactly("first-visible", "second-visible");
    }

    @Test
    void createOfferShouldRejectMissingSeller() {
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(baseInterest()));
        when(userGateway.findById("seller-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> marketplaceService.createOffer("seller-1", "interest-1", CreateOfferCommand.builder()
                .message("Tenho um item")
                .offeredPrice(new BigDecimal("400"))
                .build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Vendedor nao encontrado");
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
                .category("SERVICOS")
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
                .preferredCondition("Usado")
                .preferredContactMode("Chat")
                .status(InterestStatus.OPEN)
                .createdAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .updatedAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();
    }
}
