package com.euprocuro.api.application.view;

import java.util.List;

import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.UserProfile;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PersonalDashboardView {
    UserProfile user;
    long totalActiveInterests;
    long totalOffersSent;
    long totalOffersReceived;
    List<InterestPost> myInterests;
    List<DashboardOfferView> offersSent;
    List<DashboardOfferView> offersReceived;
}
