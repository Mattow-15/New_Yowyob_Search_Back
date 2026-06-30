package com.yowyob.crawling.infrastructure.adapters.out.sokomboa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.crawling.domain.model.GeoPoint;
import com.yowyob.crawling.domain.model.RawService;
import com.yowyob.crawling.domain.model.ServiceSource;
import com.yowyob.crawling.application.ports.out.ServiceSourcePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SokoMboaScraper implements ServiceSourcePort {

    private final ObjectMapper objectMapper;

    private static final String BASE_URL = "https://sokomboa.com";
    private static final int DELAY_MS = 1500;

    @Override
    public ServiceSource getSourceType() {
        return ServiceSource.SOKOMBOA;
    }

    @Override
    public boolean isAvailable() {
        return true; // Web Scraping public, toujours dispo
    }

    @Override
    public List<RawService> fetch(String cityName, double lat, double lng, int radiusMeters, String placeType) {
        log.info("SokoMboa scraping pour la ville : {} et catégorie : {}", cityName, placeType);
        
        // Capitalisation (ex: "restaurant" -> "Restaurant")
        String formattedCategory = placeType.substring(0, 1).toUpperCase() + placeType.substring(1).toLowerCase();

        List<String> profileUrls = getProfileUrls(formattedCategory);
        List<RawService> results = new ArrayList<>();

        for (String url : profileUrls) {
            RawService raw = scrapeProfile(url, formattedCategory, cityName);
            if (raw != null) {
                // Filtrage géographique basique (ville correspondante)
                if (cityName == null || cityName.isBlank() 
                        || (raw.getCity() != null && raw.getCity().equalsIgnoreCase(cityName))) {
                    results.add(raw);
                }
            }
            sleep(DELAY_MS);
        }

        return results;
    }

    private List<String> getProfileUrls(String category) {
        List<String> urls = new ArrayList<>();
        int page = 1;

        while (true) {
            String url = BASE_URL + "/search/pro?q=" + category + "&page=" + page;

            try {
                Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36")
                    .timeout(15000)
                    .get();

                Elements links = doc.select("a[href*='/pro/']");
                if (links.isEmpty()) break;

                for (Element link : links) {
                    String href = link.attr("href");
                    if (href.startsWith("/pro/") && !urls.contains(BASE_URL + href)) {
                        urls.add(BASE_URL + href);
                    }
                }

                Element nextPage = doc.selectFirst("a[rel=next], .pagination .next");
                if (nextPage == null) break;

                page++;
                sleep(DELAY_MS);

            } catch (Exception e) {
                log.warn("Erreur page {} catégorie {} : {}", page, category, e.getMessage());
                break;
            }
        }

        return urls;
    }

    private RawService scrapeProfile(String url, String category, String city) {
        try {
            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36")
                .timeout(15000)
                .get();

            Element jsonLdScript = doc.selectFirst("script[type='application/ld+json']");
            if (jsonLdScript != null) {
                String jsonText = jsonLdScript.html().trim();
                return parseJsonLd(jsonText, url, category);
            }

            return extractFromHtml(doc, url, category, city);

        } catch (Exception e) {
            log.warn("Erreur scraping profil {} : {}", url, e.getMessage());
            return null;
        }
    }

    private RawService parseJsonLd(String json, String url, String category) {
        try {
            JsonNode root = objectMapper.readTree(json);

            String name = getJsonText(root, "name");
            String phone = getJsonText(root, "telephone");
            String description = getJsonText(root, "description");

            String street = null;
            String city = null;
            JsonNode address = root.get("address");
            if (address != null) {
                street = getJsonText(address, "streetAddress");
                city = getJsonText(address, "addressLocality");
            }

            Double lat = null;
            Double lng = null;
            JsonNode geo = root.get("geo");
            if (geo != null) {
                String latStr = getJsonText(geo, "latitude");
                String lngStr = getJsonText(geo, "longitude");
                if (latStr != null) lat = Double.parseDouble(latStr);
                if (lngStr != null) lng = Double.parseDouble(lngStr);
            }

            return RawService.builder()
                .rawId("sokomboa_" + sanitizeId(name))
                .name(name)
                .address(street != null ? street + ", " + city : city)
                .street(street)
                .city(city)
                .location(lat != null && lng != null ? new GeoPoint(lat, lng) : new GeoPoint(0.0, 0.0))
                .phone(phone)
                .category(category)
                .reviewsSummary(description)
                .source(ServiceSource.SOKOMBOA)
                .build();

        } catch (Exception e) {
            log.warn("Erreur parsing JSON-LD : {}", e.getMessage());
            return null;
        }
    }

    private RawService extractFromHtml(Document doc, String url, String category, String defaultCity) {
        String name = doc.selectFirst("h1") != null ? doc.selectFirst("h1").text() : null;

        String phone = null;
        Element phoneEl = doc.selectFirst("a[href^='tel:']");
        if (phoneEl != null) {
            phone = phoneEl.attr("href").replace("tel:", "");
        }

        String city = defaultCity;
        Element cityEl = doc.selectFirst("[class*='city'], [class*='ville'], [class*='location']");
        if (cityEl != null) city = cityEl.text();

        return RawService.builder()
            .rawId("sokomboa_" + sanitizeId(name))
            .name(name)
            .address(city)
            .city(city)
            .location(new GeoPoint(0.0, 0.0))
            .phone(phone)
            .category(category)
            .source(ServiceSource.SOKOMBOA)
            .build();
    }

    private String sanitizeId(String name) {
        if (name == null) return "unknown_" + System.currentTimeMillis();
        return name.toLowerCase()
            .replaceAll("[^a-z0-9]", "_")
            .replaceAll("_+", "_")
            .substring(0, Math.min(name.length(), 50));
    }

    private String getJsonText(JsonNode node, String field) {
        JsonNode val = node.get(field);
        return (val != null && !val.isNull()) ? val.asText() : null;
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
