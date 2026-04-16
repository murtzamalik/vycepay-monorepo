package com.vycepay.common.choicebank.errors;

import com.vycepay.common.exception.ChoiceBankUpstreamException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads Choice BaaS error mappings from classpath {@code choice-bank-error-catalog.yaml}.
 * Resolves exact code first, then longest matching prefix, then default.
 */
@Component
public class ChoiceBankErrorCatalog {

    private static final Logger log = LoggerFactory.getLogger(ChoiceBankErrorCatalog.class);

    private final Map<String, ChoiceBankErrorMapping> entries = new HashMap<>();
    private final List<PrefixRule> prefixRules = new ArrayList<>();
    private ChoiceBankErrorMapping defaultMapping;

    @PostConstruct
    public void initialize() {
        try {
            loadFromClasspath();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load choice-bank-error-catalog.yaml", e);
        }
    }

    /**
     * Reloads catalog from classpath (intended for tests).
     */
    public void loadFromClasspath() throws IOException {
        ClassPathResource resource = new ClassPathResource("choice-bank-error-catalog.yaml");
        entries.clear();
        prefixRules.clear();
        if (!resource.exists()) {
            log.warn("choice-bank-error-catalog.yaml missing; using hardcoded defaults only");
            defaultMapping = new ChoiceBankErrorMapping("CHOICE_BANK_ERROR", 502, false, null);
            return;
        }
        try (InputStream in = resource.getInputStream()) {
            Yaml yaml = new Yaml();
            @SuppressWarnings("unchecked")
            Map<String, Object> root = yaml.load(in);
            if (root == null) {
                throw new IllegalStateException("Empty choice-bank-error-catalog.yaml");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> def = (Map<String, Object>) root.get("default");
            if (def != null) {
                defaultMapping = mapEntry("default", def);
            } else {
                defaultMapping = new ChoiceBankErrorMapping("CHOICE_BANK_ERROR", 502, false, null);
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fallbacks = (List<Map<String, Object>>) root.get("prefixFallbacks");
            if (fallbacks != null) {
                for (Map<String, Object> fb : fallbacks) {
                    String prefix = String.valueOf(fb.get("prefix"));
                    prefixRules.add(new PrefixRule(prefix, mapEntry(prefix, fb)));
                }
                prefixRules.sort(Comparator.comparingInt((PrefixRule r) -> r.prefix.length()).reversed());
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
        log.info("Choice Bank error catalog loaded: {} exact entries, {} prefix fallbacks",
                entries.size(), prefixRules.size());
    }


    private static ChoiceBankErrorMapping mapEntry(String key, Map<String, Object> m) {
        String clientCode = String.valueOf(m.get("clientCode"));
        int http = toInt(m.get("httpStatus"), 502);
        boolean retry = m.get("retryable") instanceof Boolean b ? b
                : Boolean.parseBoolean(String.valueOf(m.getOrDefault("retryable", "false")));
        String category = m.get("category") != null ? String.valueOf(m.get("category")) : null;
        return new ChoiceBankErrorMapping(clientCode, http, retry, category);
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

    /**
     * Resolves VycePay-facing mapping for a Choice {@code code} (non-00000).
     */
    public ChoiceBankErrorMapping resolve(String choiceCode) {
        if (choiceCode == null || choiceCode.isBlank()) {
            return defaultMapping;
        }
        ChoiceBankErrorMapping exact = entries.get(choiceCode);
        if (exact != null) {
            return exact;
        }
        for (PrefixRule rule : prefixRules) {
            if (choiceCode.startsWith(rule.prefix)) {
                return rule.mapping;
            }
        }
        return defaultMapping;
    }

    public ChoiceBankUpstreamException toException(
            String choiceCode,
            String choiceMsg,
            String choiceRequestId,
            String path) {
        ChoiceBankErrorMapping m = resolve(choiceCode);
        HttpStatus status = HttpStatus.resolve(m.getHttpStatus());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        String message = (choiceMsg != null && !choiceMsg.isBlank())
                ? choiceMsg
                : status.getReasonPhrase();
        return new ChoiceBankUpstreamException(
                m.getClientCode(),
                message,
                status,
                choiceCode != null ? choiceCode : "UNKNOWN",
                choiceMsg,
                choiceRequestId,
                path,
                m.isRetryable());
    }

    private record PrefixRule(String prefix, ChoiceBankErrorMapping mapping) {}
}
