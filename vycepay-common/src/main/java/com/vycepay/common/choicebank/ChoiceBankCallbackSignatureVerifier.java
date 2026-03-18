package com.vycepay.common.choicebank;

import com.vycepay.common.choicebank.dto.ChoiceBankCallbackPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import com.vycepay.common.config.CallbackSignatureVerificationCondition;

/**
 * Verifies Choice Bank webhook callback signatures.
 * Enabled when vycepay.callback.verify-signature=true and private-key is configured.
 * Per Choice Bank: same algorithm as request/response signing.
 */
@Component
@Conditional(CallbackSignatureVerificationCondition.class)
public class ChoiceBankCallbackSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(ChoiceBankCallbackSignatureVerifier.class);

    private final String privateKey;

    public ChoiceBankCallbackSignatureVerifier(
            @Value("${vycepay.choice-bank.private-key:}") String privateKey) {
        this.privateKey = privateKey;
    }

    /**
     * Verifies the callback signature. Returns false if private key is blank,
     * or if signature/salt is missing, or if verification fails.
     */
    public boolean verify(ChoiceBankCallbackPayload payload) {
        if (privateKey == null || privateKey.isBlank()) {
            log.warn("Callback verification enabled but private key not configured");
            return false;
        }
        if (payload.getSignature() == null || payload.getSalt() == null) {
            log.warn("Callback missing signature or salt; cannot verify");
            return false;
        }
        var flat = ChoiceBankSignatureUtil.buildCallbackFlatMap(
                payload.getRequestId(), payload.getSender(), payload.getLocale(),
                payload.getTimestamp(), payload.getNotificationType(),
                payload.getParams(), payload.getSalt(), privateKey);
        String computed = ChoiceBankSignatureUtil.sign(flat);
        boolean valid = computed.equalsIgnoreCase(payload.getSignature());
        if (!valid) {
            log.warn("Callback signature verification failed for requestId={} type={}",
                    payload.getRequestId(), payload.getNotificationType());
        }
        return valid;
    }
}
