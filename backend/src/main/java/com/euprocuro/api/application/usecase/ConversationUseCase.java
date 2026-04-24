package com.euprocuro.api.application.usecase;

import java.util.List;

import com.euprocuro.api.application.command.SendConversationMessageCommand;
import com.euprocuro.api.application.view.ConversationMessageView;
import com.euprocuro.api.application.view.OfferConversationView;

public interface ConversationUseCase {
    OfferConversationView getOfferConversation(String currentUserId, String offerId);

    ConversationMessageView sendMessage(String currentUserId, String offerId, SendConversationMessageCommand command);

    List<ConversationMessageView> listMessages(String currentUserId, String offerId);
}
