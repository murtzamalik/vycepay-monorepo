package com.vycepay.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VyceErrorCatalogTest {

    private VyceErrorCatalog catalog;

    @BeforeEach
    void setUp() throws Exception {
        catalog = new VyceErrorCatalog();
        catalog.loadFromClasspath();
    }

    @Test
    void resolvesKnownAuthCodes() {
        assertThat(catalog.userMessage("INVALID_OTP"))
                .contains("OTP");
        assertThat(catalog.userMessage("INVALID_CREDENTIALS"))
                .containsIgnoringCase("PIN");
        assertThat(catalog.resolve("ACCOUNT_LOCKED").getHttpStatus()).isEqualTo(423);
    }

    @Test
    void unknownCodeUsesDefaultMessage() {
        assertThat(catalog.userMessage("TOTALLY_UNKNOWN_CODE"))
                .isEqualTo(VyceErrorCatalog.DEFAULT_USER_MESSAGE);
    }

    @Test
    void catalogMessageWinsOverFallbackWhenKnown() {
        assertThat(catalog.userMessage("INVALID_OTP", "legacy raw"))
                .isNotEqualTo("legacy raw")
                .contains("OTP");
    }

    @Test
    void fallbackUsedWhenCodeMissingFromCatalog() {
        assertThat(catalog.userMessage("UNKNOWN_XYZ", "custom fallback"))
                .isEqualTo("custom fallback");
    }

    @Test
    void bffCodesPresent() {
        assertThat(catalog.userMessage("UPSTREAM_ERROR")).isNotBlank();
        assertThat(catalog.userMessage("BAD_GATEWAY")).isNotBlank();
        assertThat(catalog.entriesView()).doesNotContainKey("BACKEND_ERROR");
    }
}
