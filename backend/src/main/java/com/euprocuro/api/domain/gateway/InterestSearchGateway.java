package com.euprocuro.api.domain.gateway;

import java.util.List;

import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestSearchCriteria;

public interface InterestSearchGateway {
    List<InterestPost> search(InterestSearchCriteria criteria, int offset, int limit);
}
