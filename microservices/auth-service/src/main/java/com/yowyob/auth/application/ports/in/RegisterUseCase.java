package com.yowyob.auth.application.ports.in;

import com.yowyob.auth.domain.model.AuthResult;

public interface RegisterUseCase {
    AuthResult register(String name, String email, String password, String ipAddress);
}
