package com.vycepay.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides JwtValidator and JwtAuthFilter. Services wire the filter in their SecurityConfig.
 */
@Configuration
@ConditionalOnProperty(name = "vycepay.jwt.secret")
public class JwtSecurityConfig {

    @Bean
    public JwtValidator jwtValidator(@Value("${vycepay.jwt.secret}") String secret) {
        return new JwtValidator(secret);
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtValidator jwtValidator) {
        return new JwtAuthFilter(jwtValidator);
    }
}
