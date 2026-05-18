package com.euprocuro.api.domain.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class StickerDetails {
    private StickerListingType type;
    private String group;
    private String selection;
    private List<String> numbers;
    private List<String> players;
}
