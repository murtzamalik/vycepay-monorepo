package com.vycepay.common.choicebank;

import com.vycepay.common.choicebank.dto.ChoiceBankResponse;
import com.vycepay.common.choicebank.port.BankingProviderPort;
import com.vycepay.common.choicebank.port.ResponseSignatureVerifier;
import com.vycepay.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Adapter implementing BankingProviderPort for Choice Bank BaaS API.
 * Delegates to ChoiceBankClient for actual HTTP calls.
 * When ResponseSignatureVerifier is configured, verifies response signature before returning.
 */
@Component
public class ChoiceBankApiAdapter implements BankingProviderPort {

    private static final Logger log = LoggerFactory.getLogger(ChoiceBankApiAdapter.class);

    private final ChoiceBankClient choiceBankClient;
    private final ResponseSignatureVerifier responseSignatureVerifier;

    public ChoiceBankApiAdapter(ChoiceBankClient choiceBankClient,
                               @Autowired(required = false) ResponseSignatureVerifier responseSignatureVerifier) {
        this.choiceBankClient = choiceBankClient;
        this.responseSignatureVerifier = responseSignatureVerifier;
    }

    @Override
    public ChoiceBankResponse post(String path, Map<String, Object> params) {
        ChoiceBankResponse response = choiceBankClient.post(path, params);
        if (responseSignatureVerifier != null && !responseSignatureVerifier.verify(response)) {
            log.warn("Choice Bank response signature verification failed for requestId={}", response.getRequestId());
            throw new BusinessException("INVALID_RESPONSE_SIGNATURE", "Response signature verification failed", HttpStatus.BAD_GATEWAY);
        }
        return response;
    }
}
