package com.yowyob.auth.application.ports.in;

import com.yowyob.auth.domain.model.AuthResult;

import java.util.UUID;

public interface AuthenticateUseCase {
    AuthResult login(String email, String password, String ipAddress);
    AuthResult googleLogin(String googleToken, String ipAddress);
    void changePassword(UUID userId, String currentPassword, String newPassword);
}
