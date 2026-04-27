package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.euprocuro.api.application.command.CreateOfferCommand;
import com.euprocuro.api.application.command.CreateSellerItemCommand;
import com.euprocuro.api.application.command.InterestSearchFilter;
import com.euprocuro.api.application.command.ShareSellerItemCommand;
import com.euprocuro.api.application.command.UpdateSellerItemCommand;
import com.euprocuro.api.application.exception.ForbiddenException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.application.usecase.MarketplaceUseCase;
import com.euprocuro.api.application.view.SellerItemMatchesView;
import com.euprocuro.api.domain.gateway.SellerItemGateway;
import com.euprocuro.api.domain.gateway.UserGateway;
import com.euprocuro.api.domain.model.InterestCategory;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestStatus;
import com.euprocuro.api.domain.model.LocationInfo;
import com.euprocuro.api.domain.model.Offer;
import com.euprocuro.api.domain.model.SellerItem;
import com.euprocuro.api.domain.model.UserProfile;

@ExtendWith(MockitoExtension.class)
class SellerItemServiceTest {

    @Mock
    private SellerItemGateway sellerItemGateway;
    @Mock
    private UserGateway userGateway;
    @Mock
    private MarketplaceUseCase marketplaceUseCase;

    @InjectMocks
    private SellerItemService sellerItemService;

    @Test
    void createItemShouldPersistActiveItemForCurrentUser() {
        when(userGateway.findById("seller-1")).thenReturn(Optional.of(baseUser()));
        when(sellerItemGateway.save(any(SellerItem.class))).thenAnswer(invocation -> {
            SellerItem item = invocation.getArgument(0);
            item.setId("item-1");
            return item;
        });

        SellerItem result = sellerItemService.createItem("seller-1", CreateSellerItemCommand.builder()
                .title("Celta 2012")
                .description("Carro conservado")
                .referenceImageUrl("  foto  ")
                .category(InterestCategory.AUTOMOVEIS)
                .desiredPrice(new BigDecimal("22000"))
                .city("Erechim")
                .state("RS")
                .tags(List.of("celta", "chevrolet"))
                .build());

        assertThat(result.getId()).isEqualTo("item-1");
        assertThat(result.isActive()).isTrue();
        assertThat(result.getReferenceImageUrl()).isEqualTo("foto");
        assertThat(result.getOwnerName()).isEqualTo("Carlos Seller");
        assertThat(result.getLocation().getCity()).isEqualTo("Erechim");
    }

    @Test
    void listItemsWithMatchesShouldReturnOnlyActiveItemsAndMatchingInterestsFromOtherUsers() {
        SellerItem activeItem = baseSellerItem();
        SellerItem inactiveItem = baseSellerItem().toBuilder()
                .id("item-2")
                .title("Violao")
                .active(false)
                .build();
        InterestPost matchingInterest = baseInterest().toBuilder()
                .id("interest-1")
                .title("Procuro Celta 2012")
                .description("Quero um carro pequeno")
                .category(InterestCategory.AUTOMOVEIS)
                .tags(List.of("celta"))
                .build();
        InterestPost ownInterest = matchingInterest.toBuilder()
                .id("interest-2")
                .ownerId("seller-1")
                .build();

        when(sellerItemGateway.findByOwnerIdOrderByCreatedAtDesc("seller-1")).thenReturn(List.of(activeItem, inactiveItem));
        when(marketplaceUseCase.listInterests(any(InterestSearchFilter.class))).thenReturn(List.of(matchingInterest, ownInterest));

        List<SellerItemMatchesView> results = sellerItemService.listItemsWithMatches("seller-1", false);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getItem().getId()).isEqualTo("item-1");
        assertThat(results.get(0).getMatchingInterests()).extracting(InterestPost::getId).containsExactly("interest-1");
    }

    @Test
    void listItemsWithMatchesShouldIncludeInactiveItemsWhenRequestedWithoutMatches() {
        SellerItem activeItem = baseSellerItem();
        SellerItem inactiveItem = baseSellerItem().toBuilder()
                .id("item-2")
                .title("Violao")
                .active(false)
                .build();
        InterestPost matchingInterest = baseInterest().toBuilder()
                .id("interest-1")
                .title("Procuro Celta 2012")
                .description("Quero um carro pequeno")
                .category(InterestCategory.AUTOMOVEIS)
                .tags(List.of("celta"))
                .build();

        when(sellerItemGateway.findByOwnerIdOrderByCreatedAtDesc("seller-1")).thenReturn(List.of(activeItem, inactiveItem));
        when(marketplaceUseCase.listInterests(any(InterestSearchFilter.class))).thenReturn(List.of(matchingInterest));

        List<SellerItemMatchesView> results = sellerItemService.listItemsWithMatches("seller-1", true);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getItem().isActive()).isTrue();
        assertThat(results.get(0).getMatchingInterests()).extracting(InterestPost::getId).containsExactly("interest-1");
        assertThat(results.get(1).getItem().isActive()).isFalse();
        assertThat(results.get(1).getMatchingInterests()).isEmpty();
    }

    @Test
    void shareItemAsOfferShouldDelegateToMarketplaceUsingItemData() {
        when(sellerItemGateway.findById("item-1")).thenReturn(Optional.of(baseSellerItem()));
        Offer offer = Offer.builder().id("offer-1").interestPostId("interest-1").sellerId("seller-1").build();
        when(marketplaceUseCase.createOffer(eq("seller-1"), eq("interest-1"), any(CreateOfferCommand.class)))
                .thenReturn(offer);

        Offer result = sellerItemService.shareItemAsOffer("seller-1", "item-1", "interest-1", ShareSellerItemCommand.builder()
                .sellerPhone("54999990000")
                .includesDelivery(true)
                .build());

        ArgumentCaptor<CreateOfferCommand> commandCaptor = ArgumentCaptor.forClass(CreateOfferCommand.class);
        verify(marketplaceUseCase).createOffer(eq("seller-1"), eq("interest-1"), commandCaptor.capture());
        assertThat(result.getId()).isEqualTo("offer-1");
        assertThat(commandCaptor.getValue().getOfferedPrice()).isEqualTo(new BigDecimal("22000"));
        assertThat(commandCaptor.getValue().getSellerPhone()).isEqualTo("54999990000");
        assertThat(commandCaptor.getValue().getMessage()).contains("Celta 2012");
        assertThat(commandCaptor.getValue().isIncludesDelivery()).isTrue();
    }

    @Test
    void shareItemAsOfferShouldUseCustomPriceAndMessageWhenProvided() {
        when(sellerItemGateway.findById("item-1")).thenReturn(Optional.of(baseSellerItem()));
        when(marketplaceUseCase.createOffer(eq("seller-1"), eq("interest-1"), any(CreateOfferCommand.class)))
                .thenReturn(Offer.builder().id("offer-2").build());

        sellerItemService.shareItemAsOffer("seller-1", "item-1", "interest-1", ShareSellerItemCommand.builder()
                .offeredPrice(new BigDecimal("21000"))
                .sellerPhone("54999990000")
                .message("Posso te mostrar meu Celta.")
                .build());

        ArgumentCaptor<CreateOfferCommand> commandCaptor = ArgumentCaptor.forClass(CreateOfferCommand.class);
        verify(marketplaceUseCase).createOffer(eq("seller-1"), eq("interest-1"), commandCaptor.capture());
        assertThat(commandCaptor.getValue().getOfferedPrice()).isEqualTo(new BigDecimal("21000"));
        assertThat(commandCaptor.getValue().getMessage()).isEqualTo("Posso te mostrar meu Celta.");
    }

    @Test
    void deactivateItemShouldSaveInactiveItemForOwner() {
        when(sellerItemGateway.findById("item-1")).thenReturn(Optional.of(baseSellerItem()));
        when(sellerItemGateway.save(any(SellerItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerItem result = sellerItemService.deactivateItem("seller-1", "item-1");

        assertThat(result.isActive()).isFalse();
        verify(sellerItemGateway).save(any(SellerItem.class));
    }

    @Test
    void updateItemShouldPersistEditedItemForOwner() {
        when(sellerItemGateway.findById("item-1")).thenReturn(Optional.of(baseSellerItem()));
        when(sellerItemGateway.save(any(SellerItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SellerItem result = sellerItemService.updateItem("seller-1", "item-1", UpdateSellerItemCommand.builder()
                .title("Celta 2012 completo")
                .description("Carro revisado")
                .referenceImageUrl(" nova-foto ")
                .category(InterestCategory.AUTOMOVEIS)
                .desiredPrice(new BigDecimal("23000"))
                .city("Passo Fundo")
                .state("RS")
                .tags(List.of("celta", "completo"))
                .build());

        assertThat(result.getTitle()).isEqualTo("Celta 2012 completo");
        assertThat(result.getReferenceImageUrl()).isEqualTo("nova-foto");
        assertThat(result.getDesiredPrice()).isEqualTo(new BigDecimal("23000"));
        assertThat(result.getLocation().getCity()).isEqualTo("Passo Fundo");
        assertThat(result.getTags()).containsExactly("celta", "completo");
    }

    @Test
    void deactivateItemShouldRejectDifferentOwner() {
        when(sellerItemGateway.findById("item-1")).thenReturn(Optional.of(baseSellerItem()));

        assertThatThrownBy(() -> sellerItemService.deactivateItem("other-user", "item-1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("dono");
    }

    @Test
    void createItemShouldRejectUnknownUser() {
        when(userGateway.findById("seller-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sellerItemService.createItem("seller-1", CreateSellerItemCommand.builder()
                .title("Celta")
                .description("Carro")
                .category(InterestCategory.AUTOMOVEIS)
                .build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuario");
    }

    @Test
    void listItemsWithMatchesShouldIgnoreDifferentCategoryAndText() {
        SellerItem activeItem = baseSellerItem();
        InterestPost differentCategory = baseInterest().toBuilder()
                .id("interest-1")
                .title("Procuro apartamento")
                .category(InterestCategory.IMOVEIS)
                .tags(List.of("apartamento"))
                .build();
        InterestPost noTextMatch = baseInterest().toBuilder()
                .id("interest-2")
                .title("Procuro Gol")
                .description("Outro modelo")
                .category(InterestCategory.AUTOMOVEIS)
                .tags(List.of("gol"))
                .build();

        when(sellerItemGateway.findByOwnerIdOrderByCreatedAtDesc("seller-1")).thenReturn(List.of(activeItem));
        when(marketplaceUseCase.listInterests(any(InterestSearchFilter.class))).thenReturn(List.of(differentCategory, noTextMatch));

        List<SellerItemMatchesView> results = sellerItemService.listItemsWithMatches("seller-1", false);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMatchingInterests()).isEmpty();
    }

    private UserProfile baseUser() {
        return UserProfile.builder()
                .id("seller-1")
                .name("Carlos Seller")
                .email("carlos@teste.com")
                .build();
    }

    private SellerItem baseSellerItem() {
        return SellerItem.builder()
                .id("item-1")
                .ownerId("seller-1")
                .ownerName("Carlos Seller")
                .title("Celta 2012")
                .description("Carro conservado")
                .referenceImageUrl("foto")
                .category(InterestCategory.AUTOMOVEIS)
                .desiredPrice(new BigDecimal("22000"))
                .location(LocationInfo.builder().city("Erechim").state("RS").build())
                .tags(List.of("celta", "chevrolet"))
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private InterestPost baseInterest() {
        return InterestPost.builder()
                .ownerId("buyer-1")
                .ownerName("Ana Buyer")
                .status(InterestStatus.OPEN)
                .budgetMax(new BigDecimal("25000"))
                .location(LocationInfo.builder().city("Erechim").state("RS").build())
                .createdAt(Instant.now())
                .build();
    }
}
