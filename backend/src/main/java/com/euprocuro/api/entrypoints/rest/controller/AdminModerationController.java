package com.euprocuro.api.entrypoints.rest.controller;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.euprocuro.api.application.usecase.AdminModerationUseCase;
import com.euprocuro.api.entrypoints.rest.dto.request.ModerationDecisionRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.SaveModerationRuleRequest;
import com.euprocuro.api.entrypoints.rest.dto.response.ActionMessageResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.AdminModerationResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.InterestResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.ModerationRuleResponse;
import com.euprocuro.api.entrypoints.rest.mapper.RestMapper;
import com.euprocuro.api.entrypoints.rest.security.CurrentUserContext;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/moderation")
@RequiredArgsConstructor
public class AdminModerationController {

    private final AdminModerationUseCase adminModerationUseCase;

    @GetMapping
    public AdminModerationResponse getQueue(HttpServletRequest request) {
        return RestMapper.toResponse(adminModerationUseCase.getModerationQueue(CurrentUserContext.userId(request)));
    }

    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public ModerationRuleResponse createRule(
            HttpServletRequest request,
            @Valid @RequestBody SaveModerationRuleRequest requestBody
    ) {
        return RestMapper.toResponse(adminModerationUseCase.saveRule(
                CurrentUserContext.userId(request),
                null,
                RestMapper.toCommand(requestBody)
        ));
    }

    @PutMapping("/rules/{id}")
    public ModerationRuleResponse updateRule(
            @PathVariable String id,
            HttpServletRequest request,
            @Valid @RequestBody SaveModerationRuleRequest requestBody
    ) {
        return RestMapper.toResponse(adminModerationUseCase.saveRule(
                CurrentUserContext.userId(request),
                id,
                RestMapper.toCommand(requestBody)
        ));
    }

    @DeleteMapping("/rules/{id}")
    public ActionMessageResponse deleteRule(@PathVariable String id, HttpServletRequest request) {
        adminModerationUseCase.deleteRule(CurrentUserContext.userId(request), id);
        return ActionMessageResponse.builder()
                .message("Regra removida.")
                .build();
    }

    @PostMapping("/interests/{id}/decision")
    public InterestResponse decideInterest(
            @PathVariable String id,
            HttpServletRequest request,
            @Valid @RequestBody ModerationDecisionRequest requestBody
    ) {
        return RestMapper.toResponse(adminModerationUseCase.decideInterest(
                CurrentUserContext.userId(request),
                id,
                RestMapper.toCommand(requestBody)
        ));
    }
}
