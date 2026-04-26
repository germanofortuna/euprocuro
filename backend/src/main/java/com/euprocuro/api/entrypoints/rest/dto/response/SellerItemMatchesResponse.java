package com.euprocuro.api.entrypoints.rest.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SellerItemMatchesResponse {
    SellerItemResponse item;
    List<InterestResponse> matchingInterests;
    int matchCount;
}
