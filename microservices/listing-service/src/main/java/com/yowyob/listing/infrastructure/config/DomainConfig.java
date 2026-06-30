package com.yowyob.listing.infrastructure.config;

import com.yowyob.listing.application.ports.in.ManageListingUseCase;
import com.yowyob.listing.application.ports.in.ManageReviewsUseCase;
import com.yowyob.listing.application.ports.in.ProcessScrapedListingUseCase;
import com.yowyob.listing.application.ports.out.ListingEventPublisherPort;
import com.yowyob.listing.application.ports.out.ListingRepositoryPort;
import com.yowyob.listing.application.ports.out.ReviewRepositoryPort;
import com.yowyob.listing.application.services.ListingApplicationService;
import com.yowyob.listing.application.services.ReviewApplicationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration du câblage (Dependency Injection).
 *
 * Rôle : instancier ListingApplicationService (qui n'a pas @Service)
 * et le connecter à ses adaptateurs (qui eux sont des @Component).
 *
 * C'est ici que Spring apprend l'existence du domaine.
 * Tout le reste du projet ne voit que les interfaces (ports).
 */
@Configuration
public class DomainConfig {

    /**
     * Instancie le service d'application et l'expose sous DEUX interfaces.
     * Spring injectera la même instance partout où ManageListingUseCase
     * ou ProcessScrapedListingUseCase est demandé.
     */
    // @Primary : sous Spring 7, les beans manageListingUseCase / processScrapedListingUseCase
    // exposent aussi le type concret ListingApplicationService → ambiguïté à l'injection.
    // On désigne celui-ci comme candidat par défaut.
    @Bean
    @Primary
    public ListingApplicationService listingApplicationService(
            ListingRepositoryPort repositoryPort,
            ListingEventPublisherPort eventPublisherPort) {
        return new ListingApplicationService(repositoryPort, eventPublisherPort);
    }

    @Bean
    public ManageListingUseCase manageListingUseCase(ListingApplicationService service) {
        return service;
    }

    @Bean
    public ProcessScrapedListingUseCase processScrapedListingUseCase(ListingApplicationService service) {
        return service;
    }

    // ─── Avis (Reviews) ───────────────────────────────────────────────────────

    @Bean
    public ReviewApplicationService reviewApplicationService(
            ReviewRepositoryPort reviewRepositoryPort,
            ListingRepositoryPort listingRepositoryPort) {
        return new ReviewApplicationService(reviewRepositoryPort, listingRepositoryPort);
    }

    @Bean
    public ManageReviewsUseCase manageReviewsUseCase(ReviewApplicationService service) {
        return service;
    }
}
