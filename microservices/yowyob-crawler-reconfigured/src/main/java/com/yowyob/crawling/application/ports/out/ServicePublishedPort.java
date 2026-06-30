package com.yowyob.crawling.application.ports.out;

import com.yowyob.crawling.domain.model.SearchableService;

public interface ServicePublishedPort {
    void publish(SearchableService service);
}
