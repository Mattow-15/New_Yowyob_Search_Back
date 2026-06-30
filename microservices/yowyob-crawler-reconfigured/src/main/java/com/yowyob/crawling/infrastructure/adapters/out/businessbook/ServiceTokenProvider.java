package com.yowyob.crawling.infrastructure.adapters.out.businessbook;

/** Fournit le JWT service-to-service exigé par le contrat. */
public interface ServiceTokenProvider {
    String getToken();
}
