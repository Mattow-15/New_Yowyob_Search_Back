package com.yowyob.listing.domain.exception;

import java.util.UUID;

/**
 * Exception métier : une annonce n'a pas été trouvée.
 * Aucune dépendance Spring — c'est une règle du domaine.
 */
public class ListingNotFoundException extends RuntimeException {

    public ListingNotFoundException(UUID id) {
        super("Listing not found with id: " + id);
    }

    public ListingNotFoundException(String osmId) {
        super("Listing not found with osmId: " + osmId);
    }
}
