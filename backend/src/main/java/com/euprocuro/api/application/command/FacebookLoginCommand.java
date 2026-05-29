package com.euprocuro.api.application.command;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FacebookLoginCommand {
    private String accessToken;
    private String ipAddress;
}
