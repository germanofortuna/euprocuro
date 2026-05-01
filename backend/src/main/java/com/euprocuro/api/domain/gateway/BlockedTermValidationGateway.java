package com.euprocuro.api.domain.gateway;

import java.util.Optional;

import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.SellerItem;

/**
 * Gateway for validating if an interest post contains blocked terms.
 */
public interface BlockedTermValidationGateway {

    /**
     * Validates if an interest post contains any blocked terms.
     * 
     * @param interest The interest post to validate
     * @return A validation result containing the blocked term if found, or empty if valid
     */
    Optional<BlockedTermValidationResult> validateBlockedTerms(InterestPost interest);

    /**
     * Validates if a seller item contains any blocked terms.
     * 
     * @param item The seller item to validate
     * @return A validation result containing the blocked term if found, or empty if valid
     */
    Optional<BlockedTermValidationResult> validateBlockedTerms(SellerItem item);

    /**
     * Result of a blocked term validation
     */
    class BlockedTermValidationResult {
        private final String term;
        private final String reason;

        public BlockedTermValidationResult(String term, String reason) {
            this.term = term;
            this.reason = reason;
        }

        public String getTerm() {
            return term;
        }

        public String getReason() {
            return reason;
        }
    }
}



