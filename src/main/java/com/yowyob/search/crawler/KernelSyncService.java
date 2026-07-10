package com.yowyob.search.crawler;

import com.yowyob.search.service.IndexService;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Synchronise les organisations du Kernel vers Elasticsearch.
 *
 * <p>Prérequis : le client {@code kernel.sync.client-id} doit avoir la permission
 * {@code READ_ORGANIZATIONS} dans le Kernel (à demander à devops).
 *
 * <p>Flux : {@code GET /api/organizations?page=N&size=S} (pagination) → normalisation → {@link
 * IndexService#index}.
 */
@Service
@ConditionalOnProperty(prefix = "kernel.sync", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(KernelSyncProperties.class)
public class KernelSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KernelSyncService.class);
    private static final String COLLECTION = "organization";
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final KernelSyncProperties props;
    private final IndexService indexService;
    private final WebClient webClient;

    public KernelSyncService(KernelSyncProperties props, IndexService indexService,
            WebClient.Builder builder) {
        this.props = props;
        this.indexService = indexService;
        this.webClient = builder.build();
    }

    /** Lance la synchronisation complète et renvoie le nombre d'organisations indexées. */
    public Mono<Long> sync() {
        if (props.targetTenantId() == null || props.targetTenantId().isBlank()) {
            return Mono.error(new IllegalStateException("kernel.sync.target-tenant-id requis."));
        }
        LOGGER.info("Sync Kernel → Elasticsearch démarré (pageSize={})", props.pageSize());

        return fetchAllPages()
                .flatMapSequential(this::indexOrg)
                .reduce(0L, Long::sum)
                .doOnSuccess(total -> LOGGER.info("Sync Kernel terminé : {} organisation(s) indexée(s).", total))
                .doOnError(err -> LOGGER.error("Sync Kernel en échec : {}", err.getMessage()));
    }

    // ── Pagination ────────────────────────────────────────────────────────────

    private Flux<Map<String, Object>> fetchAllPages() {
        return Flux.range(0, Integer.MAX_VALUE)
                .concatMap(page -> fetchPage(page))
                .takeWhile(page -> !page.isEmpty())
                .flatMap(page -> Flux.fromIterable(page));
    }

    @SuppressWarnings("unchecked")
    private Mono<List<Map<String, Object>>> fetchPage(int page) {
        String url = props.kernelBaseUrl() + "/api/organizations?page=" + page + "&size=" + props.pageSize();
        return webClient.get()
                .uri(url)
                .header("X-Client-Id", props.clientId())
                .header("X-Api-Key", props.apiKey())
                .header("X-Tenant-Id", props.kernelTenantId())
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(MAP_TYPE)
                .map(response -> extractOrgs(response))
                .onErrorResume(err -> {
                    LOGGER.error("Erreur fetch Kernel page {} : {}", page, err.getMessage());
                    return Mono.just(List.of()); // arrête la pagination
                });
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> extractOrgs(Map<String, Object> response) {
        // Format { data: [...], totalPages: N }
        if (response.containsKey("data") && response.get("data") instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        // Format Spring Page { content: [...], last: bool }
        if (response.containsKey("content") && response.get("content") instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        // Format { results: [...] }
        if (response.containsKey("results") && response.get("results") instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        LOGGER.warn("Format de réponse Kernel inattendu, clés : {}", response.keySet());
        return List.of();
    }

    // ── Indexation ────────────────────────────────────────────────────────────

    private Mono<Long> indexOrg(Map<String, Object> org) {
        String id = extractId(org);
        if (id == null || id.isBlank()) {
            LOGGER.debug("Organisation ignorée : pas d'id (clés={})", org.keySet());
            return Mono.just(0L);
        }
        String name = extractName(org);
        if (name == null || name.isBlank()) {
            LOGGER.debug("Organisation {} ignorée : pas de nom", id);
            return Mono.just(0L);
        }

        Map<String, Object> doc = toDocument(id, name, org);
        return indexService.index(props.targetTenantId(), COLLECTION, id, doc)
                .map(saved -> 1L)
                .doOnSuccess(v -> LOGGER.debug("Org indexée : {} ({})", name, id))
                .onErrorResume(err -> {
                    LOGGER.warn("Échec indexation org {} ({}) : {}", id, name, err.getMessage());
                    return Mono.just(0L);
                });
    }

    private static Map<String, Object> toDocument(String id, String name, Map<String, Object> org) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id",          id);
        doc.put("name",        name);
        doc.put("description", org.get("description"));
        doc.put("category",    org.get("category"));
        doc.put("email",       org.get("email"));
        doc.put("website",     org.get("website"));
        doc.put("phone",       firstNonNull(org.get("phoneNumber"), org.get("phone")));
        doc.put("collection",  COLLECTION);
        doc.put("source",      "KERNEL_ORG");
        doc.put("indexedAt",   Instant.now().toString());

        // Adresse (champ imbriqué ou à plat)
        Object addressObj = org.get("address");
        if (addressObj instanceof Map<?, ?> addr) {
            doc.put("city",      addr.get("city"));
            doc.put("quartier",  addr.get("quartier"));
            Object lat = firstNonNull(addr.get("latitude"), addr.get("lat"));
            Object lon = firstNonNull(addr.get("longitude"), addr.get("lon"), addr.get("lng"));
            if (lat != null && lon != null) {
                Map<String, Object> location = new HashMap<>();
                location.put("lat", lat);
                location.put("lon", lon);
                doc.put("location", location);
            }
        } else {
            // Champs à plat
            doc.put("city",     org.get("city"));
            doc.put("quartier", org.get("quartier"));
            Object lat = firstNonNull(org.get("latitude"), org.get("lat"));
            Object lon = firstNonNull(org.get("longitude"), org.get("lon"), org.get("lng"));
            if (lat != null && lon != null) {
                Map<String, Object> location = new HashMap<>();
                location.put("lat", lat);
                location.put("lon", lon);
                doc.put("location", location);
            }
        }

        doc.values().removeIf(v -> v == null);
        return doc;
    }

    private static String extractId(Map<String, Object> org) {
        for (String key : new String[]{"id", "organizationId", "orgId", "uuid"}) {
            Object v = org.get(key);
            if (v != null) return String.valueOf(v);
        }
        return null;
    }

    private static String extractName(Map<String, Object> org) {
        for (String key : new String[]{"title", "name", "displayName"}) {
            Object v = org.get(key);
            if (v instanceof String s && !s.isBlank()) return s;
        }
        return null;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T v : values) {
            if (v != null) return v;
        }
        return null;
    }
}
