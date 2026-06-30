package com.yowyob.listing.domain.exception;

import java.util.UUID;

/**
 * Exception métier : un avis n'a pas été trouvé.
 * Aucune dépendance Spring — c'est une règle du domaine.
 */
public class ReviewNotFoundException extends RuntimeException {

    public ReviewNotFoundException(UUID id) {
        super("Review not found with id: " + id);
    }
}
