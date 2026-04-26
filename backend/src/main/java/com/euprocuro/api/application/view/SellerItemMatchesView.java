package com.euprocuro.api.application.view;

import java.util.List;

import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.SellerItem;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SellerItemMatchesView {
    SellerItem item;
    List<InterestPost> matchingInterests;
}
