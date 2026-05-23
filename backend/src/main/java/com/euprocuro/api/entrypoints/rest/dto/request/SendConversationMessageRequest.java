package com.euprocuro.api.entrypoints.rest.dto.request;

import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class SendConversationMessageRequest {
    @Size(max = 1000)
    private String content;
    private String imageUrl;
}
