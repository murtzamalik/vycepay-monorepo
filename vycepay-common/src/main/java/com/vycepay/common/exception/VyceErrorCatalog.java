package com.vycepay.common.exception;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads customer-facing error messages from classpath {@code vycepay-error-catalog.yaml}.
 * Source of truth for API {@code ErrorResponse.message}.
 */
@Component
public class VyceErrorCatalog {

    private static final Logger log = LoggerFactory.getLogger(VyceErrorCatalog.class);

    public static final String DEFAULT_USER_MESSAGE =
            "We couldn't complete this right now. Please try again. If it continues, contact support with your request ID.";

    private final Map<String, VyceErrorEntry> entries = new HashMap<>();
    private VyceErrorEntry defaultEntry = new VyceErrorEntry("INTERNAL_ERROR", 500, DEFAULT_USER_MESSAGE, false);

    @PostConstruct
    public void initialize() {
        try {
            loadFromClasspath();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load vycepay-error-catalog.yaml", e);
        }
    }

    /**
     * Reloads catalog from classpath (tests).
     */
    public void loadFromClasspath() throws IOException {
        entries.clear();
        ClassPathResource resource = new ClassPathResource("vycepay-error-catalog.yaml");
        if (!resource.exists()) {
            log.warn("vycepay-error-catalog.yaml missing; using hardcoded default only");
            defaultEntry = new VyceErrorEntry("INTERNAL_ERROR", 500, DEFAULT_USER_MESSAGE, false);
            return;
        }
        try (InputStream in = resource.getInputStream()) {
            Yaml yaml = new Yaml();
            @SuppressWarnings("unchecked")
            Map<String, Object> root = yaml.load(in);
            if (root == null) {
                throw new IllegalStateException("Empty vycepay-error-catalog.yaml");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> def = (Map<String, Object>) root.get("default");
            if (def != null) {
                defaultEntry = mapEntry("INTERNAL_ERROR", def);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> ent = (Map<String, Object>) root.get("entries");
            if (ent != null) {
                for (Map.Entry<String, Object> e : ent.entrySet()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> val = (Map<String, Object>) e.getValue();
                    entries.put(e.getKey(), mapEntry(e.getKey(), val));
                }
            }
        }
        log.info("VycePay error catalog loaded: {} entries", entries.size());
    }

    public VyceErrorEntry resolve(String code) {
        if (code == null || code.isBlank()) {
            return defaultEntry;
        }
        VyceErrorEntry entry = entries.get(code);
        return entry != null ? entry : defaultEntry;
    }

    /**
     * Customer-facing message for {@code code}. Catalog wins; unknown codes get the default message.
     */
    public String userMessage(String code) {
        return resolve(code).getUserMessage();
    }

    /**
     * Prefer catalog message. Use {@code fallback} only when the catalog has no exact entry
     * and fallback is non-blank (legacy throws before catalog coverage).
     */
    public String userMessage(String code, String fallback) {
        if (code != null && entries.containsKey(code)) {
            return entries.get(code).getUserMessage();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return defaultEntry.getUserMessage();
    }

    public Map<String, VyceErrorEntry> entriesView() {
        return Collections.unmodifiableMap(entries);
    }

    public VyceErrorEntry getDefaultEntry() {
        return defaultEntry;
    }

    private static VyceErrorEntry mapEntry(String code, Map<String, Object> m) {
        String resolvedCode = m.get("code") != null ? String.valueOf(m.get("code")) : code;
        int http = toInt(m.get("httpStatus"), 500);
        String userMessage = m.get("userMessage") != null
                ? String.valueOf(m.get("userMessage"))
                : DEFAULT_USER_MESSAGE;
        boolean retry = m.get("retryable") instanceof Boolean b ? b
                : Boolean.parseBoolean(String.valueOf(m.getOrDefault("retryable", "false")));
        return new VyceErrorEntry(resolvedCode, http, userMessage, retry);
    }

    private static int toInt(Object v, int dflt) {
        if (v instanceof Number n) {
            return n.intValue();
        }
        if (v != null) {
            try {
                return Integer.parseInt(v.toString());
            } catch (NumberFormatException ignored) {
                return dflt;
            }
        }
        return dflt;
    }
}
