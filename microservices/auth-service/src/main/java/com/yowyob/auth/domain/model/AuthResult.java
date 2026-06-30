package com.yowyob.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResult {
    private User user;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String message;
    /** Position de l'utilisateur déduite de son IP au moment de la connexion (peut être null). */
    private GeoLocation location;
}
