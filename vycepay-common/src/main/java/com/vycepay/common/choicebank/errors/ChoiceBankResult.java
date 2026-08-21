package com.vycepay.common.choicebank.errors;

/**
 * Successful Choice Bank call outcome: payload plus customer-facing {@code msg}.
 *
 * @param data             Choice {@code data} (never null; empty map when Choice omitted data)
 * @param msg              Choice {@code msg} (may be null/blank)
 * @param choiceRequestId  Choice {@code requestId} (may be null)
 */
public record ChoiceBankResult(Object data, String msg, String choiceRequestId) {
}
