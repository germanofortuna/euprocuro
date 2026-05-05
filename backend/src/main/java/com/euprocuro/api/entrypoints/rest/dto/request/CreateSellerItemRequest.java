package com.euprocuro.api.entrypoints.rest.dto.request;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.euprocuro.api.domain.model.InterestCategory;

import lombok.Data;

@Data
public class CreateSellerItemRequest {
    @NotBlank
    @Size(max = 80)
    private String title;

    @NotBlank
    @Size(max = 120)
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
