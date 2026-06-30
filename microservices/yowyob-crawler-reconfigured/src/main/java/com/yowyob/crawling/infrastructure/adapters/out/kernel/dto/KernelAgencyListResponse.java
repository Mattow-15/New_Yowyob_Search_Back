package com.yowyob.crawling.infrastructure.adapters.out.kernel.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Enveloppe standard des réponses du Kernel ({@code ApiResponseListAgencyResponse}) :
 * la liste des agences est sous {@code data}, jamais à la racine.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KernelAgencyListResponse(
        boolean success,
        List<KernelAgencyResponse> data,
        String message,
        String errorCode,
        String timestamp
) {
}
