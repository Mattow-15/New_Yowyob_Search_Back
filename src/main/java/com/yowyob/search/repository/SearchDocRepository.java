package com.yowyob.search.repository;

import com.yowyob.search.domain.SearchDoc;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;

public interface SearchDocRepository extends ReactiveElasticsearchRepository<SearchDoc, String> {
}
