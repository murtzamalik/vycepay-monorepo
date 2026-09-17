package com.vycepay.transaction.application.service;

import com.vycepay.transaction.api.v1.dto.UtilityBillersResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KenyaUtilitiesCatalogServiceTest {

    private KenyaUtilitiesCatalogService service;

    @BeforeEach
    void setUp() {
        service = new KenyaUtilitiesCatalogService();
        service.loadFromClasspath();
    }

    @Test
    void list_loadsAllKenyaBillers() {
        UtilityBillersResponse res = service.list(null);
        assertEquals(4, res.getCategories().size());
        assertEquals(8, res.getBillers().size());
        assertTrue(res.getBillers().stream().anyMatch(b -> "888880".equals(b.getPaybill())));
        assertTrue(res.getBillers().stream().anyMatch(b -> "kplc-prepaid".equals(b.getId())));
        assertEquals(1, res.getBillers().get(0).getAccountType());
    }

    @Test
    void list_filtersByCategory() {
        UtilityBillersResponse res = service.list("ELECTRICITY");
        assertFalse(res.getBillers().isEmpty());
        assertTrue(res.getBillers().stream().allMatch(b -> "ELECTRICITY".equals(b.getCategory())));
        assertEquals(2, res.getBillers().size());
    }
}
