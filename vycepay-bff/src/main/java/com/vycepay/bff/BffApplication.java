package com.vycepay.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * VycePay BFF - single entry point for mobile. Validates JWT and proxies to backend services.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class BffApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffApplication.class, args);
    }
}
