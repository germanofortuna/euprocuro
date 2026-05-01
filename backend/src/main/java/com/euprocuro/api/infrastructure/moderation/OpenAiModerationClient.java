package com.euprocuro.api.infrastructure.moderation;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.euprocuro.api.infrastructure.moderation.dto.OpenAiModerationRequest;
import com.euprocuro.api.infrastructure.moderation.dto.OpenAiModerationResponse;

@FeignClient(
        name = "openAiModerationClient",
        url = "${application.moderation.openai.base-url:https://api.openai.com}"
)
public interface OpenAiModerationClient {

    @PostMapping(value = "/v1/moderations", consumes = MediaType.APPLICATION_JSON_VALUE)
    OpenAiModerationResponse moderate(
            @RequestHeader("Authorization") String authorization,
            @RequestBody OpenAiModerationRequest request
    );
}
