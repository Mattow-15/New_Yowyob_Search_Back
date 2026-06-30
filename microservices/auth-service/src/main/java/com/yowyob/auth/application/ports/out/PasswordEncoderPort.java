package com.yowyob.auth.application.ports.out;

public interface PasswordEncoderPort {
    String encode(String rawPassword);
    boolean matches(String rawPassword, String encodedPassword);
}
