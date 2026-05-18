package com.euprocuro.api.application.command;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GoogleLoginCommand {
    private String idToken;
    private String ipAddress;
}
