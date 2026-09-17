package com.vycepay.transaction.application.service;

import com.vycepay.transaction.api.v1.dto.UtilityBillerCategoryDto;
import com.vycepay.transaction.api.v1.dto.UtilityBillerDto;
import com.vycepay.transaction.api.v1.dto.UtilityBillersResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serves curated Kenya paybill/utility catalog from classpath YAML (no Choice dependency).
 */
@Service
public class KenyaUtilitiesCatalogService {

    private static final Logger log = LoggerFactory.getLogger(KenyaUtilitiesCatalogService.class);
    private static final String RESOURCE = "kenya-utilities-catalog.yaml";

    private List<UtilityBillerCategoryDto> categories = List.of();
    private List<UtilityBillerDto> billers = List.of();

    @PostConstruct
    public void initialize() {
        loadFromClasspath();
    }

    /**
     * Reloads catalog (tests may call after construction).
     */
    public void loadFromClasspath() {
        ClassPathResource resource = new ClassPathResource(RESOURCE);
        if (!resource.exists()) {
            log.warn("{} missing; utilities catalog empty", RESOURCE);
            categories = List.of();
            billers = List.of();
            return;
        }
        try (InputStream in = resource.getInputStream()) {
            Yaml yaml = new Yaml();
            @SuppressWarnings("unchecked")
            Map<String, Object> root = yaml.load(in);
            if (root == null) {
                throw new IllegalStateException("Empty " + RESOURCE);
            }
            categories = parseCategories(root.get("categories"));
            billers = parseBillers(root.get("billers"));
            log.info("Loaded Kenya utilities catalog: {} categories, {} billers",
                    categories.size(), billers.size());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load " + RESOURCE, e);
        }
    }

    /**
     * Returns full catalog, optionally filtered by category id (case-insensitive).
     */
    public UtilityBillersResponse list(String categoryFilter) {
        List<UtilityBillerDto> filtered = billers;
        if (categoryFilter != null && !categoryFilter.isBlank()) {
            String want = categoryFilter.trim().toUpperCase(Locale.ROOT);
            filtered = billers.stream()
                    .filter(b -> b.getCategory() != null && b.getCategory().equalsIgnoreCase(want))
                    .collect(Collectors.toList());
        }
        return new UtilityBillersResponse(categories, filtered);
    }

    @SuppressWarnings("unchecked")
    private static List<UtilityBillerCategoryDto> parseCategories(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<UtilityBillerCategoryDto> out = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Map<String, Object> m = (Map<String, Object>) map;
            String id = stringVal(m.get("id"));
            String name = stringVal(m.get("name"));
            if (id != null && name != null) {
                out.add(new UtilityBillerCategoryDto(id, name));
            }
        }
        return Collections.unmodifiableList(out);
    }

    @SuppressWarnings("unchecked")
    private static List<UtilityBillerDto> parseBillers(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<UtilityBillerDto> out = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Map<String, Object> m = (Map<String, Object>) map;
            String id = stringVal(m.get("id"));
            String name = stringVal(m.get("name"));
            String category = stringVal(m.get("category"));
            String paybill = stringVal(m.get("paybill"));
            if (id == null || name == null || category == null || paybill == null) {
                continue;
            }
            List<String> alts = parseStringList(m.get("alternatePaybills"));
            out.add(new UtilityBillerDto(
                    id,
                    name,
                    category,
                    paybill,
                    alts,
                    stringVal(m.get("accountHint")),
                    stringVal(m.get("notes"))));
        }
        return Collections.unmodifiableList(out);
    }

    @SuppressWarnings("unchecked")
    private static List<String> parseStringList(Object raw) {
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Object o : list) {
            String s = stringVal(o);
            if (s != null) {
                out.add(s);
            }
        }
        return Collections.unmodifiableList(out);
    }

    private static String stringVal(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
