package com.yowyob.listing.infrastructure.adapters.in.web;

import com.yowyob.listing.application.ports.in.ManageListingUseCase;
import com.yowyob.listing.application.ports.in.ProcessScrapedListingUseCase;
import com.yowyob.listing.domain.model.Listing;
import com.yowyob.listing.domain.model.ListingStatus;
import com.yowyob.listing.infrastructure.adapters.in.web.dto.CrawlerListingRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * ADAPTATEUR ENTRANT — REST.
 *
 * Dépend uniquement des ports entrants (interfaces du domaine).
 * Ne connaît pas ListingApplicationService directement.
 */
@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Listings", description = "Endpoints for managing listings (products/services)")
public class ListingController {

    private final ManageListingUseCase manageListingUseCase;
    private final ProcessScrapedListingUseCase processScrapedListingUseCase;

    @PostMapping
    @Operation(summary = "Create a listing", description = "Creates a new listing for a product or service")
    public ResponseEntity<Listing> createListing(@RequestBody Listing listing) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(manageListingUseCase.createListing(listing));
    }

    @GetMapping
    @Operation(summary = "Get all listings", description = "Retrieve a list of all available listings")
    public ResponseEntity<List<Listing>> getAllListings() {
        return ResponseEntity.ok(manageListingUseCase.getAllListings());
    }

    @GetMapping("/search/documents")
    @Operation(summary = "Search Documents", description = "Internal endpoint for Crawler Service to fetch documents")
    public ResponseEntity<List<Listing>> searchDocuments(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime updatedAfter) {
        return ResponseEntity.ok(manageListingUseCase.searchListings(updatedAfter));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get listing by ID", description = "Retrieve a specific listing by its unique identifier")
    public ResponseEntity<Listing> getListingById(@PathVariable UUID id) {
        return manageListingUseCase.getListingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/seller/{sellerId}")
    @Operation(summary = "Get listings by seller", description = "Retrieve all listings belonging to a specific seller")
    public ResponseEntity<List<Listing>> getListingsBySeller(@PathVariable UUID sellerId) {
        return ResponseEntity.ok(manageListingUseCase.getListingsBySellerId(sellerId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update listing", description = "Update details of an existing listing")
    public ResponseEntity<Listing> updateListing(@PathVariable UUID id, @RequestBody Listing listing) {
        return ResponseEntity.ok(manageListingUseCase.updateListing(id, listing));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete listing", description = "Remove a listing from the platform")
    public ResponseEntity<Void> deleteListing(@PathVariable UUID id) {
        manageListingUseCase.deleteListing(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public String health() {
        return "Listing Service is running!";
    }

    @Deprecated(since = "2.0", forRemoval = true)
    @PostMapping("/crawler/ingest")
    @Operation(summary = "Ingest crawler listing", description = "DEPRECATED Internal endpoint - use Kafka event instead")
    public ResponseEntity<Listing> ingestCrawlerListing(@RequestBody CrawlerListingRequest request) {
        log.warn("DEPRECATED endpoint /crawler/ingest called — migrate to Kafka topic crawler.listings.events");
        String address = request.getAddress();
        String sourceCity = request.getSourceCity();
        String fullAddress = (address == null || address.isBlank()) ? sourceCity :
                             (sourceCity == null || sourceCity.isBlank()) ? address :
                             address + ", " + sourceCity;

        Listing listing = Listing.builder()
                .osmId(request.getOsmId())
                .externalId(request.getOsmId())
                .title(request.getName())
                .description("Source: " + (request.getSource() != null ? request.getSource() : "UNKNOWN"))
                .price(0.0)
                .category(request.getCategory() != null ? request.getCategory() : "GENERAL")
                .address(fullAddress)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status(ListingStatus.ACTIVE)
                .sellerId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(processScrapedListingUseCase.execute(listing));
    }
}
