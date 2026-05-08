package com.euprocuro.api.application.service;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.LocationInfo;
import com.euprocuro.api.domain.model.SellerItem;
import com.euprocuro.api.domain.model.UserProfile;

@Service
public class InterestDeliveryRankingService {

    private static final int BOOST_SCORE = 30;
    private static final int SAME_USER_CITY_SCORE = 40;
    private static final int SAME_USER_STATE_SCORE = 20;
    private static final int SAME_USER_COUNTRY_SCORE = 5;
    private static final int SAME_ITEM_CATEGORY_SCORE = 25;
    private static final int SAME_ITEM_CITY_SCORE = 20;
    private static final int SAME_ITEM_STATE_SCORE = 10;
    private static final int MAX_EXACT_TAG_SCORE = 36;
    private static final int MAX_TEXT_TOKEN_SCORE = 40;

    public List<InterestPost> rank(List<InterestPost> interests, Optional<UserProfile> user, List<SellerItem> sellerItems) {
        Instant now = Instant.now();
        List<SellerItem> activeSellerItems = Optional.ofNullable(sellerItems).orElse(List.of())
                .stream()
                .filter(SellerItem::isActive)
                .collect(Collectors.toList());

        return Optional.ofNullable(interests).orElse(List.of())
                .stream()
                .sorted(Comparator
                        .comparingInt((InterestPost interest) -> score(interest, user, activeSellerItems, now)).reversed()
                        .thenComparing(InterestPost::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    int score(InterestPost interest, Optional<UserProfile> user, List<SellerItem> sellerItems, Instant now) {
        int score = 0;
        if (isBoostActive(interest, now)) {
            score += BOOST_SCORE;
        }
        score += recencyScore(interest, now);
        score += userLocationScore(interest, user);
        score += sellerItemScore(interest, sellerItems);
        return score;
    }

    private int userLocationScore(InterestPost interest, Optional<UserProfile> user) {
        if (user.isEmpty() || interest.getLocation() == null) {
            return 0;
        }

        UserProfile profile = user.get();
        LocationInfo location = interest.getLocation();
        int score = 0;
        if (same(location.getCountry(), profile.getCountry())) {
            score += SAME_USER_COUNTRY_SCORE;
        }
        if (same(location.getState(), profile.getState())) {
            score += SAME_USER_STATE_SCORE;
        }
        if (same(location.getCity(), profile.getCity())) {
            score += SAME_USER_CITY_SCORE;
        }
        return score;
    }

    private int sellerItemScore(InterestPost interest, List<SellerItem> sellerItems) {
        if (sellerItems == null || sellerItems.isEmpty()) {
            return 0;
        }

        int score = 0;
        Set<String> interestTags = normalizedSet(interest.getTags());
        Set<String> interestTokens = tokens(
                safe(interest.getTitle()) + " "
                        + safe(interest.getDescription()) + " "
                        + String.join(" ", Optional.ofNullable(interest.getTags()).orElse(List.of()))
        );

        boolean categoryMatched = false;
        int exactTagScore = 0;
        int textTokenScore = 0;
        int locationScore = 0;

        for (SellerItem item : sellerItems) {
            if (!categoryMatched && same(interest.getCategory(), item.getCategory())) {
                categoryMatched = true;
            }

            Set<String> itemTags = normalizedSet(item.getTags());
            exactTagScore += Math.min(MAX_EXACT_TAG_SCORE, intersectionSize(interestTags, itemTags) * 12);

            Set<String> itemTokens = tokens(
                    safe(item.getTitle()) + " "
                            + safe(item.getDescription()) + " "
                            + String.join(" ", Optional.ofNullable(item.getTags()).orElse(List.of()))
            );
            textTokenScore += Math.min(MAX_TEXT_TOKEN_SCORE, intersectionSize(interestTokens, itemTokens) * 8);

            locationScore = Math.max(locationScore, sellerItemLocationScore(interest.getLocation(), item.getLocation()));
        }

        if (categoryMatched) {
            score += SAME_ITEM_CATEGORY_SCORE;
        }
        score += Math.min(MAX_EXACT_TAG_SCORE, exactTagScore);
        score += Math.min(MAX_TEXT_TOKEN_SCORE, textTokenScore);
        score += locationScore;
        return score;
    }

    private int sellerItemLocationScore(LocationInfo interestLocation, LocationInfo itemLocation) {
        if (interestLocation == null || itemLocation == null) {
            return 0;
        }
        int score = 0;
        if (same(interestLocation.getState(), itemLocation.getState())) {
            score += SAME_ITEM_STATE_SCORE;
        }
        if (same(interestLocation.getCity(), itemLocation.getCity())) {
            score += SAME_ITEM_CITY_SCORE;
        }
        return score;
    }

    private int recencyScore(InterestPost interest, Instant now) {
        if (interest.getCreatedAt() == null) {
            return 0;
        }
        long ageDays = Math.max(0, Duration.between(interest.getCreatedAt(), now).toDays());
        if (ageDays <= 1) {
            return 10;
        }
        if (ageDays <= 7) {
            return 6;
        }
        if (ageDays <= 30) {
            return 3;
        }
        return 0;
    }

    private boolean isBoostActive(InterestPost interest, Instant now) {
        return interest.getBoostedUntil() != null && interest.getBoostedUntil().isAfter(now);
    }

    private int intersectionSize(Set<String> left, Set<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        Set<String> copy = new HashSet<>(left);
        copy.retainAll(right);
        return copy.size();
    }

    private Set<String> normalizedSet(Collection<String> values) {
        return Optional.ofNullable(values).orElse(List.of())
                .stream()
                .map(this::normalize)
                .filter(value -> value.length() >= 3)
                .collect(Collectors.toSet());
    }

    private Set<String> tokens(String value) {
        return Stream.of(normalize(value).split("\\s+"))
                .map(String::trim)
                .filter(token -> token.length() >= 3)
                .collect(Collectors.toSet());
    }

    private boolean same(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        return !normalizedLeft.isBlank() && normalizedLeft.equals(normalizedRight);
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(safe(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT).trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
