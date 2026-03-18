package com.vycepay.common.choicebank.port;

import com.vycepay.common.choicebank.dto.ChoiceBankResponse;

/**
 * Port for verifying Choice Bank response signatures.
 * Per Choice Bank docs: verify response with private key to ensure authenticity.
 * Implementation builds string-to-sign from response fields and compares signature.
 */
public interface ResponseSignatureVerifier {

    /**
     * Verifies the response signature. Returns true if valid or verification disabled.
     *
     * @param response Parsed Choice Bank response (includes salt, signature)
     * @return true if signature is valid, false if invalid
     */
    boolean verify(ChoiceBankResponse response);
}
