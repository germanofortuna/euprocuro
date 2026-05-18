package com.euprocuro.api.entrypoints.rest.dto.response;

import java.util.List;

import com.euprocuro.api.domain.model.StickerListingType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StickerDetailsResponse {
    StickerListingType type;
    String group;
    String selection;
    List<String> numbers;
    List<String> players;
}
