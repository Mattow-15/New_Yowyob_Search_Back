package com.yowyob.crawling.application.ports.out;

import com.yowyob.crawling.domain.model.RawService;
import java.util.Optional;

public interface PhotoProviderPort {
    Optional<String> findPhotoUrl(RawService service);
}
