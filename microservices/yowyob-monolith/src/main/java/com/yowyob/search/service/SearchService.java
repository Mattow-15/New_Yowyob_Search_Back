/**
 * Core search service for the YowYob platform.
 * Provides full-text search, proximity search, autocomplete, and indexing
 * via Elasticsearch. Integrates with GeoService for location-based queries.
 * @author Matteo Owona, Rouchda Yampen
 * @date 2024-01-14
 * @updated 2025-02-11
 */
package com.yowyob.search.service;

import com.yowyob.geo.service.GeoService;
import com.yowyob.geo.service.IpGeolocationService;
import com.yowyob.search.document.ProductDocument;
import com.yowyob.search.dto.SearchResponse;
import com.yowyob.search.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;

import java.util.List;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ProductSearchRepository searchRepository;
    private final ReactiveElasticsearchOperations elasticsearchOperations;
    private final GeoService geoService;
    private final IpGeolocationService ipGeolocationService;
    private final KeywordParser keywordParser;
    private final SearchHistoryService searchHistoryService;
    private final EmbeddingClient embeddingClient;

    /**
     * Normalise une chaîne en supprimant les accents "Yaoundé" → "Yaounde".
     *
     * @param input the string to normalize
     * @return the normalized string without accents
     */
    private String normalizeAccents(String input) {
        if (input == null)
            return "";
        return input.replace("é", "e").replace("È", "E").replace("É", "E").replace("è", "e").replace("ê", "e")
                .replace("ë", "e").replace("à", "a").replace("â", "a").replace("ù", "u").replace("û", "u")
                .replace("ô", "o").replace("ö", "o").replace("ç", "c").replace("Ç", "C").replace("î", "i")
                .replace("ï", "i");
    }

    /**
     * Main search method supporting full-text, type-filtered, city-filtered,
     * and proximity-based searches.
     *
     * @param query      the search query string
     * @param type       the type filter (shop, product, service, all)
     * @param city       the city filter
     * @param user_id    the authenticated user ID for search history
     * @param ip_address the client IP for geolocation-based proximity
     * @return the search response with matching results
     */
    public Mono<SearchResponse> search(String query, String type, String city, String user_id, String ip_address) {
        log.info("Searching: query={}, type={}, city={}, userId={}, ip={}", query, type, city, user_id, ip_address);

        KeywordParser.ParsedQueryResult parsed = keywordParser.parseWithCity(query);

        if (parsed.is_proximity_search && ip_address != null) {
            log.info("Proximity search detected via IP: {}", ip_address);
            return ipGeolocationService.getLocationFromIp(ip_address).flatMap(location -> {
                if (location.getLatitude() != null && location.getLongitude() != null) {
                    return searchByUserProximity(query, location.getLatitude(), location.getLongitude(), type);
                }
                return Mono.empty();
            }).switchIfEmpty(Mono.defer(() -> executeStandardSearch(parsed, query, type, city, user_id)));
        }

        return executeStandardSearch(parsed, query, type, city, user_id);
    }

    /**
     * Executes a standard Elasticsearch search with optional type and city filters.
     *
     * @param parsed         the parsed query result with extracted metadata
     * @param original_query the original raw query string
     * @param type           the type filter
     * @param city           the city filter
     * @param user_id        the user ID for history persistence
     * @return the search response
     */
    private Mono<SearchResponse> executeStandardSearch(KeywordParser.ParsedQueryResult parsed, String original_query,
            String type, String city, String user_id) {
        Flux<ProductDocument> result_flux;
        String tempCity = city;
        if (parsed.query != null && !parsed.query.isEmpty()) {
            final String parsed_query = parsed.query;
            final String extracted_city = parsed.extracted_city;
            String inferred_category = parsed.inferred_category;

            if (tempCity == null && extracted_city != null) {
                tempCity = extracted_city;
            }
            final String effective_city = tempCity;

            // --- 1. Générer l'embedding pour la requête utilisateur ---
            return embeddingClient.generateEmbedding(original_query)
                    .flatMap(vectorList -> {
                        float[] vectorArray = null;
                        if (vectorList != null && !vectorList.isEmpty()) {
                            vectorArray = new float[vectorList.size()];
                            for (int i = 0; i < vectorList.size(); i++) {
                                vectorArray[i] = vectorList.get(i);
                            }
                        }

                        final float[] finalVectorArray = vectorArray; // effectively final for lambda

                        Query searchQuery = NativeQuery.builder()
                                .withQuery(q -> q.bool(b -> {
                                    if (parsed_query != null && !parsed_query.isEmpty()) {
                                        // CHANGED: must -> should pour laisser le k-NN dominer le ranking
                                        // Le multiMatch BM25 est optionnel et boost faible pour les requêtes
                                        // sémantiques
                                        b.should(s -> s.multiMatch(mm -> mm
                                                .query(parsed_query)
                                                .fields("title^2", "description^1", "category^1")
                                                .fuzziness("AUTO")
                                                .prefixLength(1)
                                                .operator(Operator.Or)
                                                .boost(1.0f)));

                                        // Phrase match booste les correspondances exactes
                                        b.should(s -> s.matchPhrase(mp -> mp.field("description").query(parsed_query)
                                                .boost(3.0f)));
                                        b.should(s -> s
                                                .matchPhrase(mp -> mp.field("title").query(parsed_query).boost(5.0f)));
                                    }

                                    if (inferred_category != null) {
                                        b.should(s -> s.match(m -> m.field("category").query(inferred_category)
                                                .boost(3.0f)));
                                        b.should(s -> s.match(
                                                m -> m.field("serviceType").query(inferred_category).boost(3.0f)));
                                    }

                                    // minimum_should_match = 0 : les résultats purement sémantiques (sans
                                    // mots-clés)
                                    // peuvent quand même apparaître grâce au k-NN
                                    b.minimumShouldMatch("0");

                                    return b;
                                }))
                                .withKnnSearches(co.elastic.clients.elasticsearch._types.KnnSearch.of(knn -> {
                                    if (finalVectorArray != null) {
                                        List<Float> vectorListForKnn = new java.util.ArrayList<>();
                                        for (float f : finalVectorArray) {
                                            vectorListForKnn.add(f);
                                        }
                                        // CHANGED: k=20 et boost=2.0 → le score sémantique pèse 2x plus
                                        return knn.field("text_vector")
                                                .queryVector(vectorListForKnn)
                                                .k(20)
                                                .numCandidates(100)
                                                .boost(2.0f);
                                    }
                                    return knn;
                                }))
                                .build();

                        return getProcessedFlux(elasticsearchOperations.search(searchQuery, ProductDocument.class)
                                .map(SearchHit::getContent), type, effective_city, parsed, user_id, original_query);
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        // Fallback si le service Python est en panne
                        Query search_query = NativeQuery.builder()
                                .withQuery(q -> q.bool(b -> {
                                    // (Même logique booléenne sans k-NN)
                                    b.must(m -> m.multiMatch(mm -> mm
                                            .query(parsed_query)
                                            .fields("title^3", "description", "category")
                                            .fuzziness("AUTO")
                                            .prefixLength(2)
                                            .operator(Operator.Or)));
                                    return b;
                                })).build();
                        return getProcessedFlux(elasticsearchOperations.search(search_query, ProductDocument.class)
                                .map(SearchHit::getContent), type, effective_city, parsed, user_id, original_query);
                    }));
        } else {
            result_flux = searchRepository.findAll();
            return getProcessedFlux(result_flux, type, tempCity, parsed, user_id, original_query);
        }
    }

    private Mono<SearchResponse> getProcessedFlux(Flux<ProductDocument> result_flux, String type, String effective_city,
            KeywordParser.ParsedQueryResult parsed, String user_id, String original_query) {

        result_flux = result_flux.distinct();

        if (type != null && !"all".equalsIgnoreCase(type)) {
            final String filter_type = type.toLowerCase();
            result_flux = result_flux.filter(doc -> {
                String doc_service_type = (doc.getServiceType() != null) ? doc.getServiceType().toLowerCase() : "";
                String doc_type = (doc.getType() != null) ? doc.getType().toLowerCase() : "";

                if (filter_type.equals("shop")) {
                    return "user".equals(doc_service_type) || "shop".equals(doc_type);
                } else if (filter_type.equals("product") || filter_type.equals("products")) {
                    return "listing".equals(doc_service_type) || "product".equals(doc_type);
                } else if (filter_type.equals("service") || filter_type.equals("services")) {
                    return "service".equals(doc_type);
                }

                return filter_type.equals(doc_service_type) || filter_type.equals(doc_type);
            });
        }

        final String final_city_for_filter = effective_city;
        if (final_city_for_filter != null && !final_city_for_filter.isEmpty()) {
            final String normalized_filter_city = normalizeAccents(final_city_for_filter).toLowerCase();
            result_flux = result_flux.filter(doc -> {
                if (doc.getCity() == null)
                    return false;
                String normalized_doc_city = normalizeAccents(doc.getCity()).toLowerCase();
                boolean direct_match = doc.getCity().equalsIgnoreCase(final_city_for_filter);
                boolean normalized_match = normalized_doc_city.equals(normalized_filter_city);
                return direct_match || normalized_match;
            });
        }

        return result_flux.map(doc -> toProductDto(doc, null, null)).collectList()
                .map(results -> SearchResponse.builder().success(true)
                        .query(parsed.query).total(results.size()).results(results).build())
                .flatMap(response -> {
                    if (user_id != null && parsed.query != null && !parsed.query.isEmpty()) {
                        return searchHistoryService.saveSearch(user_id, parsed.query, type, effective_city)
                                .thenReturn(response);
                    }
                    return Mono.just(response);
                });
    }

    /**
     * Indexes a product document in Elasticsearch.
     * Automatically geocodes the city if coordinates are missing.
     *
     * @param product the product to index
     * @return the saved product document
     */
    public Mono<ProductDocument> indexProduct(ProductDocument product) {

        // 1. Concaténer les champs importants pour la sémantique
        String textToEmbed = String.format("%s %s %s",
                product.getTitle() != null ? product.getTitle() : "",
                product.getCategory() != null ? product.getCategory() : "",
                product.getDescription() != null ? product.getDescription() : "").trim();

        // 2. Assurer la localisation
        Mono<ProductDocument> locationMono = Mono.just(product);
        if (product.getCity() != null && (product.getLatitude() == null || product.getLongitude() == null)) {
            locationMono = geoService.geocode(product.getCity()).map(geo_location -> {
                product.setLatitude(geo_location.getLatitude());
                product.setLongitude(geo_location.getLongitude());
                return product;
            }).switchIfEmpty(Mono.just(product));
        }

        // 3. Obtenir le vecteur et sauvegarder
        return locationMono.flatMap(p -> {
            if (!textToEmbed.isEmpty()) {
                return embeddingClient.generateEmbedding(textToEmbed)
                        .flatMap(vectorList -> {
                            if (vectorList != null && !vectorList.isEmpty()) {
                                float[] vectorArray = new float[vectorList.size()];
                                for (int i = 0; i < vectorList.size(); i++) {
                                    vectorArray[i] = vectorList.get(i);
                                }
                                p.setTextVector(vectorArray);
                            }
                            return searchRepository.save(p);
                        })
                        .switchIfEmpty(searchRepository.save(p)); // Fallback si embedding échoue
            }
            return searchRepository.save(p);
        })
                .doOnNext(saved -> log.info("Product indexed with vector & geo: {}", saved.getTitle()));
    }

    /**
     * Retrieves a single product document by its ID.
     *
     * @param id the product document ID
     * @return the product document if found
     */
    public Mono<ProductDocument> getProductById(String id) {
        log.info("Getting product details for ID: {}", id);
        return searchRepository.findById(id).doOnNext(doc -> log.info("Found product: {}", doc.getTitle()))
                .doOnError(error -> log.error("Error retrieving product: {}", error.getMessage()));
    }

    /**
     * Searches products by proximity to the user's location.
     * Uses manual distance filtering and sorts results by distance.
     *
     * @param query          the search query string
     * @param user_latitude  the user's latitude
     * @param user_longitude the user's longitude
     * @param type           the type filter
     * @return the search response with results sorted by distance
     */
    public Mono<SearchResponse> searchByUserProximity(String query, Double user_latitude, Double user_longitude,
            String type) {
        log.info("Searching by user proximity: query={}, lat={}, lon={}, type={}", query, user_latitude, user_longitude,
                type);

        if (user_latitude == null || user_longitude == null) {
            log.warn("User location not provided, falling back to standard search");
            return search(query, type, null, null, null);
        }

        KeywordParser.ParsedQueryResult parsed = keywordParser.parseWithCity(query);
        String parsed_query = parsed.query;
        Double proximity_radius = parsed.proximity_radius != null ? parsed.proximity_radius : 10.0;

        log.info("Parsed query: {}", parsed_query);
        log.info("Is proximity search: {}, radius: {}km", parsed.is_proximity_search, proximity_radius);

        try {
            Query search_query = NativeQuery.builder()
                    .withQuery(q -> q.bool(b -> {
                        if (parsed_query != null && !parsed_query.isEmpty()) {
                            b.must(m -> m.multiMatch(mm -> mm
                                    .query(parsed_query)
                                    .fields("title^3", "description", "category", "city^2")
                                    .fuzziness("AUTO")
                                    .prefixLength(2)
                                    .operator(Operator.Or)));
                        } else {
                            b.must(m -> m.matchAll(ma -> ma));
                        }

                        if (type != null && !"all".equalsIgnoreCase(type)) {
                            final String filter_type = type.toLowerCase();
                            if (filter_type.equals("shop")) {
                                b.filter(f -> f.term(t -> t.field("serviceType").value("user")));
                            } else if (filter_type.equals("product") || filter_type.equals("service")
                                    || filter_type.equals("products") || filter_type.equals("services")) {
                                b.filter(f -> f.term(t -> t.field("serviceType").value("listing")));
                            } else {
                                b.filter(f -> f.term(t -> t.field("serviceType").value(filter_type)));
                            }
                        }

                        return b;
                    }))
                    .build();

            log.info("Executing proximity search with manual distance filtering and sorting");

            return elasticsearchOperations.search(search_query, ProductDocument.class)
                    .map(SearchHit::getContent)
                    .filter(doc -> {
                        if (doc.getLatitude() != null && doc.getLongitude() != null) {
                            double distance = calculateDistance(user_latitude, user_longitude,
                                    doc.getLatitude(), doc.getLongitude());
                            return distance <= proximity_radius;
                        }
                        return false;
                    })
                    .collectList()
                    .map(results -> {
                        results.sort((a, b) -> {
                            double dist_a = calculateDistance(user_latitude, user_longitude,
                                    a.getLatitude(), a.getLongitude());
                            double dist_b = calculateDistance(user_latitude, user_longitude,
                                    b.getLatitude(), b.getLongitude());
                            return Double.compare(dist_a, dist_b);
                        });

                        log.info("Found {} results within {}km", results.size(), proximity_radius);

                        List<SearchResponse.ProductDto> dtos = results.stream()
                                .map(doc -> toProductDto(doc, user_latitude, user_longitude))
                                .toList();

                        return SearchResponse.builder()
                                .success(true)
                                .query(query)
                                .total(dtos.size())
                                .results(dtos)
                                .build();
                    })
                    .defaultIfEmpty(SearchResponse.builder()
                            .success(true)
                            .query(query)
                            .total(0)
                            .results(List.of())
                            .build())
                    .onErrorResume(e -> {
                        log.error("Error executing proximity search", e);
                        return Mono.just(SearchResponse.builder()
                                .success(false)
                                .query(query)
                                .total(0)
                                .results(null)
                                .build());
                    });
        } catch (Exception e) {
            log.error("Error building proximity search query", e);
            return Mono.just(SearchResponse.builder()
                    .success(false)
                    .query(query)
                    .total(0)
                    .results(null)
                    .build());
        }
    }

    /**
     * Provides autocomplete suggestions based on partial query.
     *
     * @param query the partial search query
     * @return list of up to 5 title suggestions
     */
    public Mono<List<String>> autocomplete(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Mono.just(List.of());
        }

        String clean_query = query.trim();

        return searchRepository.findByTitleContainingOrDescriptionContaining(clean_query, clean_query)
                .map(ProductDocument::getTitle).distinct().take(5)
                .collectList();
    }

    /**
     * Searches products within a radius of a geocoded city.
     * Uses Elasticsearch geo_distance filter with geo_distance sorting.
     *
     * @param query     the search query string
     * @param city      the reference city name
     * @param radius_km the search radius in kilometers
     * @return the search response with proximity-sorted results
     */
    public Mono<SearchResponse> searchByProximity(String query, String city, Double radius_km) {
        log.info("Searching by proximity: query={}, city={}, radiusKm={}", query, city, radius_km);

        return geoService.geocode(city).flatMap(geo_location -> {
            log.info("Geocoded reference city: lat={}, lng={}", geo_location.getLatitude(),
                    geo_location.getLongitude());

            final Double radius = radius_km != null ? radius_km : 10.0;

            Query search_query = NativeQuery.builder()
                    .withQuery(q -> q.bool(b -> {
                        if (query != null && !query.isEmpty()) {
                            b.must(m -> m.multiMatch(mm -> mm
                                    .query(query)
                                    .fields("title^3", "description", "category")
                                    .fuzziness("AUTO")));
                        }

                        b.filter(f -> f.geoDistance(gd -> gd
                                .field("location")
                                .distance(radius + "km")
                                .location(gl -> gl.latlon(latlon -> latlon
                                        .lat(geo_location.getLatitude())
                                        .lon(geo_location.getLongitude())))));

                        return b;
                    }))
                    .withSort(s -> s.geoDistance(gd -> gd
                            .field("location")
                            .location(gl -> gl.latlon(latlon -> latlon
                                    .lat(geo_location.getLatitude())
                                    .lon(geo_location.getLongitude())))
                            .order(co.elastic.clients.elasticsearch._types.SortOrder.Asc)))
                    .build();

            return elasticsearchOperations.search(search_query, ProductDocument.class)
                    .map(SearchHit::getContent)
                    .map(doc -> toProductDto(doc, geo_location.getLatitude(), geo_location.getLongitude()))
                    .collectList()
                    .map(results -> SearchResponse.builder()
                            .success(true)
                            .query(query)
                            .total(results.size())
                            .results(results)
                            .build());
        }).switchIfEmpty(Mono.just(SearchResponse.builder().success(false).query(query).total(0).build()));
    }

    /**
     * Calculates the distance between two geographic points using Haversine
     * formula.
     *
     * @param lat1 latitude of the first point
     * @param lng1 longitude of the first point
     * @param lat2 latitude of the second point
     * @param lng2 longitude of the second point
     * @return the distance in kilometers
     */
    private Double calculateDistance(Double lat1, Double lng1, Double lat2, Double lng2) {
        final int R = 6371;
        double lat_distance = Math.toRadians(lat2 - lat1);
        double lon_distance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(lat_distance / 2) * Math.sin(lat_distance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(lon_distance / 2)
                        * Math.sin(lon_distance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Converts a ProductDocument to a ProductDto for the search response.
     *
     * @param doc      the product document from Elasticsearch
     * @param user_lat the user's latitude for distance calculation
     * @param user_lon the user's longitude for distance calculation
     * @return the product DTO with distance information
     */
    private SearchResponse.ProductDto toProductDto(ProductDocument doc, Double user_lat, Double user_lon) {
        Double distance = null;
        if (user_lat != null && user_lon != null && doc.getLatitude() != null && doc.getLongitude() != null) {
            distance = calculateDistance(user_lat, user_lon, doc.getLatitude(), doc.getLongitude());
        }

        String effective_type = doc.getType();
        if (effective_type == null || effective_type.isEmpty()) {
            String st = doc.getServiceType();
            if ("listing".equalsIgnoreCase(st) || "businessbook".equalsIgnoreCase(st)) {
                effective_type = "product";
            } else if ("user".equalsIgnoreCase(st)) {
                effective_type = "shop";
            } else {
                effective_type = "product";
            }
        }

        return SearchResponse.ProductDto.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .price(doc.getPrice())
                .serviceType(doc.getServiceType())
                .type(effective_type)
                .category(doc.getCategory())
                .city(doc.getCity())
                .quartier(doc.getQuartier())
                .rating(doc.getRating())
                .images(doc.getImages())
                .latitude(doc.getLatitude())
                .longitude(doc.getLongitude())
                .distanceKm(distance)
                .build();
    }
}