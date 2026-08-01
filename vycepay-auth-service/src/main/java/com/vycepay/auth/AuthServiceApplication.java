package com.vycepay.auth;

import com.vycepay.auth.config.AuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * VycePay Auth Service — signup OTP, PIN login, device binding, JWT.
 * Scans common.exception as a belt-and-suspenders alongside VyceErrorAutoConfiguration.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.vycepay.auth", "com.vycepay.common.exception"})
@EnableConfigurationProperties(AuthProperties.class)
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
