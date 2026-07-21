package com.vycepay.callback.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight root response for uptime probes hitting {@code GET /}.
 * Choice Bank webhooks use {@code POST /api/v1/choice-bank/callback}.
 */
@RestController
public class ServiceInfoController {

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> root() {
        return ResponseEntity.ok(Map.of(
                "service", "vycepay-callback-service",
                "status", "up"));
    }
}
