package com.yowyob.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.search.domain.SearchDoc;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class AiSearchServiceTest {

    @Test
    void returnsGeneratedAnswerWithOnlyIndexedSources() {
        SearchService searchService = mock(SearchService.class);
        LlmClient llmClient = mock(LlmClient.class);
        SearchDoc document = new SearchDoc("t:places:p1", "t", "places", "p1", "Pharmacie Centrale",
                "pharmacie douala", Map.of("city", "Douala", "category", "pharmacy"), null,
                new GeoPoint(4.05, 9.76), Instant.now());
        when(searchService.search(any(SearchQuery.class))).thenReturn(Flux.just(document));
        when(llmClient.generate(any(String.class))).thenReturn(Mono.just("La Pharmacie Centrale est disponible."));

        AiSearchService service = new AiSearchService(searchService, llmClient, new ObjectMapper());
        StepVerifier.create(service.answer(SearchQuery.of("t", "pharmacie", null, 0, 10)))
                .assertNext(result -> {
                    assertThat(result.aiAnswer()).contains("Pharmacie Centrale");
                    assertThat(result.sources()).singleElement().satisfies(source -> {
                        assertThat(source).containsEntry("id", "p1");
                        assertThat(source).containsEntry("city", "Douala");
                    });
                })
                .verifyComplete();
    }

    @Test
    void doesNotCallLlmWhenIndexHasNoResult() {
        SearchService searchService = mock(SearchService.class);
        LlmClient llmClient = mock(LlmClient.class);
        when(searchService.search(any(SearchQuery.class))).thenReturn(Flux.empty());

        AiSearchService service = new AiSearchService(searchService, llmClient, new ObjectMapper());
        StepVerifier.create(service.answer(SearchQuery.of("t", "introuvable", null, 0, 10)))
                .assertNext(result -> assertThat(result.sources()).isEmpty())
                .verifyComplete();
        verifyNoInteractions(llmClient);
    }
}
