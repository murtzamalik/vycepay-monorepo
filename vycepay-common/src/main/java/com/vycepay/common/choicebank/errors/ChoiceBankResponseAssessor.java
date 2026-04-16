package com.vycepay.common.choicebank.errors;

import com.vycepay.common.choicebank.dto.ChoiceBankResponse;
import com.vycepay.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;

/**
 * Validates Choice Bank responses after {@link com.vycepay.common.choicebank.port.BankingProviderPort#post}.
 * Maps non-success codes via {@link ChoiceBankErrorCatalog}.
 */
@Component
public class ChoiceBankResponseAssessor {

    private final ChoiceBankErrorCatalog catalog;

    public ChoiceBankResponseAssessor(ChoiceBankErrorCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Throws {@link ChoiceBankUpstreamException} if the response is not Choice success ({@code code=00000}).
     */
    public void requireSuccess(ChoiceBankResponse response, String path) {
        if (response == null) {
            throw new BusinessException("CHOICE_NULL_RESPONSE", "Empty response from Choice Bank", HttpStatus.BAD_GATEWAY);
        }
        if (response.isSuccess()) {
            return;
        }
        throw catalog.toException(response.getCode(), response.getMsg(), response.getRequestId(), path);
    }

    /**
     * After {@link #requireSuccess}, validates business data; throws {@link BusinessException} if guard fails.
     */
    public void requireSuccessAndData(
            ChoiceBankResponse response,
            String path,
            Predicate<ChoiceBankResponse> dataGuard,
            String clientCode,
            String message) {
        requireSuccess(response, path);
        if (!dataGuard.test(response)) {
            throw new BusinessException(
                    clientCode,
                    message != null ? message : "Invalid or incomplete data from Choice Bank",
                    HttpStatus.BAD_GATEWAY);
        }
    }
}
