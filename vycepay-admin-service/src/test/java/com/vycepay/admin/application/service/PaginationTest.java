package com.vycepay.admin.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class PaginationTest {
    @Test
    void boundsPageSizeToOneHundredRows() {
        assertEquals(100, Pagination.size(1_000));
    }

    @Test
    void createsConsistentPageEnvelope() {
        var response = Pagination.response(List.of(), 2, 20, 41);
        assertEquals(2, response.get("page"));
        assertEquals(3, response.get("totalPages"));
    }
}
