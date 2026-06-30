package com.yowyob.listing.application.services;

import com.yowyob.listing.application.ports.in.ManageListingUseCase;
import com.yowyob.listing.application.ports.in.ProcessScrapedListingUseCase;
import com.yowyob.listing.application.ports.out.ListingEventPublisherPort;
import com.yowyob.listing.application.ports.out.ListingRepositoryPort;
import com.yowyob.listing.domain.exception.ListingNotFoundException;
import com.yowyob.listing.domain.model.Listing;
import com.yowyob.listing.domain.model.ListingStatus;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service d'application — cœur de la logique métier.
 *
 * RÈGLE D'OR : PAS d'annotation Spring ici (@Service, @Transactional, etc.).
 * Cette classe est un POJO pur, instancié et câblé par DomainConfig.
 * Cela la rend 100% testable unitairement sans démarrer Spring.
 *
 * Implémente les deux ports entrants :
 *   - ManageListingUseCase  (flux REST)
 *   - ProcessScrapedListingUseCase  (flux Kafka → upsert → RabbitMQ)
 */
@Slf4j
public class ListingApplicationService
        implements ManageListingUseCase, ProcessScrapedListingUseCase {

    private static final UUID SYSTEM_CRAWLER_SELLER =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final ListingRepositoryPort repositoryPort;
    private final ListingEventPublisherPort eventPublisherPort;

    public ListingApplicationService(ListingRepositoryPort repositoryPort,
                                     ListingEventPublisherPort eventPublisherPort) {
        this.repositoryPort = repositoryPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    // ─── PORT ENTRANT : ProcessScrapedListingUseCase ─────────────────────────

    /**
     * Upsert idempotent : si un listing avec le même osmId existe déjà,
     * on le met à jour ; sinon on en crée un nouveau.
     * La logique de décision est ici, dans le domaine — pas dans l'adaptateur JPA.
     */
    @Override
    public Listing execute(Listing incomingListing) {
        Optional<Listing> existing = repositoryPort.findByOsmId(incomingListing.getOsmId());

        Listing toSave;
        boolean isNew;

        if (existing.isPresent()) {
            // Mise à jour via toBuilder() — possible grâce à @Builder(toBuilder=true)
            // sur le modèle du domaine (piège n°2 évité)
            toSave = existing.get().toBuilder()
                    .title(incomingListing.getTitle())
                    .description(incomingListing.getDescription())
                    .address(incomingListing.getAddress())
                    .latitude(incomingListing.getLatitude())
                    .longitude(incomingListing.getLongitude())
                    .imageUrl(incomingListing.getImageUrl())
                    .phone(incomingListing.getPhone())
                    .website(incomingListing.getWebsite())
                    .openingHours(incomingListing.getOpeningHours())
                    .rating(incomingListing.getRating())
                    .reviewsCount(incomingListing.getReviewsCount())
                    .updatedAt(LocalDateTime.now())
                    .build();
            isNew = false;
            log.info("[Upsert] Mise à jour osmId={}", incomingListing.getOsmId());
        } else {
            toSave = incomingListing.toBuilder()
                    .sellerId(incomingListing.getSellerId() != null
                            ? incomingListing.getSellerId()
                            : SYSTEM_CRAWLER_SELLER)
                    .status(ListingStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            isNew = true;
            log.info("[Upsert] Création osmId={}", incomingListing.getOsmId());
        }

        // NOTE sur les transactions (piège n°3) :
        // Le @Transactional est sur PostgresListingAdapter.save().
        // Si le save() lève une exception, on ne publiera pas l'événement.
        Listing saved = repositoryPort.save(toSave);

        if (isNew) {
            eventPublisherPort.publishListingCreated(saved);
        } else {
            eventPublisherPort.publishListingUpdated(saved);
        }

        return saved;
    }

    // ─── PORT ENTRANT : ManageListingUseCase (flux REST CRUD) ────────────────

    @Override
    public Listing createListing(Listing listing) {
        Listing toSave = listing.toBuilder()
                .status(listing.getStatus() != null ? listing.getStatus() : ListingStatus.ACTIVE)
                .sellerId(listing.getSellerId() != null ? listing.getSellerId() : SYSTEM_CRAWLER_SELLER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Listing saved = repositoryPort.save(toSave);
        eventPublisherPort.publishListingCreated(saved);
        return saved;
    }

    @Override
    public Optional<Listing> getListingById(UUID id) {
        return repositoryPort.findById(id);
    }

    @Override
    public List<Listing> getAllListings() {
        return repositoryPort.findAll();
    }

    @Override
    public List<Listing> searchListings(LocalDateTime updatedAfter) {
        if (updatedAfter == null) {
            return repositoryPort.findAll();
        }
        return repositoryPort.findByUpdatedAtAfter(updatedAfter);
    }

    @Override
    public List<Listing> getListingsBySellerId(UUID sellerId) {
        return repositoryPort.findBySellerId(sellerId);
    }

    @Override
    public Listing updateListing(UUID id, Listing listingDetails) {
        Listing existing = repositoryPort.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));

        Listing updated = existing.toBuilder()
                .title(listingDetails.getTitle())
                .description(listingDetails.getDescription())
                .price(listingDetails.getPrice())
                .category(listingDetails.getCategory())
                .address(listingDetails.getAddress())
                .latitude(listingDetails.getLatitude())
                .longitude(listingDetails.getLongitude())
                .imageUrl(listingDetails.getImageUrl())
                .phone(listingDetails.getPhone())
                .website(listingDetails.getWebsite())
                .openingHours(listingDetails.getOpeningHours())
                .rating(listingDetails.getRating())
                .reviewsCount(listingDetails.getReviewsCount())
                .status(listingDetails.getStatus())
                .updatedAt(LocalDateTime.now())
                .build();

        Listing saved = repositoryPort.save(updated);
        eventPublisherPort.publishListingUpdated(saved);
        return saved;
    }

    @Override
    public void deleteListing(UUID id) {
        repositoryPort.findById(id).ifPresent(listing -> {
            repositoryPort.delete(listing);
            eventPublisherPort.publishListingDeleted(listing);
        });
    }
}
