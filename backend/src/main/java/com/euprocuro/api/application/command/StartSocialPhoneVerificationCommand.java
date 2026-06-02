package com.euprocuro.api.application.command;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StartSocialPhoneVerificationCommand {
    String socialToken;
    String phone;
}
