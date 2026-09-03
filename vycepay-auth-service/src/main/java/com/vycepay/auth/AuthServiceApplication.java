package com.vycepay.auth;

import com.vycepay.auth.config.AuthProperties;
import com.vycepay.common.config.SmsClientConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * VycePay Auth Service — signup OTP, PIN login, device binding, JWT.
 * Scans common.exception as a belt-and-suspenders alongside VyceErrorAutoConfiguration.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.vycepay.auth", "com.vycepay.common.exception"})
@EnableConfigurationProperties(AuthProperties.class)
@Import(SmsClientConfig.class)
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
