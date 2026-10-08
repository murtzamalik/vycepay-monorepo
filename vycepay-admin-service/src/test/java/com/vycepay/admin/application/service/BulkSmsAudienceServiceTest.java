package com.vycepay.admin.application.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies ALL_CUSTOMERS audience resolution: invalid skip, dedupe, status SQL.
 */
class BulkSmsAudienceServiceTest {

    @Test
    void fromRows_skipsInvalid_dedupesRecipient() {
        BulkSmsAudienceService.ResolvedAudience resolved = BulkSmsAudienceService.fromRows(List.of(
                Map.of("id", 1L, "mobileCountryCode", "254", "mobile", "712345678"),
                Map.of("id", 2L, "mobileCountryCode", "254", "mobile", "bad"),
                Map.of("id", 3L, "mobileCountryCode", "254", "mobile", "712345678"),
                Map.of("id", 4L, "mobileCountryCode", "254", "mobile", "723456789")
        ));

        assertEquals(4, resolved.totalCustomers());
        assertEquals(1, resolved.skippedInvalid());
        assertEquals(2, resolved.recipients().size());
        assertEquals(1L, resolved.recipients().get(0).customerId());
        assertEquals("254712345678", resolved.recipients().get(0).recipient());
        assertEquals(4L, resolved.recipients().get(1).customerId());
        assertEquals("254723456789", resolved.recipients().get(1).recipient());
    }

    @Test
    void preview_mapsCounts_viaFromRows() {
        BulkSmsAudienceService.ResolvedAudience resolved = BulkSmsAudienceService.fromRows(List.of(
                Map.of("id", 1L, "mobileCountryCode", "254", "mobile", "712345678"),
                Map.of("id", 2L, "mobileCountryCode", "254", "mobile", "xx")
        ));
        assertEquals(2, resolved.totalCustomers());
        assertEquals(1, resolved.recipients().size());
        assertEquals(1, resolved.skippedInvalid());
    }

    @Test
    void eligibleStatusesSql_excludesDeactivated() {
        assertTrue(BulkSmsAudienceService.ELIGIBLE_STATUSES_SQL.contains("ACTIVE"));
        assertTrue(BulkSmsAudienceService.ELIGIBLE_STATUSES_SQL.contains("PENDING"));
        assertTrue(BulkSmsAudienceService.ELIGIBLE_STATUSES_SQL.contains("SUSPENDED"));
        assertTrue(!BulkSmsAudienceService.ELIGIBLE_STATUSES_SQL.contains("DEACTIVATED"));
    }

    @Test
    void maxCap_isTenThousand() {
        assertEquals(10_000, BulkSmsAudienceService.MAX_ALL_CUSTOMERS);
    }
}
