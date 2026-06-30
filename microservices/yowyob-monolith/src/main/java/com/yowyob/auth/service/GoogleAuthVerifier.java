/**
 * Service for verifying Google OAuth ID tokens.
 * Validates tokens against Google's API and returns the user payload.
 * @author Matteo Owona, Rouchda Yampen
 * @date 2024-01-14
 * @updated 2025-02-11
 */
package com.yowyob.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
@Slf4j
public class GoogleAuthVerifier {

    @org.springframework.beans.factory.annotation.Value("${google.client-id}")
    private String clientId;

    /**
     * Verifies a Google ID token and returns the payload containing user info.
     *
     * @param token_string the Google ID token string to verify
     * @return the decoded token payload with user claims
     * @throws GeneralSecurityException if there is a security issue
     * @throws IOException              if there is a network issue
     * @throws IllegalArgumentException if the token is invalid
     */
    public GoogleIdToken.Payload verify(String token_string) throws GeneralSecurityException, IOException {
        String actual_client_id = (clientId != null && !clientId.isEmpty()) ? clientId
                : "763004243989-g1fftlketknf2ip32f0fsi39ukcqqkq3.apps.googleusercontent.com";
        log.info("Verifying Google Token with Client ID: '{}'", actual_client_id);

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
                    new GsonFactory())
                    .setAudience(Collections.singletonList(actual_client_id))
                    .build();

            GoogleIdToken id_token = verifier.verify(token_string);
            if (id_token != null) {
                return id_token.getPayload();
            } else {
                throw new IllegalArgumentException("Invalid ID token (verify returned null).");
            }
        } catch (Exception e) {
            log.error("Error in GoogleAuthVerifier: ", e);
            throw e;
        }
    }
}
