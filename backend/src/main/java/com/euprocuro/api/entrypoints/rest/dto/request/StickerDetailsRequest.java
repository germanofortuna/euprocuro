package com.euprocuro.api.entrypoints.rest.dto.request;

import java.util.ArrayList;
import java.util.List;

import com.euprocuro.api.domain.model.StickerListingType;

import lombok.Data;

@Data
public class StickerDetailsRequest {
    private StickerListingType type;
    private String group;
    private String selection;
    private List<String> numbers = new ArrayList<>();
}
