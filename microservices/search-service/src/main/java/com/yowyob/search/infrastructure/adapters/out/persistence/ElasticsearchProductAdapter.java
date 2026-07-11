package com.yowyob.search.infrastructure.adapters.out.persistence;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import com.yowyob.search.application.ports.out.ProductSearchRepositoryPort;
import com.yowyob.search.infrastructure.adapters.out.persistence.document.ProductDocument;
import com.yowyob.search.domain.model.Product;
import com.yowyob.search.infrastructure.adapters.out.persistence.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchProductAdapter implements ProductSearchRepositoryPort {

    private final ProductSearchRepository searchRepository;
    private final ReactiveElasticsearchOperations elasticsearchOperations;

    private static final Map<String, List<String>> CATEGORY_ES_TERMS = Map.of(
        "Restaurant",   Arrays.asList("SERVICES_RESTAURANTS", "RESTAURATION", "Restaurant",
                                       "Boulangerie", "SERVICES_BOULANGERIES", "Brasserie"),
        "Immobilier",   Arrays.asList("SERVICES_HOTELS", "H\u00f4tellerie", "Hotel", "H\u00f4tel"),
        "Electronique", Arrays.asList("Electronique", "T\u00e9l\u00e9communications", "Technologie"),
        "Services",     Arrays.asList("SERVICES_PHARMACIES", "Pharmacie", "SANTE", "Sant\u00e9",
                                       "SERVICES_FINANCIERS", "Banque", "SERVICES_LOCAUX", "BTP", "Logistique"),
        "Automobile",   Arrays.asList("Automobile", "Transport maritime"),
        "Beaute",       Arrays.asList("Beaute", "Beaut\u00e9"),
        "Mode",         Arrays.asList("Mode", "Grande distribution")
    );

    @Override
    public Mono<Product> findById(String id) {
        return searchRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Mono<Product> save(Product product) {
        ProductDocument doc = toDocument(product);
        return searchRepository.save(doc)
                .map(this::toDomain);
    }

    @Override
    public Flux<Product> findAll() {
        return searchRepository.findAll()
                .map(this::toDomain);
    }

    @Override
    public Flux<Product> findAllPaged(int page, int size) {
        // match_all + pagination "from/size" classique. Pas de tri explicite : le
        // score est constant (1.0) pour match_all, l'ordre suit donc l'ordre de
        // segment/insertion — stable en pratique pour un index qui ne change pas
        // pendant la génération d'un sitemap, ce qui est le seul usage visé ici.
        Query pagedQuery = NativeQuery.builder()
                .withQuery(q -> q.matchAll(m -> m))
                .withPageable(org.springframework.data.domain.PageRequest.of(page, size))
                .build();

        return elasticsearchOperations.search(pagedQuery, ProductDocument.class)
                .map(SearchHit::getContent)
                .map(this::toDomain)
                // Elasticsearch refuse from+size > index.max_result_window (10 000 par
                // défaut) avec un search_phase_execution_exception — un client qui
                // paginerait jusqu'au bout (ex: génération de sitemap) tomberait sur
                // une 500 au lieu d'une simple liste vide signalant la fin. Testé en
                // conditions réelles : page=9999&size=10 plante sans ce repli.
                // Spring Data traduit l'exception ES brute en UncategorizedElasticsearchException
                // (hiérarchie org.springframework.dao.DataAccessException) avant qu'elle
                // n'atteigne ce point — c'est ce type-là qu'il faut intercepter, pas
                // l'exception ES brute du client bas niveau (vérifié via les logs réels).
                .onErrorResume(org.springframework.dao.DataAccessException.class,
                        e -> Flux.empty());
    }

    @Override
    public Mono<Long> count() {
        return elasticsearchOperations.count(ProductDocument.class);
    }

    @Override
    public Flux<Product> findByTitleOrDescription(String query) {
        // match_phrase_prefix gère les requêtes multi-mots (ex: "je veux manger"),
        // contrairement à la dérivation "Containing" qui génère un wildcard *"..."*
        // invalide et fait planter Elasticsearch sur les espaces.
        Query searchQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .should(s -> s.matchPhrasePrefix(mp -> mp.field("title").query(query)))
                        .should(s -> s.matchPhrasePrefix(mp -> mp.field("description").query(query)))
                        .minimumShouldMatch("1")))
                .withMaxResults(10)
                .build();

        return elasticsearchOperations.search(searchQuery, ProductDocument.class)
                .map(SearchHit::getContent)
                .map(this::toDomain);
    }

    @Override
    public Flux<Product> searchNative(String query, String inferredCategory, String extractedCity) {
        Query searchQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {
                    b.must(m -> m.bool(sub -> {
                        if (query != null && !query.isEmpty()) {
                            sub.should(s -> s.multiMatch(mm -> mm
                                    .query(query)
                                    .fields("title^3", "description", "category")
                                    .fuzziness("AUTO")
                                    .prefixLength(2)
                                    .operator(Operator.Or)
                            ));
                        }
                        if (inferredCategory != null) {
                            CATEGORY_ES_TERMS
                                .getOrDefault(inferredCategory, List.of(inferredCategory))
                                .stream()
                                .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                                .forEach(fv -> sub.should(s -> s.term(t -> t.field("category").value(fv))));
                        }
                        sub.minimumShouldMatch("1");
                        return sub;
                    }));

                    if (query != null && !query.isEmpty()) {
                        b.should(s -> s.matchPhrase(mp -> mp.field("description").query(query).boost(10.0f)));
                        b.should(s -> s.matchPhrase(mp -> mp.field("title").query(query).boost(15.0f)));
                    }

                    if (extractedCity != null) {
                        b.should(s -> s.match(m -> m.field("city").query(extractedCity).boost(3.0f)));
                    }

                    b.should(s -> s.term(t -> t.field("source.keyword").value("KERNEL_ORG").boost(50.0f)));

                    return b;
                }))
                .build();

        return elasticsearchOperations.search(searchQuery, ProductDocument.class)
                .map(SearchHit::getContent)
                .map(this::toDomain);
    }

    @Override
    public Flux<Product> searchKnn(float[] queryVector, int size) {
        List<Float> vectorList = new ArrayList<>();
        for (float v : queryVector) {
            vectorList.add(v);
        }

        NativeQuery knnQuery = NativeQuery.builder()
            .withKnnQuery(co.elastic.clients.elasticsearch._types.KnnQuery.of(knn -> knn
                .field("embedding")
                .queryVector(vectorList)
                .numCandidates(100)
                .k(size)
            ))
            .withMaxResults(size)
            .build();

        return elasticsearchOperations.search(knnQuery, ProductDocument.class)
                .map(SearchHit::getContent)
                .map(this::toDomain);
    }

    @Override
    public Flux<Product> searchGeoDistance(double latitude, double longitude, double radiusKm, String query) {
        Query searchQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {
                    if (query != null && !query.isEmpty()) {
                        b.must(m -> m.multiMatch(mm -> mm
                                .query(query)
                                .fields("title^3", "description", "category")
                                .fuzziness("AUTO")));
                    }

                    b.filter(f -> f.geoDistance(gd -> gd
                            .field("location")
                            .distance(radiusKm + "km")
                            .location(gl -> gl.latlon(latlon -> latlon
                                    .lat(latitude)
                                    .lon(longitude)))));

                    return b;
                }))
                .withSort(s -> s.geoDistance(gd -> gd
                        .field("location")
                        .location(gl -> gl.latlon(latlon -> latlon
                                .lat(latitude)
                                .lon(longitude)))
                        .order(co.elastic.clients.elasticsearch._types.SortOrder.Asc)))
                .build();

        return elasticsearchOperations.search(searchQuery, ProductDocument.class)
                .map(SearchHit::getContent)
                .map(this::toDomain);
    }

    private Product toDomain(ProductDocument doc) {
        if (doc == null) return null;
        return Product.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .price(doc.getPrice())
                .serviceType(doc.getServiceType())
                .type(doc.getType())
                .category(doc.getCategory())
                .city(doc.getCity())
                .quartier(doc.getQuartier())
                .rating(doc.getRating())
                .latitude(doc.getLatitude())
                .longitude(doc.getLongitude())
                .images(doc.getImages())
                .imageUrl(doc.getImageUrl())
                .phone(doc.getPhone())
                .website(doc.getWebsite())
                .openingHours(doc.getOpeningHours())
                .reviewsCount(doc.getReviewsCount())
                .openNow(doc.getOpenNow())
                .priceLevel(doc.getPriceLevel())
                .reviewsSummary(doc.getReviewsSummary())
                .googleMapsUrl(doc.getGoogleMapsUrl())
                .street(doc.getStreet())
                .source(doc.getSource())
                .embedding(doc.getEmbedding())
                .build();
    }

    private ProductDocument toDocument(Product domain) {
        if (domain == null) return null;
        ProductDocument doc = ProductDocument.builder()
                .id(domain.getId())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .price(domain.getPrice())
                .serviceType(domain.getServiceType())
                .type(domain.getType())
                .category(domain.getCategory())
                .city(domain.getCity())
                .quartier(domain.getQuartier())
                .rating(domain.getRating())
                .latitude(domain.getLatitude())
                .longitude(domain.getLongitude())
                .images(domain.getImages())
                .imageUrl(domain.getImageUrl())
                .phone(domain.getPhone())
                .website(domain.getWebsite())
                .openingHours(domain.getOpeningHours())
                .reviewsCount(domain.getReviewsCount())
                .openNow(domain.getOpenNow())
                .priceLevel(domain.getPriceLevel())
                .reviewsSummary(domain.getReviewsSummary())
                .googleMapsUrl(domain.getGoogleMapsUrl())
                .street(domain.getStreet())
                .source(domain.getSource())
                .embedding(domain.getEmbedding())
                .build();
        doc.getLocation(); // Initialize GeoPoint
        return doc;
    }
}
