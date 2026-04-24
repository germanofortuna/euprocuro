package com.euprocuro.api.domain.gateway;

import com.euprocuro.api.domain.model.UserProfile;

public interface EmailGateway {
    boolean sendPasswordResetEmail(UserProfile user, String resetLink);
}
