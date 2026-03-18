package com.vycepay.common.choicebank.port;

import com.vycepay.common.choicebank.dto.ChoiceBankResponse;

import java.util.Map;

/**
 * Port for outbound banking API calls. Adapters (e.g. Choice Bank) implement this.
 * Keeps domain/application layer independent of concrete provider.
 */
public interface BankingProviderPort {

    /**
     * Posts a signed request to the banking provider.
     *
     * @param path   Provider-specific endpoint path (e.g. "onboarding/v3/submitEasyOnboardingRequest")
     * @param params Request parameters
     * @return Provider response envelope
     */
    ChoiceBankResponse post(String path, Map<String, Object> params);
}
