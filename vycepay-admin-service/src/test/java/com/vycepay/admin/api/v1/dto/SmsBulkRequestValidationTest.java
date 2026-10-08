package com.vycepay.admin.api.v1.dto;

import com.vycepay.admin.api.v1.dto.AdminRequests.SmsBulkRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bean-validation rules for MANUAL vs ALL_CUSTOMERS bulk SMS requests.
 *
 * Callers: JUnit only. No production importers. Validates SmsBulkRequest used by POST /sms/bulk.
 * No data files. User: Implement the plan as specified... Do NOT edit the plan file itself.
 */
class SmsBulkRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void manual_requiresRecipients() {
        SmsBulkRequest req = new SmsBulkRequest("MANUAL", null, "Hello world", "Audit reason long enough");
        Set<ConstraintViolation<SmsBulkRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }

    @Test
    void manual_rejectsMoreThan100() {
        List<String> phones = IntStream.range(0, 101)
                .mapToObj(i -> "2547123456" + String.format("%02d", i % 100))
                .toList();
        SmsBulkRequest req = new SmsBulkRequest("MANUAL", phones, "Hello world", "Audit reason long enough");
        Set<ConstraintViolation<SmsBulkRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }

    @Test
    void manual_acceptsValidList_defaultsAudience() {
        SmsBulkRequest req = new SmsBulkRequest(
                null, List.of("254712345678"), "Hello world", "Audit reason long enough");
        Set<ConstraintViolation<SmsBulkRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
        assertEquals("MANUAL", req.resolvedAudience());
    }

    @Test
    void allCustomers_allowsEmptyRecipients() {
        SmsBulkRequest req = new SmsBulkRequest(
                "ALL_CUSTOMERS", Collections.emptyList(), "Hello world", "Audit reason long enough");
        Set<ConstraintViolation<SmsBulkRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
        assertEquals("ALL_CUSTOMERS", req.resolvedAudience());
    }

    @Test
    void allCustomers_allowsNullRecipients() {
        SmsBulkRequest req = new SmsBulkRequest(
                "ALL_CUSTOMERS", null, "Hello world", "Audit reason long enough");
        Set<ConstraintViolation<SmsBulkRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    void rejectsUnknownAudience() {
        SmsBulkRequest req = new SmsBulkRequest(
                "EVERYONE", List.of("254712345678"), "Hello world", "Audit reason long enough");
        Set<ConstraintViolation<SmsBulkRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }
}
