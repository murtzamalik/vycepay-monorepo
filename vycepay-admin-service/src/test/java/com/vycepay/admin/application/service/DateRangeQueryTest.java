package com.vycepay.admin.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.vycepay.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class DateRangeQueryTest {

    @Test
    void emptyRangeHasNoBounds() {
        DateRangeQuery range = DateRangeQuery.of(null, null);
        assertFalse(range.hasRange());
    }

    @Test
    void validRangeParses() {
        DateRangeQuery range = DateRangeQuery.of("2026-01-01", "2026-01-31");
        assertTrue(range.hasRange());
        assertEquals("2026-01-01", range.from().toString());
        assertEquals("2026-01-31", range.to().toString());
    }

    @Test
    void rejectsPartialRange() {
        assertThrows(BusinessException.class, () -> DateRangeQuery.of("2026-01-01", null));
    }

    @Test
    void rejectsInvertedRange() {
        assertThrows(BusinessException.class, () -> DateRangeQuery.of("2026-06-01", "2026-01-01"));
    }

    @Test
    void applyAddsSqlClause() {
        DateRangeQuery range = DateRangeQuery.of("2026-01-01", "2026-01-31");
        StringBuilder where = new StringBuilder("1=1");
        List<Object> params = new ArrayList<>();
        range.apply("created_at", where, params);
        assertTrue(where.toString().contains("created_at"));
        assertEquals(2, params.size());
    }
}
