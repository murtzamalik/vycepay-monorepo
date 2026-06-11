package com.vycepay.admin.config;

import java.util.Arrays;

import com.vycepay.admin.security.AdminJwtFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Security boundary for admin APIs. Customer JWTs are never accepted here. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(AdminProperties.class)
public class AdminSecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AdminJwtFilter adminJwtFilter, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/v1/auth/login", "/api/admin/v1/auth/login/mfa",
                                "/api/admin/v1/auth/forgot-password", "/api/admin/v1/auth/reset-password",
                                "/actuator/health", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(adminJwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AdminProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(properties.getCors().getAllowedOrigins().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, "X-Request-Id"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
