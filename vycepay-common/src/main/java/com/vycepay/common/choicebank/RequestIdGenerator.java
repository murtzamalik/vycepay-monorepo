package com.vycepay.common.choicebank;

import java.util.UUID;

/**
 * Generates unique request IDs for Choice Bank API calls.
 * Choice requires requestId = senderId + 32 hex chars (UUID without hyphens).
 * Verified working format: e.g. VYCEIN45c7a2be85474bd2858197afee9b679f
 */
public final class RequestIdGenerator {

    /** Default sender ID when none provided (Choice sandbox uses VYCEIN). */
    private static final String DEFAULT_SENDER_ID = "VYCEIN";

    private RequestIdGenerator() {
        // utility class
    }

    /**
     * Generates a unique request ID in Choice Bank format: senderId + UUID (no hyphens).
     * Uses default sender ID {@value #DEFAULT_SENDER_ID}.
     *
     * @return Unique string (e.g. VYCEIN45c7a2be85474bd2858197afee9b679f)
     */
    public static String generate() {
        return generate(DEFAULT_SENDER_ID);
    }

    /**
     * Generates a unique request ID in Choice Bank format: senderId + UUID (no hyphens).
     * This format is required for signature validation (server returns 00000).
     *
     * @param senderId Sender ID (e.g. VYCEIN) – must match the one used in the request body
     * @return Unique string (e.g. VYCEIN45c7a2be85474bd2858197afee9b679f)
     */
    public static String generate(String senderId) {
        String prefix = (senderId != null && !senderId.isBlank()) ? senderId : DEFAULT_SENDER_ID;
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }
}
