package com.vycepay.common.choicebank;

import com.vycepay.common.choicebank.dto.ChoiceBankHttpTraceDto;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Thread-safe ring buffer of recent Choice BaaS HTTP exchanges for this JVM.
 */
public class ChoiceBankHttpAuditStore {

    private final int maxEntries;
    private final Deque<ChoiceBankHttpTraceDto> deque = new ArrayDeque<>();

    public ChoiceBankHttpAuditStore(int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);
    }

    /**
     * Records a completed or failed outbound call.
     */
    public synchronized void record(String path,
                                    String choiceBankRequestId,
                                    String requestPayloadSanitized,
                                    String responsePayloadSanitized,
                                    String responseCode,
                                    String responseMsg,
                                    String error) {
        ChoiceBankHttpTraceDto e = new ChoiceBankHttpTraceDto();
        e.setTimestamp(Instant.now());
        e.setPath(path);
        e.setChoiceBankRequestId(choiceBankRequestId);
        e.setRequestPayload(requestPayloadSanitized);
        e.setResponsePayload(responsePayloadSanitized);
        e.setResponseCode(responseCode);
        e.setResponseMsg(responseMsg);
        e.setError(error);
        deque.addFirst(e);
        while (deque.size() > maxEntries) {
            deque.removeLast();
        }
    }

    /**
     * Newest first.
     */
    public synchronized List<ChoiceBankHttpTraceDto> getRecentTraces() {
        return new ArrayList<>(deque);
    }
}
