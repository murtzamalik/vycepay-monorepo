package com.vycepay.kyc.config;

import com.vycepay.common.security.JsonAccessDeniedHandler;
import com.vycepay.common.security.JsonAuthenticationEntryPoint;
import com.vycepay.common.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security config. When JWT configured: JWT or X-Customer-Id required. Otherwise permit all.
 * PIN hashing uses BCrypt strength 12 (same as auth-service).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           @Autowired(required = false) JwtAuthFilter jwtAuthFilter,
                                           JsonAuthenticationEntryPoint authenticationEntryPoint,
                                           JsonAccessDeniedHandler accessDeniedHandler) throws Exception {
        var chain = http.csrf(cs -> cs.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        if (jwtAuthFilter != null) {
            chain.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                    .authorizeHttpRequests(a -> a
                            .requestMatchers("/actuator/health", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                            .requestMatchers("/internal/choice-bank/**").permitAll()
                            .requestMatchers("/api/v1/**").authenticated());
        } else {
            chain.authorizeHttpRequests(a -> a.anyRequest().permitAll());
        }
        return chain.build();
    }
}
