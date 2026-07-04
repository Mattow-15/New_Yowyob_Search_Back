package com.yowyob.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "yowyob.search.ai")
public record AiProperties(
        boolean enabled,
        String groqApiKey,
        String groqModel,
        String geminiApiKey,
        String geminiModel) {

    public AiProperties {
        groqApiKey = groqApiKey == null ? "" : groqApiKey.trim();
        geminiApiKey = geminiApiKey == null ? "" : geminiApiKey.trim();
        if (groqModel == null || groqModel.isBlank()) {
            groqModel = "llama-3.3-70b-versatile";
        }
        if (geminiModel == null || geminiModel.isBlank()) {
            geminiModel = "gemini-2.0-flash";
        }
    }
}
