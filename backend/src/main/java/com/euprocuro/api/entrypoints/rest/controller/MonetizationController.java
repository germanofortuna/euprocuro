package com.euprocuro.api.entrypoints.rest.controller;

import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.euprocuro.api.application.usecase.MonetizationUseCase;
import com.euprocuro.api.entrypoints.rest.dto.request.BoostInterestRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.PurchaseProductRequest;
import com.euprocuro.api.entrypoints.rest.dto.response.CheckoutResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.InterestResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.MonetizationAccountResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.MonetizationProductResponse;
import com.euprocuro.api.entrypoints.rest.mapper.RestMapper;
import com.euprocuro.api.entrypoints.rest.security.CurrentUserContext;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/monetization")
@RequiredArgsConstructor
public class MonetizationController {

    private final MonetizationUseCase monetizationUseCase;

    @GetMapping("/products")
    public List<MonetizationProductResponse> products() {
        return monetizationUseCase.listProducts()
                .stream()
                .map(RestMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/account")
    public MonetizationAccountResponse account(HttpServletRequest request) {
        return RestMapper.toResponse(monetizationUseCase.getAccount(CurrentUserContext.userId(request)));
    }

    @PostMapping("/purchase")
    public CheckoutResponse purchase(
            HttpServletRequest request,
            @Valid @RequestBody PurchaseProductRequest requestBody
    ) {
        return RestMapper.toResponse(
                monetizationUseCase.purchase(CurrentUserContext.userId(request), RestMapper.toCommand(requestBody))
        );
    }

    @PostMapping("/interests/{interestId}/boost")
    public InterestResponse boostInterest(
            @PathVariable String interestId,
            HttpServletRequest request,
            @Valid @RequestBody BoostInterestRequest requestBody
    ) {
        return RestMapper.toResponse(
                monetizationUseCase.boostInterest(
                        CurrentUserContext.userId(request),
                        interestId,
                        RestMapper.toCommand(requestBody)
                )
        );
    }
}
