package com.vycepay.common.sms.port;

/**
 * Request to send one outbound SMS.
 *
 * @param recipient E.164 digits without plus (e.g. 254712345678)
 * @param message   SMS body text
 */
public record SmsSendRequest(String recipient, String message) {
}
