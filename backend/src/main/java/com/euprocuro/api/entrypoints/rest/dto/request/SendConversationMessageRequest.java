package com.euprocuro.api.entrypoints.rest.dto.request;

import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class SendConversationMessageRequest {
    @NotBlank
    private String content;
}
