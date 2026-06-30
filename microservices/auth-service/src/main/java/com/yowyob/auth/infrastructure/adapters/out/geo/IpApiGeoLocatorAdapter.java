package com.yowyob.auth.infrastructure.adapters.out.geo;

import com.yowyob.auth.application.ports.out.GeoLocatorPort;
import com.yowyob.auth.domain.model.GeoLocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;

/**
 * Adaptateur de géolocalisation IP basé sur le service public ip-api.com.
 *
 * <p>Comportement :
 * <ul>
 *   <li>IP publique → {@code /json/{ip}} géolocalise cette IP.</li>
 *   <li>IP locale/privée (dev, 127.0.0.1, 192.168.x, ::1…) → {@code /json/} sans IP :
 *       ip-api géolocalise alors l'IP publique de l'appelant (le serveur), ce qui
 *       reste exploitable en local pour tester.</li>
 * </ul>
 * La résolution est best-effort : toute erreur renvoie {@link Optional#empty()}
 * pour ne jamais bloquer la connexion.
 */
@Component
@Slf4j
public class IpApiGeoLocatorAdapter implements GeoLocatorPort {

    private final RestClient restClient;

    @Value("${app.ipapi.url:http://ip-api.com}")
    private String ipapiUrl;

    public IpApiGeoLocatorAdapter(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public Optional<GeoLocation> locate(String ipAddress) {
        try {
            boolean localOrPrivate = isLocalOrPrivate(ipAddress);
            String uri = localOrPrivate ? ipapiUrl + "/json/" : ipapiUrl + "/json/" + ipAddress;

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(uri)
                    .header("User-Agent", "YowYob-Auth-Service")
                    .retrieve()
                    .body(Map.class);

            if (response == null || !"success".equals(response.get("status"))) {
                log.warn("Géolocalisation IP indisponible pour '{}' (réponse: {})",
                        ipAddress, response != null ? response.get("status") : "null");
                return Optional.empty();
            }

            GeoLocation location = GeoLocation.builder()
                    .ip(asString(response.get("query")))
                    .city(asString(response.get("city")))
                    .country(asString(response.get("country")))
                    .latitude(asDouble(response.get("lat")))
                    .longitude(asDouble(response.get("lon")))
                    .build();

            log.info("IP {} géolocalisée → {}, {} ({}, {})", location.getIp(),
                    location.getCity(), location.getCountry(),
                    location.getLatitude(), location.getLongitude());
            return Optional.of(location);

        } catch (Exception e) {
            log.error("Échec géolocalisation IP '{}': {}", ipAddress, e.getMessage());
            return Optional.empty();
        }
    }

    private boolean isLocalOrPrivate(String ip) {
        if (ip == null || ip.isBlank()) return true;
        return ip.equals("127.0.0.1")
                || ip.equals("::1")
                || ip.startsWith("0:0:0:0")
                || ip.startsWith("10.")
                || ip.startsWith("192.168.")
                || ip.matches("^172\\.(1[6-9]|2[0-9]|3[01])\\..*");
    }

    private String asString(Object o) {
        return o != null ? o.toString() : null;
    }

    private double asDouble(Object o) {
        return o != null ? Double.parseDouble(o.toString()) : 0.0;
    }
}
