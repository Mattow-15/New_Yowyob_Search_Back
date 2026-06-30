package com.yowyob.search.config;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
public class ElasticsearchConfig {

    @Bean
    public org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions elasticsearchCustomConversions() {
        return new org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions(
                java.util.Arrays.asList(new StringToGeoPointConverter()));
    }

    @org.springframework.data.convert.ReadingConverter
    static class StringToGeoPointConverter
            implements
            org.springframework.core.convert.converter.Converter<String, org.springframework.data.elasticsearch.core.geo.GeoPoint> {
        @Override
        public org.springframework.data.elasticsearch.core.geo.GeoPoint convert(String source) {
            if (source == null || source.isEmpty()) {
                return null;
            }
            String[] coords = source.split(",");
            if (coords.length != 2) {
                return null;
            }
            try {
                double lat = Double.parseDouble(coords[0].trim());
                double lon = Double.parseDouble(coords[1].trim());
                return new org.springframework.data.elasticsearch.core.geo.GeoPoint(lat, lon);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
