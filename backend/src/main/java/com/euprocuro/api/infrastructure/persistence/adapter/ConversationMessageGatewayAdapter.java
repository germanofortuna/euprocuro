package com.euprocuro.api.infrastructure.persistence.adapter;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.ConversationMessageGateway;
import com.euprocuro.api.domain.model.ConversationMessage;
import com.euprocuro.api.infrastructure.persistence.mapper.ConversationMessagePersistenceMapper;
import com.euprocuro.api.infrastructure.persistence.repository.SpringDataConversationMessageRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ConversationMessageGatewayAdapter implements ConversationMessageGateway {

    private static final String DELETED_USER_LABEL = "Usuário excluído";
    private static final String REMOVED_MESSAGE_CONTENT = "[Mensagem removida por exclusão de conta]";

    private final SpringDataConversationMessageRepository repository;

    @Override
    public ConversationMessage save(ConversationMessage message) {
        return ConversationMessagePersistenceMapper.toDomain(
                repository.save(ConversationMessagePersistenceMapper.toDocument(message))
        );
    }

    @Override
    public List<ConversationMessage> findByOfferIdOrderByCreatedAtAsc(String offerId) {
        return repository.findByOfferIdOrderByCreatedAtAsc(offerId)
                .stream()
                .map(ConversationMessagePersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConversationMessage> findByOfferIdInOrderByCreatedAtAsc(List<String> offerIds) {
        if (offerIds == null || offerIds.isEmpty()) {
            return List.of();
        }

        return repository.findByOfferIdInOrderByCreatedAtAsc(offerIds)
                .stream()
                .map(ConversationMessagePersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByOfferIdIn(List<String> offerIds) {
        if (offerIds == null || offerIds.isEmpty()) {
            return;
        }
        repository.deleteByOfferIdIn(offerIds);
    }

    @Override
    public void deleteBySenderIdOrRecipientId(String userId) {
        repository.deleteBySenderIdOrRecipientId(userId, userId);
    }

    @Override
    public void anonymizeByUserId(String userId) {
        repository.findBySenderIdOrRecipientId(userId, userId)
                .stream()
                .map(message -> {
                    boolean isSender = userId.equals(message.getSenderId());
                    boolean isRecipient = userId.equals(message.getRecipientId());
                    if (isSender) {
                        message.setSenderId(null);
                        message.setSenderName(DELETED_USER_LABEL);
                        message.setContent(REMOVED_MESSAGE_CONTENT);
                    }
                    if (isRecipient) {
                        message.setRecipientId(null);
                        message.setRecipientName(DELETED_USER_LABEL);
                    }
                    return message;
                })
                .forEach(repository::save);
    }
}
