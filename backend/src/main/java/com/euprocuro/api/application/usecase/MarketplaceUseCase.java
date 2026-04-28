package com.euprocuro.api.application.usecase;

import java.util.List;

import com.euprocuro.api.application.command.CreateInterestCommand;
import com.euprocuro.api.application.command.CreateOfferCommand;
import com.euprocuro.api.application.command.InterestSearchFilter;
import com.euprocuro.api.application.command.UpdateInterestCommand;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.Offer;

public interface MarketplaceUseCase {
    InterestPost createInterest(String currentUserId, CreateInterestCommand command);

    InterestPost updateInterest(String currentUserId, String interestId, UpdateInterestCommand command);

    InterestPost renewInterest(String currentUserId, String interestId);

    InterestPost closeInterest(String currentUserId, String interestId);

    void deleteInterest(String currentUserId, String interestId);

    List<InterestPost> listInterests(InterestSearchFilter filter);

    List<InterestPost> listInterests(InterestSearchFilter filter, int offset, int limit);

    InterestPost getInterest(String id);

    Offer createOffer(String currentUserId, String interestId, CreateOfferCommand command);

    List<Offer> listOffersByInterest(String currentUserId, String interestId);
}
