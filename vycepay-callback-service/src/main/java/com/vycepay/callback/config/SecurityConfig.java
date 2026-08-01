package com.vycepay.callback.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Choice Bank webhooks stay public (signature verified in service).
 * Mobile notification routes require {@code X-Customer-Id} (injected by BFF after JWT).
 * Internal routes are gated by {@link InternalApiKeyFilter}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> customerIdHeaderFilter() {
        FilterRegistrationBean<OncePerRequestFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    jakarta.servlet.http.HttpServletRequest request,
                    jakarta.servlet.http.HttpServletResponse response,
                    jakarta.servlet.FilterChain filterChain)
                    throws jakarta.servlet.ServletException, java.io.IOException {
                String path = request.getRequestURI();
                if (path != null && path.startsWith("/api/v1/notifications")) {
                    String customerId = request.getHeader("X-Customer-Id");
                    if (customerId == null || customerId.isBlank()) {
                        response.setStatus(401);
                        response.setContentType("application/json");
                        response.getWriter().write(
                                "{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"Missing X-Customer-Id\"}");
                        return;
                    }
                }
                filterChain.doFilter(request, response);
            }
        });
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        bean.addUrlPatterns("/api/v1/notifications", "/api/v1/notifications/*");
        return bean;
    }

    @Bean
    public FilterRegistrationBean<InternalApiKeyFilter> internalApiKeyFilterRegistration(
            @Value("${vycepay.internal.api-key:}") String apiKey) {
        FilterRegistrationBean<InternalApiKeyFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new InternalApiKeyFilter(apiKey));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        bean.addUrlPatterns("/internal/*");
        return bean;
    }
}
