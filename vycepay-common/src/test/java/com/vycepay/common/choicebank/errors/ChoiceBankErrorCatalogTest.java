package com.vycepay.common.choicebank.errors;

import com.vycepay.common.exception.ChoiceBankUpstreamException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ChoiceBankErrorCatalogTest {

    private ChoiceBankErrorCatalog catalog;

    @BeforeEach
    void setUp() throws Exception {
        catalog = new ChoiceBankErrorCatalog();
        catalog.loadFromClasspath();
    }

    @Test
    void resolveExact13000() {
        ChoiceBankErrorMapping m = catalog.resolve("13000");
        assertThat(m.getClientCode()).isEqualTo("CHOICE_ACCOUNT_NOT_FOUND");
        assertThat(m.getHttpStatus()).isEqualTo(400);
    }

    @Test
    void resolveUnknownUsesDefault() {
        ChoiceBankErrorMapping m = catalog.resolve("99999");
        assertThat(m.getClientCode()).isEqualTo("CHOICE_BANK_ERROR");
        assertThat(m.getHttpStatus()).isEqualTo(502);
    }

    @Test
    void prefixFallback14xxx() {
        ChoiceBankErrorMapping m = catalog.resolve("14099");
        assertThat(m.getClientCode()).isEqualTo("CHOICE_TRANSACTION_ERROR");
        assertThat(m.getHttpStatus()).isEqualTo(400);
    }

    @Test
    void toExceptionCarriesMetadata() {
        ChoiceBankUpstreamException ex = catalog.toException("13000", "Account does not exist.", "REQ1", "trans/v2/applyForTransfer");
        assertThat(ex.getCode()).isEqualTo("CHOICE_ACCOUNT_NOT_FOUND");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getChoiceCode()).isEqualTo("13000");
        assertThat(ex.getChoiceRequestId()).isEqualTo("REQ1");
        assertThat(ex.getChoicePath()).isEqualTo("trans/v2/applyForTransfer");
        assertThat(ex.getMessage()).contains("Account does not exist");
    }
}
