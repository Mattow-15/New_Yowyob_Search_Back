package com.yowyob.auth.infrastructure.adapters.out.geo;

import com.yowyob.auth.domain.model.GeoLocation;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test d'intégration : appelle réellement ip-api.com.
 * (Nécessite un accès réseau ; sert à prouver que la géolocalisation IP fonctionne.)
 */
class IpApiGeoLocatorAdapterIT {

    private IpApiGeoLocatorAdapter newAdapter() {
        IpApiGeoLocatorAdapter adapter = new IpApiGeoLocatorAdapter(RestClient.builder());
        ReflectionTestUtils.setField(adapter, "ipapiUrl", "http://ip-api.com");
        return adapter;
    }

    @Test
    void resoutUneIpPubliqueConnue() {
        Optional<GeoLocation> location = newAdapter().locate("8.8.8.8");

        assertThat(location).isPresent();
        assertThat(location.get().getCountry()).isEqualTo("United States");
        assertThat(location.get().getCity()).isEqualTo("Ashburn");
        assertThat(location.get().getIp()).isEqualTo("8.8.8.8");
        assertThat(location.get().getLatitude()).isNotZero();
        assertThat(location.get().getLongitude()).isNotZero();
    }

    @Test
    void uneIpLocaleEstResolueVersLIpPubliqueDeLAppelant() {
        // Pour 127.0.0.1, on interroge ip-api sans IP → géoloc de l'appelant (le serveur).
        Optional<GeoLocation> location = newAdapter().locate("127.0.0.1");

        assertThat(location).isPresent();
        assertThat(location.get().getCity()).isNotBlank();
        assertThat(location.get().getCountry()).isNotBlank();
    }
}
