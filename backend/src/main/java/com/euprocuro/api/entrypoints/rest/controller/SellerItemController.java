package com.euprocuro.api.entrypoints.rest.controller;

import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.euprocuro.api.application.usecase.SellerItemUseCase;
import com.euprocuro.api.entrypoints.rest.dto.request.CreateSellerItemRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.ShareSellerItemRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.UpdateSellerItemRequest;
import com.euprocuro.api.entrypoints.rest.dto.response.OfferResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.SellerItemMatchesResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.SellerItemResponse;
import com.euprocuro.api.entrypoints.rest.mapper.RestMapper;
import com.euprocuro.api.entrypoints.rest.security.CurrentUserContext;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/seller-items")
@RequiredArgsConstructor
public class SellerItemController {

    private final SellerItemUseCase sellerItemUseCase;

    @GetMapping
    public List<SellerItemMatchesResponse> listItemsWithMatches(HttpServletRequest request) {
        return sellerItemUseCase.listItemsWithMatches(CurrentUserContext.userId(request))
                .stream()
                .map(RestMapper::toResponse)
                .collect(Collectors.toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SellerItemResponse createItem(
            HttpServletRequest request,
            @Valid @RequestBody CreateSellerItemRequest requestBody
    ) {
        return RestMapper.toResponse(
                sellerItemUseCase.createItem(CurrentUserContext.userId(request), RestMapper.toCommand(requestBody))
        );
    }

    @PutMapping("/{itemId}")
    public SellerItemResponse updateItem(
            @PathVariable String itemId,
            HttpServletRequest request,
            @Valid @RequestBody UpdateSellerItemRequest requestBody
    ) {
        return RestMapper.toResponse(
                sellerItemUseCase.updateItem(CurrentUserContext.userId(request), itemId, RestMapper.toCommand(requestBody))
        );
    }

    @PatchMapping("/{itemId}/deactivate")
    public SellerItemResponse deactivateItem(@PathVariable String itemId, HttpServletRequest request) {
        return RestMapper.toResponse(
                sellerItemUseCase.deactivateItem(CurrentUserContext.userId(request), itemId)
        );
    }

    @PostMapping("/{itemId}/interests/{interestId}/offer")
    @ResponseStatus(HttpStatus.CREATED)
    public OfferResponse shareItemAsOffer(
            @PathVariable String itemId,
            @PathVariable String interestId,
            HttpServletRequest request,
            @Valid @RequestBody ShareSellerItemRequest requestBody
    ) {
        return RestMapper.toResponse(
                sellerItemUseCase.shareItemAsOffer(
                        CurrentUserContext.userId(request),
                        itemId,
                        interestId,
                        RestMapper.toCommand(requestBody)
                )
        );
    }
}
