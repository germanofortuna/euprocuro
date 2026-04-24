package com.euprocuro.api.entrypoints.rest.controller;

import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.euprocuro.api.application.usecase.ConversationUseCase;
import com.euprocuro.api.entrypoints.rest.dto.request.SendConversationMessageRequest;
import com.euprocuro.api.entrypoints.rest.dto.response.ConversationMessageResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.OfferConversationResponse;
import com.euprocuro.api.entrypoints.rest.mapper.RestMapper;
import com.euprocuro.api.entrypoints.rest.security.CurrentUserContext;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationUseCase conversationUseCase;

    @GetMapping("/{offerId}/conversation")
    public OfferConversationResponse getConversation(@PathVariable String offerId, HttpServletRequest request) {
        return RestMapper.toResponse(
                conversationUseCase.getOfferConversation(CurrentUserContext.userId(request), offerId)
        );
    }

    @GetMapping("/{offerId}/messages")
    public List<ConversationMessageResponse> listMessages(@PathVariable String offerId, HttpServletRequest request) {
        return conversationUseCase.listMessages(CurrentUserContext.userId(request), offerId)
                .stream()
                .map(RestMapper::toResponse)
                .collect(Collectors.toList());
    }

    @PostMapping("/{offerId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationMessageResponse sendMessage(
            @PathVariable String offerId,
            HttpServletRequest request,
            @Valid @RequestBody SendConversationMessageRequest requestBody
    ) {
        return RestMapper.toResponse(
                conversationUseCase.sendMessage(
                        CurrentUserContext.userId(request),
                        offerId,
                        RestMapper.toCommand(requestBody)
                )
        );
    }
}
