package com.yowyob.search.service;

import co.elastic.clients.elasticsearch._types.GeoLocation;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.LatLonGeoLocation;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.yowyob.search.config.EmbeddingProperties;
import com.yowyob.search.domain.SearchDoc;
import com.yowyob.search.geo.GeoProperties;
import com.yowyob.search.geo.GeoService;
import com.yowyob.search.geo.IpGeolocationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Recherche hybride scopée au tenant : lexical (edge-ngram) + kNN sémantique + proximité géo.
 *
 * <p>La requête brute est analysée par {@link KeywordParser} (ville, intention de proximité). Un
 * centre géographique est résolu depuis les coordonnées explicites, une ville (géocodée) ou l'IP
 * (« près de moi ») ; il déclenche alors un filtre + tri par distance. Chaque brique est
 * optionnelle et dégrade proprement : sans embeddings → lexical seul ; sans géo → recherche globale.
 */
@Service
public class SearchService {

    private static final int MAX_SIZE = 100;
    private static final Set<String> PUBLIC_COLLECTIONS = Set.of(
            "organization", "agency", "product", "products", "places", "listing", "listings", "services");

    private final ReactiveElasticsearchOperations operations;
    private final EmbeddingClient embeddingClient;
    private final EmbeddingProperties embeddingProperties;
    private final KeywordParser keywordParser;
    private final GeoService geoService;
    private final IpGeolocationService ipGeolocationService;
    private final GeoProperties geoProperties;

    public SearchService(ReactiveElasticsearchOperations operations, EmbeddingClient embeddingClient,
            EmbeddingProperties embeddingProperties, KeywordParser keywordParser, GeoService geoService,
            IpGeolocationService ipGeolocationService, GeoProperties geoProperties) {
        this.operations = operations;
        this.embeddingClient = embeddingClient;
        this.embeddingProperties = embeddingProperties;
        this.keywordParser = keywordParser;
        this.geoService = geoService;
        this.ipGeolocationService = ipGeolocationService;
        this.geoProperties = geoProperties;
    }

    /** Recherche simple (sans contexte géo) — conservée pour compatibilité. */
    public Flux<SearchDoc> search(String tenantId, String query, String collection, int page, int size) {
        return search(SearchQuery.of(tenantId, query, collection, page, size));
    }

    /** Recherche complète (lexical + sémantique + proximité). */
    public Flux<SearchDoc> search(SearchQuery request) {
        if (request.collection() != null && !request.collection().isBlank()
                && !PUBLIC_COLLECTIONS.contains(request.collection().toLowerCase())) {
            return Flux.empty();
        }
        KeywordParser.ParsedQuery parsed = keywordParser.parse(request.rawQuery());
        String text = parsed.query();
        int page = Math.max(request.page(), 0);
        int size = request.size() <= 0 ? 20 : Math.min(request.size(), MAX_SIZE);
        double radiusKm = resolveRadius(request, parsed);

        return resolveCenter(request, parsed)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMapMany(centerOpt -> embeddingClient.embed(text)
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty())
                        .flatMapMany(vectorOpt -> {
                            NativeQuery query = buildQuery(request, text, centerOpt.orElse(null),
                                    vectorOpt.orElse(null), radiusKm, page, size);
                            return operations.search(query, SearchDoc.class)
                                    .map(SearchHit::getContent)
                                    .switchIfEmpty(kernelOrgFallback(request, centerOpt.orElse(null), size));
                        }));
    }

    /**
     * Fallback : si la recherche principale ne retourne aucun résultat, on propose
     * les organisations Kernel disponibles (pertinentes par proximité si géo disponible).
     */
    private Flux<SearchDoc> kernelOrgFallback(SearchQuery request, GeoPoint center, int size) {
        NativeQueryBuilder builder = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {
                    b.filter(f -> f.term(t -> t.field("tenantId").value(request.tenantId())));
                    b.filter(f -> f.term(t -> t.field("collection").value("organization")));
                    b.filter(f -> f.exists(e -> e.field("title")));
                    b.must(m -> m.matchAll(ma -> ma));
                    return b;
                }))
                .withPageable(PageRequest.of(0, size));
        if (center != null) {
            builder.withSort(s -> s.geoDistance(gd -> gd
                    .field("location")
                    .location(geoLocation(center))
                    .order(SortOrder.Asc)));
        }
        return operations.search(builder.build(), SearchDoc.class).map(SearchHit::getContent);
    }

    // -------------------------------------------------------------------------
    // Résolution du contexte géographique
    // -------------------------------------------------------------------------

    private Mono<GeoPoint> resolveCenter(SearchQuery request, KeywordParser.ParsedQuery parsed) {
        if (!geoProperties.enabled()) {
            return Mono.empty();
        }
        if (request.lat() != null && request.lon() != null) {
            return Mono.just(new GeoPoint(request.lat(), request.lon()));
        }
        String city = (request.city() != null && !request.city().isBlank()) ? request.city() : parsed.city();
        if (city != null && !city.isBlank()) {
            return geoService.geocode(city).map(r -> new GeoPoint(r.latitude(), r.longitude()));
        }
        if (parsed.proximity() && request.ipAddress() != null && !request.ipAddress().isBlank()) {
            return ipGeolocationService.getLocationFromIp(request.ipAddress())
                    .flatMap(loc -> (loc.latitude() != null && loc.longitude() != null)
                            ? Mono.just(new GeoPoint(loc.latitude(), loc.longitude()))
                            : Mono.empty());
        }
        return Mono.empty();
    }

    private double resolveRadius(SearchQuery request, KeywordParser.ParsedQuery parsed) {
        if (request.radiusKm() != null && request.radiusKm() > 0) {
            return request.radiusKm();
        }
        if (parsed.proximityRadiusKm() != null && parsed.proximityRadiusKm() > 0) {
            return parsed.proximityRadiusKm();
        }
        return geoProperties.defaultRadiusKm();
    }

    // -------------------------------------------------------------------------
    // Construction de la requête Elasticsearch
    // -------------------------------------------------------------------------

    private NativeQuery buildQuery(SearchQuery request, String text, GeoPoint center, float[] vector,
            double radiusKm, int page, int size) {
        String tenantId = request.tenantId();
        String collection = request.collection();
        boolean hasText = text != null && !text.isBlank();

        NativeQueryBuilder builder = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {
                    b.filter(f -> f.term(t -> t.field("tenantId").value(tenantId)));
                    if (collection != null && !collection.isBlank()) {
                        b.filter(f -> f.term(t -> t.field("collection").value(collection)));
                    } else {
                        b.filter(publicCollectionFilter());
                    }
                    // N'afficher que les docs qui ont un titre indexé (exclut les orgs vides)
                    b.filter(f -> f.exists(e -> e.field("title")));
                    if (center != null) {
                        b.filter(geoDistanceFilter(center, radiusKm));
                    }
                    if (hasText) {
                        // Le texte doit toujours matcher (que kNN soit actif ou non).
                        // kNN sert au reranking, pas à trouver des résultats hors-sujet.
                        b.must(m -> m.bool(inner -> {
                            inner.should(s -> s.match(mm -> mm.field("title").query(text).boost(4.0f)));
                            inner.should(s -> s.match(mm -> mm.field("content").query(text).boost(1.5f)));
                            inner.minimumShouldMatch("1");
                            return inner;
                        }));
                        // Les organisations Kernel pertinentes remontent en tête (boost additionnel)
                        b.should(s -> s.term(t -> t.field("collection").value("organization").boost(10.0f)));
                    } else {
                        b.must(m -> m.matchAll(ma -> ma));
                        b.should(s -> s.term(t -> t.field("collection").value("organization").boost(10.0f)));
                    }
                    return b;
                }))
                .withPageable(PageRequest.of(page, size));

        if (vector != null) {
            builder.withKnnSearches(KnnSearch.of(knn -> knn
                    .field("textVector")
                    .queryVector(toList(vector))
                    .k(embeddingProperties.k())
                    .numCandidates(embeddingProperties.numCandidates())
                    .boost(embeddingProperties.boost())
                    .filter(knnFilters(tenantId, collection, center, radiusKm))));
        }
        if (center != null) {
            builder.withSort(s -> s.geoDistance(gd -> gd
                    .field("location")
                    .location(geoLocation(center))
                    .order(SortOrder.Asc)));
        }
        return builder.build();
    }

    private static Query geoDistanceFilter(GeoPoint center, double radiusKm) {
        return Query.of(q -> q.geoDistance(gd -> gd
                .field("location")
                .distance(radiusKm + "km")
                .location(geoLocation(center))));
    }

    private static List<Query> knnFilters(String tenantId, String collection, GeoPoint center, double radiusKm) {
        List<Query> filters = new ArrayList<>();
        filters.add(Query.of(q -> q.term(t -> t.field("tenantId").value(tenantId))));
        if (collection != null && !collection.isBlank()) {
            filters.add(Query.of(q -> q.term(t -> t.field("collection").value(collection))));
        } else {
            filters.add(publicCollectionFilter());
        }
        if (center != null) {
            filters.add(geoDistanceFilter(center, radiusKm));
        }
        return filters;
    }

    private static Query publicCollectionFilter() {
        return Query.of(q -> q.bool(b -> {
            PUBLIC_COLLECTIONS.forEach(collection ->
                    b.should(s -> s.term(t -> t.field("collection").value(collection))));
            return b.minimumShouldMatch("1");
        }));
    }

    private static GeoLocation geoLocation(GeoPoint center) {
        return GeoLocation.of(g -> g.latlon(LatLonGeoLocation.of(ll -> ll.lat(center.getLat()).lon(center.getLon()))));
    }

    private static List<Float> toList(float[] vector) {
        List<Float> list = new ArrayList<>(vector.length);
        for (float value : vector) {
            list.add(value);
        }
        return list;
    }
}
