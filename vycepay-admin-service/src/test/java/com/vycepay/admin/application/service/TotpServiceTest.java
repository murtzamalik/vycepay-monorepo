package com.vycepay.admin.application.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class TotpServiceTest {
    @Test
    void verifiesRfc6238TotpCodeWithinTimeWindow() {
        TotpService service = new TotpService(Clock.fixed(Instant.ofEpochSecond(59), ZoneOffset.UTC));

        assertTrue(service.verify("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", "287082"));
        assertFalse(service.verify("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", "000000"));
    }
}
