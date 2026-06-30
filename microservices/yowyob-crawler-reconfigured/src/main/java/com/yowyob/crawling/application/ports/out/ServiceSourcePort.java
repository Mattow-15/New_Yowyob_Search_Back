package com.yowyob.crawling.application.ports.out;

import com.yowyob.crawling.domain.model.RawService;
import com.yowyob.crawling.domain.model.ServiceSource;
import java.util.List;

public interface ServiceSourcePort {
    ServiceSource getSourceType();
    boolean isAvailable();
    List<RawService> fetch(String cityName, double lat, double lng, int radiusMeters, String placeType);
}
