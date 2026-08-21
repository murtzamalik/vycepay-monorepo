package com.vycepay.common.choicebank.errors;

/**
 * Resolves the customer-facing message for Choice Bank–backed flows.
 * <p>
 * Rule: if Choice returned a non-blank {@code msg}, show that; otherwise use the Vyce fallback.
 */
public final class ChoiceCustomerMessage {

    private ChoiceCustomerMessage() {
    }

    /**
     * Prefers Choice Bank text when present; otherwise returns {@code fallback}.
     *
     * @param choiceMsg raw Choice {@code msg} (may be null/blank)
     * @param fallback  Vyce catalog or static copy when Choice has no usable message
     * @return trimmed Choice msg, or fallback (may be null if both blank)
     */
    public static String prefer(String choiceMsg, String fallback) {
        if (choiceMsg != null) {
            String trimmed = choiceMsg.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return fallback;
    }
}
