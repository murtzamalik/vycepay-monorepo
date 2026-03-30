package com.vycepay.bff.choicebank;

import com.vycepay.common.choicebank.dto.ChoiceBankHttpTraceDto;

/**
 * One trace from a backend service, labeled so callers can tell which JVM produced it.
 */
public record ChoiceBankHttpTraceWithSource(String source, ChoiceBankHttpTraceDto trace) {
}
