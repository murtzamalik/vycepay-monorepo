package com.vycepay.common.choicebank.errors;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChoiceCustomerMessageTest {

    @Test
    void preferChoiceWhenNonBlank() {
        assertThat(ChoiceCustomerMessage.prefer("Account does not exist.", "fallback"))
                .isEqualTo("Account does not exist.");
    }

    @Test
    void preferTrimsChoiceMsg() {
        assertThat(ChoiceCustomerMessage.prefer("  ok  ", "fallback")).isEqualTo("ok");
    }

    @Test
    void preferFallbackWhenChoiceBlank() {
        assertThat(ChoiceCustomerMessage.prefer("  ", "fallback")).isEqualTo("fallback");
        assertThat(ChoiceCustomerMessage.prefer(null, "fallback")).isEqualTo("fallback");
        assertThat(ChoiceCustomerMessage.prefer("", "fallback")).isEqualTo("fallback");
    }
}
