package com.yowyob.crawling.infrastructure.adapters.out.businessbook;

import com.yowyob.crawling.infrastructure.adapters.out.businessbook.dto.StandardizedDocumentDto;
import com.yowyob.crawling.infrastructure.adapters.out.businessbook.dto.StandardizedResponse;
import com.yowyob.crawling.infrastructure.config.BusinessBookProperties;
import com.yowyob.crawling.domain.model.StandardizedDocument;
import com.yowyob.crawling.application.ports.out.StandardizedDocumentSourcePort;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

/**
 * Adaptateur GÉNÉRIQUE du contrat de pull standardisé :
 * GET {baseUrl}/api/{serviceName}/search/documents?since=&page=&size=
 * avec Authorization: Bearer <JWT s2s>. Bloquant (le crawler est un batch).
 */
@Component
public class StandardizedPullAdapter implements StandardizedDocumentSourcePort {

    private final RestClient client;
    private final ServiceTokenProvider tokenProvider;

    public StandardizedPullAdapter(BusinessBookProperties props, ServiceTokenProvider tokenProvider) {
        this.client = RestClient.builder().baseUrl(props.api().baseUrl()).build();
        this.tokenProvider = tokenProvider;
    }

    @Override
    public List<StandardizedDocument> fetch(String serviceName, Instant since, int page, int size) {
        StandardizedResponse resp = client.get()
                .uri(b -> {
                    b.path("/api/{service}/search/documents")
                     .queryParam("page", page)
                     .queryParam("size", size);
                    if (since != null) b.queryParam("since", since.toString());
                    return b.build(serviceName);
                })
                .headers(h -> h.setBearerAuth(tokenProvider.getToken()))
                .retrieve()
                .body(StandardizedResponse.class);

        if (resp == null || resp.documents() == null) return List.of();
        return resp.documents().stream().map(StandardizedDocumentDto::toDomain).toList();
    }
}
