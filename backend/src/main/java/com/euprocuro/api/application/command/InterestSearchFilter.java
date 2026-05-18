package com.euprocuro.api.application.command;

import java.math.BigDecimal;


import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InterestSearchFilter {
    String category;
    String city;
    String state;
    String neighborhood;
    BigDecimal maxBudget;
    String query;
    String stickerType;
    String stickerGroup;
    String stickerSelection;
    String stickerNumber;
    String stickerPlayer;
    boolean openOnly;
    String currentUserId;
}
