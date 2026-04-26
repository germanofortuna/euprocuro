package com.euprocuro.api.entrypoints.rest.controller;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.euprocuro.api.application.command.InterestSearchFilter;
import com.euprocuro.api.application.usecase.DashboardUseCase;
import com.euprocuro.api.application.usecase.MarketplaceUseCase;
import com.euprocuro.api.domain.model.InterestCategory;
import com.euprocuro.api.entrypoints.rest.dto.request.CreateInterestRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.CreateOfferRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.UpdateInterestRequest;
import com.euprocuro.api.entrypoints.rest.dto.response.CategoryOptionResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.InterestResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.OfferResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.PersonalDashboardResponse;
import com.euprocuro.api.entrypoints.rest.mapper.RestMapper;
import com.euprocuro.api.entrypoints.rest.security.CurrentUserContext;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MarketplaceController {

    private final MarketplaceUseCase marketplaceUseCase;
    private final DashboardUseCase dashboardUseCase;

    @GetMapping("/categories")
    public List<CategoryOptionResponse> listCategories() {
        return Arrays.stream(InterestCategory.values())
                .map(RestMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/dashboard")
    public PersonalDashboardResponse dashboard(HttpServletRequest request) {
        return RestMapper.toResponse(dashboardUseCase.getDashboard(CurrentUserContext.userId(request)));
    }

    @GetMapping("/interests")
    public List<InterestResponse> listInterests(
            @RequestParam(required = false) InterestCategory category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BigDecimal maxBudget,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "true") boolean openOnly
    ) {
        InterestSearchFilter filter = InterestSearchFilter.builder()
                .category(category)
                .city(city)
                .maxBudget(maxBudget)
                .query(query)
                .openOnly(openOnly)
                .build();

        return marketplaceUseCase.listInterests(filter)
                .stream()
                .map(RestMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/interests/{id}")
    public InterestResponse getInterest(@PathVariable String id) {
        return RestMapper.toResponse(marketplaceUseCase.getInterest(id));
    }

    @PostMapping("/interests")
    @ResponseStatus(HttpStatus.CREATED)
    public InterestResponse createInterest(
            HttpServletRequest request,
            @Valid @RequestBody CreateInterestRequest requestBody
    ) {
        return RestMapper.toResponse(
                marketplaceUseCase.createInterest(CurrentUserContext.userId(request), RestMapper.toCommand(requestBody))
        );
    }

    @PutMapping("/interests/{id}")
    public InterestResponse updateInterest(
            @PathVariable String id,
            HttpServletRequest request,
            @Valid @RequestBody UpdateInterestRequest requestBody
    ) {
        return RestMapper.toResponse(
                marketplaceUseCase.updateInterest(CurrentUserContext.userId(request), id, RestMapper.toCommand(requestBody))
        );
    }

    @PatchMapping("/interests/{id}/close")
    public InterestResponse closeInterest(@PathVariable String id, HttpServletRequest request) {
        return RestMapper.toResponse(
                marketplaceUseCase.closeInterest(CurrentUserContext.userId(request), id)
        );
    }

    @DeleteMapping("/interests/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInterest(@PathVariable String id, HttpServletRequest request) {
        marketplaceUseCase.deleteInterest(CurrentUserContext.userId(request), id);
    }

    @GetMapping("/interests/{id}/offers")
    public List<OfferResponse> listOffers(@PathVariable String id, HttpServletRequest request) {
        return marketplaceUseCase.listOffersByInterest(CurrentUserContext.userId(request), id)
                .stream()
                .map(RestMapper::toResponse)
                .collect(Collectors.toList());
    }

    @PostMapping("/interests/{id}/offers")
    @ResponseStatus(HttpStatus.CREATED)
    public OfferResponse createOffer(
            @PathVariable String id,
            HttpServletRequest request,
            @Valid @RequestBody CreateOfferRequest requestBody
    ) {
        return RestMapper.toResponse(
                marketplaceUseCase.createOffer(CurrentUserContext.userId(request), id, RestMapper.toCommand(requestBody))
        );
    }
}
