package com.vycepay.common.choicebank;

import com.vycepay.common.choicebank.dto.ChoiceBankResponse;
import com.vycepay.common.choicebank.port.ResponseSignatureVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Verifies Choice Bank response signatures using the same logic as request signing.
 * Enabled when vycepay.choice-bank.verify-response-signature=true.
 * Per Choice Bank: build string from code, msg, requestId, sender, timestamp, salt, data; SHA-256; compare.
 */
@Component
@ConditionalOnProperty(name = "vycepay.choice-bank.verify-response-signature", havingValue = "true")
public class ChoiceBankResponseSignatureVerifier implements ResponseSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(ChoiceBankResponseSignatureVerifier.class);

    private final String privateKey;

    public ChoiceBankResponseSignatureVerifier(
            @Value("${vycepay.choice-bank.private-key}") String privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public boolean verify(ChoiceBankResponse response) {
        if (response.getSignature() == null || response.getSalt() == null) {
            log.warn("Response missing signature or salt; cannot verify");
            return false;
        }
        Map<String, String> flat = ChoiceBankSignatureUtil.buildResponseFlatMap(
                response.getCode(), response.getMsg(), response.getRequestId(),
                response.getSender(), response.getLocale(), response.getTimestamp(),
                response.getSalt(), response.getData(), privateKey);
        String computed = ChoiceBankSignatureUtil.sign(flat);
        boolean valid = computed.equalsIgnoreCase(response.getSignature());
        if (!valid) {
            log.warn("Response signature verification failed for requestId={}", response.getRequestId());
        }
        return valid;
    }
}
