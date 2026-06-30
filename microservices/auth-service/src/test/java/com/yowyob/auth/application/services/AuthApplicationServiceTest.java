package com.yowyob.auth.application.services;

import com.yowyob.auth.application.ports.out.*;
import com.yowyob.auth.domain.model.AuthResult;
import com.yowyob.auth.domain.model.GeoLocation;
import com.yowyob.auth.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceTest {

    @Mock UserRepositoryPort userRepositoryPort;
    @Mock PasswordEncoderPort passwordEncoderPort;
    @Mock TokenServicePort tokenServicePort;
    @Mock IdentityProviderPort identityProviderPort;
    @Mock UserEventPublisherPort userEventPublisherPort;
    @Mock GeoLocatorPort geoLocatorPort;

    AuthApplicationService service;

    private final User user = User.builder()
            .id(UUID.randomUUID())
            .name("Test User")
            .email("test@yowyob.com")
            .password("hashed")
            .role(User.Role.USER)
            .status(User.Status.ACTIVE)
            .build();

    @BeforeEach
    void setUp() {
        service = new AuthApplicationService(
                userRepositoryPort, passwordEncoderPort, tokenServicePort,
                identityProviderPort, userEventPublisherPort, geoLocatorPort);

        when(userRepositoryPort.findByEmail("test@yowyob.com")).thenReturn(Optional.of(user));
        when(passwordEncoderPort.matches(eq("secret"), any())).thenReturn(true);
        when(tokenServicePort.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(tokenServicePort.generateRefreshToken(any())).thenReturn("refresh-token");
        when(tokenServicePort.getExpirationTime()).thenReturn(3600L);
    }

    @Test
    void loginAttacheLaPositionDeLIp() {
        when(geoLocatorPort.locate("8.8.8.8")).thenReturn(Optional.of(
                GeoLocation.builder().ip("8.8.8.8").city("Ashburn").country("United States")
                        .latitude(39.03).longitude(-77.5).build()));

        AuthResult result = service.login("test@yowyob.com", "secret", "8.8.8.8");

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getLocation()).isNotNull();
        assertThat(result.getLocation().getCity()).isEqualTo("Ashburn");
        assertThat(result.getLocation().getCountry()).isEqualTo("United States");
    }

    @Test
    void loginReussitMemeSansPositionDisponible() {
        // La géoloc échoue/indisponible : la connexion doit quand même aboutir.
        when(geoLocatorPort.locate(any())).thenReturn(Optional.empty());

        AuthResult result = service.login("test@yowyob.com", "secret", "127.0.0.1");

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getLocation()).isNull();
    }

    @Test
    void loginNEchouePasSiLaGeolocLeveUneException() {
        when(geoLocatorPort.locate(any())).thenThrow(new RuntimeException("ip-api down"));

        AuthResult result = service.login("test@yowyob.com", "secret", "1.2.3.4");

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getLocation()).isNull();
    }
}
