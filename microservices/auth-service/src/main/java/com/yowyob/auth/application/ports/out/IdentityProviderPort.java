package com.yowyob.auth.application.ports.out;

import com.yowyob.auth.domain.model.GoogleUserInfo;

public interface IdentityProviderPort {
    GoogleUserInfo verifyGoogleToken(String tokenString);
}
