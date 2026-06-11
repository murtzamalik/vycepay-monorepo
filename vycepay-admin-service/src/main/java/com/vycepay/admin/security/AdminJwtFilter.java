package com.vycepay.admin.security;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import com.vycepay.admin.application.service.AdminSessionService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Authenticates admin requests by validating JWT signature and revocable session state. */
@Component
public class AdminJwtFilter extends OncePerRequestFilter {
    private final AdminJwtService jwtService;
    private final AdminSessionService sessionService;
    public AdminJwtFilter(AdminJwtService jwtService, AdminSessionService sessionService) { this.jwtService = jwtService; this.sessionService = sessionService; }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims = jwtService.parse(token);
            if (claims != null && claims.getId() != null) {
                AdminPrincipal principal = sessionService.loadPrincipal(claims.getId()).orElse(null);
                if (principal != null) {
                    Set<SimpleGrantedAuthority> authorities = principal.permissions().stream().map(permission -> new SimpleGrantedAuthority("PERM_" + permission)).collect(Collectors.toSet());
                    principal.roles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, token, authorities));
                }
            }
        }
        filterChain.doFilter(request, response);
    }
    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) { return authorization.substring(7); }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("admin_token".equals(cookie.getName())) { return cookie.getValue(); }
            }
        }
        return null;
    }
}
