package com.yowyob.listing.application.ports.in;

import com.yowyob.listing.domain.model.Listing;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PORT ENTRANT — Opérations CRUD exposées via l'API REST.
 * Le Controller REST dépendra de cette interface, jamais du service directement.
 */
public interface ManageListingUseCase {

    Listing createListing(Listing listing);

    Optional<Listing> getListingById(UUID id);

    List<Listing> getAllListings();

    List<Listing> searchListings(LocalDateTime updatedAfter);

    List<Listing> getListingsBySellerId(UUID sellerId);

    Listing updateListing(UUID id, Listing listingDetails);

    void deleteListing(UUID id);
}
