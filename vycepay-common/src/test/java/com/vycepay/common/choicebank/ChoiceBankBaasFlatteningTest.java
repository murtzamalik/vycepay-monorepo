package com.vycepay.common.choicebank;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * BaaS request signing flatten rules: arrays as {@code path[0]}, empty array {@code path=[]} (Choice Postman / GitBook).
 */
class ChoiceBankBaasFlatteningTest {

    @Test
    void flattenNested_emptyAndIndexedArrays_matchChoiceRules() {
        Map<String, String> flat = new HashMap<>();
        Map<String, Object> params = new HashMap<>();
        params.put("txType", List.of());
        params.put("txStatus", List.of(2, 8));

        ChoiceBankSignatureUtil.flattenNested(flat, "params", params);

        assertEquals("[]", flat.get("params.txType"));
        assertEquals("2", flat.get("params.txStatus[0]"));
        assertEquals("8", flat.get("params.txStatus[1]"));
    }

    @Test
    void sign_differsFromLegacyListToString() {
        Map<String, String> correct = new HashMap<>();
        correct.put("locale", "en_KE");
        correct.put("requestId", "VYCEINabc");
        correct.put("salt", "salt12345678");
        correct.put("sender", "VYCEIN");
        correct.put("timestamp", "1000");
        Map<String, Object> params = new HashMap<>();
        params.put("txType", List.of());
        params.put("txStatus", List.of(2, 8));
        ChoiceBankSignatureUtil.flattenNested(correct, "params", params);
        correct.put("senderKey", "secret");

        Map<String, String> legacy = new HashMap<>(correct);
        legacy.remove("params.txType");
        legacy.remove("params.txStatus[0]");
        legacy.remove("params.txStatus[1]");
        legacy.put("params.txType", "[]");
        legacy.put("params.txStatus", "[2, 8]");

        assertNotEquals(ChoiceBankSignatureUtil.sign(correct), ChoiceBankSignatureUtil.sign(legacy));
    }
}
