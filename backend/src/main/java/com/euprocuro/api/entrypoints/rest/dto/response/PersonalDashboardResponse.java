package com.euprocuro.api.entrypoints.rest.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PersonalDashboardResponse {
    UserResponse user;
    long totalActiveInterests;
    long totalOffersSent;
    long totalOffersReceived;
    List<InterestResponse> myInterests;
    List<DashboardOfferResponse> offersSent;
    List<DashboardOfferResponse> offersReceived;
}
