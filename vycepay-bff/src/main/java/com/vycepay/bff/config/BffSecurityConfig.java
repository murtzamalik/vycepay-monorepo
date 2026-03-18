package com.vycepay.bff.config;

import com.vycepay.common.security.JwtValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class BffSecurityConfig {

    @Bean
    public JwtValidator jwtValidator(@Value("${vycepay.jwt.secret}") String secret) {
        return new JwtValidator(secret);
    }

    @Bean
    public BffJwtFilter bffJwtFilter(JwtValidator jwtValidator) {
        return new BffJwtFilter(jwtValidator);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, BffJwtFilter bffJwtFilter) throws Exception {
        return http.csrf(cs -> cs.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(bffJwtFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/verify-otp").permitAll()
                        .requestMatchers("/api/v1/**").authenticated())
                .build();
    }
}
