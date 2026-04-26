package com.euprocuro.api.entrypoints.rest.dto.request;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.euprocuro.api.domain.model.InterestCategory;

import lombok.Data;

@Data
public class UpdateSellerItemRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String description;

    private String referenceImageUrl;

    @NotNull
    private InterestCategory category;

    private BigDecimal desiredPrice;

    private String city;
    private String state;
    private String neighborhood;
    private List<String> tags = new ArrayList<>();
}
