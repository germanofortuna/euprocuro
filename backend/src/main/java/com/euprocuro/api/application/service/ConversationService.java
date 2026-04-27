package com.euprocuro.api.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.euprocuro.api.application.command.SendConversationMessageCommand;
import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ForbiddenException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.application.usecase.ConversationUseCase;
import com.euprocuro.api.application.view.ConversationMessageView;
import com.euprocuro.api.application.view.OfferConversationView;
import com.euprocuro.api.domain.gateway.ConversationMessageGateway;
import com.euprocuro.api.domain.gateway.EmailGateway;
import com.euprocuro.api.domain.gateway.EventPublisherGateway;
import com.euprocuro.api.domain.gateway.InterestGateway;
import com.euprocuro.api.domain.gateway.OfferGateway;
import com.euprocuro.api.domain.gateway.RealtimeMessageGateway;
import com.euprocuro.api.domain.gateway.UserGateway;
import com.euprocuro.api.domain.model.ConversationMessage;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.Offer;
import com.euprocuro.api.domain.model.UserProfile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConversationService implements ConversationUseCase {

    private final OfferGateway offerGateway;
    private final InterestGateway interestGateway;
    private final UserGateway userGateway;
    private final ConversationMessageGateway conversationMessageGateway;
    private final EmailGateway emailGateway;
    private final EventPublisherGateway eventPublisherGateway;
    private final RealtimeMessageGateway realtimeMessageGateway;

    @Override
    public OfferConversationView getOfferConversation(String currentUserId, String offerId) {
        Offer offer = requireOfferParticipant(currentUserId, offerId);
        InterestPost interest = interestGateway.findById(offer.getInterestPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Interesse nao encontrado."));

        return OfferConversationView.builder()
                .offerId(offer.getId())
                .interestPostId(interest.getId())
                .interestTitle(interest.getTitle())
                .buyerId(interest.getOwnerId())
                .buyerName(interest.getOwnerName())
                .sellerId(offer.getSellerId())
                .sellerName(offer.getSellerName())
                .sellerEmail(offer.getSellerEmail())
                .sellerPhone(offer.getSellerPhone())
                .offeredPrice(offer.getOfferedPrice())
                .messages(listMessages(currentUserId, offerId))
                .build();
    }

    @Override
    public ConversationMessageView sendMessage(String currentUserId, String offerId, SendConversationMessageCommand command) {
        if (!StringUtils.hasText(command.getContent())) {
            throw new BusinessException("A mensagem nao pode estar vazia.");
        }

        Offer offer = requireOfferParticipant(currentUserId, offerId);
        InterestPost interest = interestGateway.findById(offer.getInterestPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Interesse nao encontrado."));
        UserProfile sender = userGateway.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."));

        String recipientId = Objects.equals(currentUserId, offer.getSellerId()) ? interest.getOwnerId() : offer.getSellerId();
        UserProfile recipient = userGateway.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Destinatario nao encontrado."));

        ConversationMessage message = conversationMessageGateway.save(ConversationMessage.builder()
                .offerId(offerId)
                .interestPostId(interest.getId())
                .senderId(sender.getId())
                .senderName(sender.getName())
                .recipientId(recipient.getId())
                .recipientName(recipient.getName())
                .content(command.getContent().trim())
                .createdAt(Instant.now())
                .build());

        emailGateway.sendConversationMessageEmail(
                recipient,
                sender.getName(),
                interest.getTitle(),
                command.getContent().trim()
        );
        eventPublisherGateway.publish("conversation.message.created", Map.of(
                "messageId", message.getId(),
                "offerId", message.getOfferId(),
                "interestPostId", message.getInterestPostId(),
                "senderId", message.getSenderId(),
                "recipientId", message.getRecipientId(),
                "createdAt", message.getCreatedAt()
        ));
        realtimeMessageGateway.publishConversationMessage(recipient.getId(), message);

        return toView(message);
    }

    @Override
    public List<ConversationMessageView> listMessages(String currentUserId, String offerId) {
        requireOfferParticipant(currentUserId, offerId);
        return conversationMessageGateway.findByOfferIdOrderByCreatedAtAsc(offerId)
                .stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    private Offer requireOfferParticipant(String currentUserId, String offerId) {
        Offer offer = offerGateway.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta nao encontrada."));
        InterestPost interest = interestGateway.findById(offer.getInterestPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Interesse nao encontrado."));

        boolean isParticipant = Objects.equals(currentUserId, offer.getSellerId())
                || Objects.equals(currentUserId, interest.getOwnerId());
        if (!isParticipant) {
            throw new ForbiddenException("Voce nao pode acessar esta conversa.");
        }

        return offer;
    }

    private ConversationMessageView toView(ConversationMessage message) {
        return ConversationMessageView.builder()
                .id(message.getId())
                .offerId(message.getOfferId())
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .recipientId(message.getRecipientId())
                .recipientName(message.getRecipientName())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
