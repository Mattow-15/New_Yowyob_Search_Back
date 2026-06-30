package com.yowyob.auth.infrastructure.adapters.out.identity;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.yowyob.auth.application.ports.out.IdentityProviderPort;
import com.yowyob.auth.domain.model.GoogleUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@Slf4j
public class GoogleAuthAdapter implements IdentityProviderPort {

    @Value("${google.client-id}")
    private String clientId;

    @Override
    public GoogleUserInfo verifyGoogleToken(String tokenString) {
        String actualClientId = (clientId != null && !clientId.isEmpty()) ? clientId
                : "763004243989-g1fftlketknf2ip32f0fsi39ukcqqkq3.apps.googleusercontent.com";
        log.info("Verifying Google Token with Client ID: '{}'", actualClientId);

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
                    new GsonFactory())
                    .setAudience(Collections.singletonList(actualClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(tokenString);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                return GoogleUserInfo.builder()
                        .email(payload.getEmail())
                        .name((String) payload.get("name"))
                        .build();
            } else {
                throw new IllegalArgumentException("Invalid ID token (verify returned null).");
            }
        } catch (Exception e) {
            log.error("Error in GoogleAuthAdapter: ", e);
            throw new RuntimeException("Google token verification failed: " + e.getMessage(), e);
        }
    }
}
