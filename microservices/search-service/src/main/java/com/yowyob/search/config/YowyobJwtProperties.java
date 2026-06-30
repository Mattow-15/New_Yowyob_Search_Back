package com.yowyob.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Auth YowYob locale (repli quand le Kernel est indisponible). */
@ConfigurationProperties(prefix = "yowyob.jwt")
public record YowyobJwtProperties(String secret) {}
