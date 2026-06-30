package com.yowyob.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync; // Added this import

/**
 * Auth Service Application
 * 
 * @author Matteo Owona, Rouchda Yampen
 * @date 2024-12-18
 */
@SpringBootApplication
@EnableAsync // Changed from fully qualified to simple annotation
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
