package com.euprocuro.api.application.command;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConfirmRegistrationCommand {
    String email;
    String code;
    String ipAddress;
}
