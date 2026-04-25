package com.euprocuro.api.domain.gateway;

import com.euprocuro.api.domain.model.UserProfile;

public interface EmailGateway {
    boolean sendPasswordResetEmail(UserProfile user, String resetLink);

    boolean sendOfferReceivedEmail(UserProfile buyer, String interestTitle, String sellerName);

    boolean sendConversationMessageEmail(UserProfile recipient, String senderName, String interestTitle, String messagePreview);

    boolean sendPurchaseConfirmationEmail(UserProfile user, String productName, String paymentMethod);

    boolean sendBoostActivatedEmail(UserProfile user, String interestTitle, String boostedUntil);
}
