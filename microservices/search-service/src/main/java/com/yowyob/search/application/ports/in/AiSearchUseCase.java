package com.yowyob.search.application.ports.in;

import com.yowyob.search.domain.model.AiSearchQueryResult;
import com.yowyob.search.domain.model.FaqQueryResult;
import reactor.core.publisher.Mono;

public interface AiSearchUseCase {
    Mono<AiSearchQueryResult> answer(String query, String city);
    Mono<AiSearchQueryResult> answerAiMode(String query, String city);
    Mono<FaqQueryResult> generateFaq(String query, String city);
}
