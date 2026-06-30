/**
 * Service for managing product and service listings.
 * Handles CRUD operations and publishes events via RabbitMQ.
 * @author Matteo Owona, Rouchda Yampen
 * @date 2024-01-14
 * @updated 2025-02-11
 */
package com.yowyob.listing.service;

import com.yowyob.auth.exception.ResourceNotFoundException;
import com.yowyob.config.RabbitMQConfig;
import com.yowyob.listing.entity.Listing;
import com.yowyob.listing.entity.ListingStatus;
import com.yowyob.listing.event.ListingEvent;
import com.yowyob.listing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Creates a new listing and publishes a creation event.
     *
     * @param listing the listing entity to create
     * @return the saved listing with generated ID and timestamps
     */
    public Listing createListing(Listing listing) {
        listing.setCreatedAt(LocalDateTime.now());
        listing.setUpdatedAt(LocalDateTime.now());
        if (listing.getStatus() == null) {
            listing.setStatus(ListingStatus.ACTIVE);
        }
        Listing saved_listing = listingRepository.save(listing);

        publishEvent(saved_listing, "CREATED");

        return saved_listing;
    }

    /**
     * Retrieves all listings from the database.
     *
     * @return list of all listings
     */
    public List<Listing> getAllListings() {
        return listingRepository.findAll();
    }

    /**
     * Searches listings updated after a given timestamp.
     *
     * @param updated_after the timestamp filter, null returns all listings
     * @return list of matching listings
     */
    public List<Listing> searchListings(LocalDateTime updated_after) {
        if (updated_after == null) {
            return listingRepository.findAll();
        }
        return listingRepository.findByUpdatedAtAfter(updated_after);
    }

    /**
     * Retrieves a listing by its unique identifier.
     *
     * @param id the listing UUID
     * @return optional containing the listing if found
     */
    public Optional<Listing> getListingById(UUID id) {
        return listingRepository.findById(id);
    }

    /**
     * Retrieves all listings belonging to a specific seller.
     *
     * @param seller_id the seller UUID
     * @return list of listings for the seller
     */
    public List<Listing> getListingsBySellerId(UUID seller_id) {
        return listingRepository.findBySellerId(seller_id);
    }

    /**
     * Updates an existing listing and publishes an update event.
     *
     * @param id              the listing UUID to update
     * @param listing_details the new listing details
     * @return the updated listing
     * @throws ResourceNotFoundException if the listing does not exist
     */
    public Listing updateListing(UUID id, Listing listing_details) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", id.toString()));

        listing.setTitle(listing_details.getTitle());
        listing.setDescription(listing_details.getDescription());
        listing.setPrice(listing_details.getPrice());
        listing.setCategory(listing_details.getCategory());
        listing.setAddress(listing_details.getAddress());
        listing.setLatitude(listing_details.getLatitude());
        listing.setLongitude(listing_details.getLongitude());
        listing.setStatus(listing_details.getStatus());
        listing.setUpdatedAt(LocalDateTime.now());

        Listing updated_listing = listingRepository.save(listing);

        publishEvent(updated_listing, "UPDATED");

        return updated_listing;
    }

    /**
     * Deletes a listing and publishes a deletion event.
     *
     * @param id the listing UUID to delete
     */
    public void deleteListing(UUID id) {
        Listing listing = listingRepository.findById(id).orElse(null);
        if (listing != null) {
            listingRepository.delete(listing);
            publishEvent(listing, "DELETED");
        }
    }

    /**
     * Publishes a listing event to RabbitMQ for asynchronous processing.
     *
     * @param listing    the listing that triggered the event
     * @param event_type the type of event (CREATED, UPDATED, DELETED)
     */
    private void publishEvent(Listing listing, String event_type) {
        ListingEvent event = ListingEvent.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .price(listing.getPrice())
                .category(listing.getCategory())
                .address(listing.getAddress())
                .latitude(listing.getLatitude())
                .longitude(listing.getLongitude())
                .status(listing.getStatus().name())
                .sellerId(listing.getSellerId())
                .eventType(event_type)
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.LISTING_EXCHANGE, "listing." + event_type.toLowerCase(), event);
    }
}
