package com.vycepay.common.choicebank.errors;

import com.vycepay.common.choicebank.dto.ChoiceBankResponse;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.common.exception.ChoiceBankUpstreamException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChoiceBankResponseAssessorTest {

    private ChoiceBankResponseAssessor assessor;

    @BeforeEach
    void setUp() throws Exception {
        ChoiceBankErrorCatalog catalog = new ChoiceBankErrorCatalog();
        catalog.loadFromClasspath();
        assessor = new ChoiceBankResponseAssessor(catalog);
    }

    @Test
    void requireSuccessPassesOn00000() {
        ChoiceBankResponse r = new ChoiceBankResponse();
        r.setCode("00000");
        r.setMsg("ok");
        assessor.requireSuccess(r, "staticData/getBankCodes");
    }

    @Test
    void requireSuccessThrowsMappedException() {
        ChoiceBankResponse r = new ChoiceBankResponse();
        r.setCode("13000");
        r.setMsg("Account does not exist.");
        r.setRequestId("RID");
        assertThatThrownBy(() -> assessor.requireSuccess(r, "trans/v2/applyForTransfer"))
                .isInstanceOf(ChoiceBankUpstreamException.class)
                .satisfies(t -> {
                    ChoiceBankUpstreamException e = (ChoiceBankUpstreamException) t;
                    assertThat(e.getChoiceCode()).isEqualTo("13000");
                    assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void requireSuccessAndDataThrowsWhenGuardFails() {
        ChoiceBankResponse r = new ChoiceBankResponse();
        r.setCode("00000");
        r.setData(java.util.Map.of());
        assertThatThrownBy(() -> assessor.requireSuccessAndData(
                r, "x", resp -> false, "CHOICE_INVALID_RESPONSE", "missing tx"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "CHOICE_INVALID_RESPONSE");
    }
}
