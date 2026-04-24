package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.euprocuro.api.application.command.SendConversationMessageCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ForbiddenException;
import com.euprocuro.api.application.view.ConversationMessageView;
import com.euprocuro.api.application.view.OfferConversationView;
import com.euprocuro.api.domain.gateway.ConversationMessageGateway;
import com.euprocuro.api.domain.gateway.InterestGateway;
import com.euprocuro.api.domain.gateway.OfferGateway;
import com.euprocuro.api.domain.gateway.UserGateway;
import com.euprocuro.api.domain.model.ConversationMessage;
import com.euprocuro.api.domain.model.InterestCategory;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestStatus;
import com.euprocuro.api.domain.model.LocationInfo;
import com.euprocuro.api.domain.model.Offer;
import com.euprocuro.api.domain.model.UserProfile;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private OfferGateway offerGateway;
    @Mock
    private InterestGateway interestGateway;
    @Mock
    private UserGateway userGateway;
    @Mock
    private ConversationMessageGateway conversationMessageGateway;

    @InjectMocks
    private ConversationService conversationService;

    @Test
    void getOfferConversationShouldReturnMessagesForParticipant() {
        Offer offer = baseOffer();
        InterestPost interest = baseInterest();
        ConversationMessage message = baseMessage();

        when(offerGateway.findById("offer-1")).thenReturn(Optional.of(offer));
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(interest));
        when(conversationMessageGateway.findByOfferIdOrderByCreatedAtAsc("offer-1")).thenReturn(List.of(message));

        OfferConversationView result = conversationService.getOfferConversation("buyer-1", "offer-1");

        assertThat(result.getInterestTitle()).isEqualTo("Quero um violao");
        assertThat(result.getSellerEmail()).isEqualTo("carlos@teste.com");
        assertThat(result.getMessages()).hasSize(1);
        assertThat(result.getMessages().get(0).getContent()).isEqualTo("Ainda está disponível?");
    }

    @Test
    void getOfferConversationShouldRejectNonParticipant() {
        when(offerGateway.findById("offer-1")).thenReturn(Optional.of(baseOffer()));
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(baseInterest()));

        assertThatThrownBy(() -> conversationService.getOfferConversation("intruder", "offer-1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("acessar esta conversa");
    }

    @Test
    void sendMessageShouldRejectBlankContent() {
        assertThatThrownBy(() -> conversationService.sendMessage("buyer-1", "offer-1", SendConversationMessageCommand.builder()
                .content("   ")
                .build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vazia");
    }

    @Test
    void sendMessageShouldPersistTrimmedMessageForOtherParticipant() {
        Offer offer = baseOffer();
        InterestPost interest = baseInterest();
        UserProfile seller = UserProfile.builder().id("seller-1").name("Carlos").build();
        UserProfile buyer = UserProfile.builder().id("buyer-1").name("Ana").build();

        when(offerGateway.findById("offer-1")).thenReturn(Optional.of(offer));
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(interest));
        when(userGateway.findById("seller-1")).thenReturn(Optional.of(seller));
        when(userGateway.findById("buyer-1")).thenReturn(Optional.of(buyer));
        when(conversationMessageGateway.save(any(ConversationMessage.class))).thenAnswer(invocation -> {
            ConversationMessage message = invocation.getArgument(0);
            message.setId("msg-1");
            return message;
        });

        ConversationMessageView result = conversationService.sendMessage("seller-1", "offer-1", SendConversationMessageCommand.builder()
                .content("  Podemos combinar entrega.  ")
                .build());

        assertThat(result.getId()).isEqualTo("msg-1");
        assertThat(result.getRecipientId()).isEqualTo("buyer-1");
        assertThat(result.getContent()).isEqualTo("Podemos combinar entrega.");
    }

    @Test
    void sendMessageShouldAllowBuyerToReplyToSeller() {
        Offer offer = baseOffer();
        InterestPost interest = baseInterest();
        UserProfile buyer = UserProfile.builder().id("buyer-1").name("Ana").build();
        UserProfile seller = UserProfile.builder().id("seller-1").name("Carlos").build();

        when(offerGateway.findById("offer-1")).thenReturn(Optional.of(offer));
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(interest));
        when(userGateway.findById("buyer-1")).thenReturn(Optional.of(buyer));
        when(userGateway.findById("seller-1")).thenReturn(Optional.of(seller));
        when(conversationMessageGateway.save(any(ConversationMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConversationMessageView result = conversationService.sendMessage("buyer-1", "offer-1", SendConversationMessageCommand.builder()
                .content("Pode mandar fotos?")
                .build());

        assertThat(result.getRecipientId()).isEqualTo("seller-1");
        assertThat(result.getSenderId()).isEqualTo("buyer-1");
    }

    @Test
    void listMessagesShouldRejectUserOutsideConversation() {
        Offer offer = baseOffer();
        InterestPost interest = baseInterest();

        when(offerGateway.findById("offer-1")).thenReturn(Optional.of(offer));
        when(interestGateway.findById("interest-1")).thenReturn(Optional.of(interest));

        assertThatThrownBy(() -> conversationService.listMessages("intruder", "offer-1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("acessar esta conversa");
    }

    private Offer baseOffer() {
        return Offer.builder()
                .id("offer-1")
                .interestPostId("interest-1")
                .sellerId("seller-1")
                .sellerName("Carlos")
                .sellerEmail("carlos@teste.com")
                .sellerPhone("11999999999")
                .createdAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
    }

    private InterestPost baseInterest() {
        return InterestPost.builder()
                .id("interest-1")
                .ownerId("buyer-1")
                .ownerName("Ana")
                .title("Quero um violao")
                .description("Procuro violao")
                .category(InterestCategory.SERVICOS)
                .status(InterestStatus.OPEN)
                .location(LocationInfo.builder().city("Campinas").state("SP").build())
                .build();
    }

    private ConversationMessage baseMessage() {
        return ConversationMessage.builder()
                .id("msg-1")
                .offerId("offer-1")
                .interestPostId("interest-1")
                .senderId("seller-1")
                .senderName("Carlos")
                .recipientId("buyer-1")
                .recipientName("Ana")
                .content("Ainda está disponível?")
                .createdAt(Instant.now())
                .build();
    }
}
