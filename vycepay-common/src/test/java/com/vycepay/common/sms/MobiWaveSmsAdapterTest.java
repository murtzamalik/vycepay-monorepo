package com.vycepay.common.sms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vycepay.common.sms.port.SmsSendRequest;
import com.vycepay.common.sms.port.SmsSendResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobiWaveSmsAdapterTest {

    @Test
    void send_success_parsesUid() {
        RestTemplate rt = new RestTemplate() {
            @Override
            public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
                                                  Class<T> responseType, Object... uriVariables) {
                @SuppressWarnings("unchecked")
                ResponseEntity<T> response = (ResponseEntity<T>) new ResponseEntity<>(
                        "{\"status\":\"success\",\"data\":{\"uid\":\"msg-123\"}}", HttpStatus.OK);
                return response;
            }
        };
        MobiWaveSmsAdapter adapter = new MobiWaveSmsAdapter(
                "https://sms.mobiwave.co.ke/api/v3", "test-token", "VycePay",
                rt, new ObjectMapper(), null, null);

        SmsSendResult result = adapter.send(new SmsSendRequest("254712345678", "Hello"));

        assertEquals(SmsSendResult.SENT, result.status());
        assertEquals("msg-123", result.providerUid());
        assertNull(result.errorMessage());
    }

    @Test
    void send_error_returnsFailed() {
        RestTemplate rt = new RestTemplate() {
            @Override
            public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> requestEntity,
                                                  Class<T> responseType, Object... uriVariables) {
                @SuppressWarnings("unchecked")
                ResponseEntity<T> response = (ResponseEntity<T>) new ResponseEntity<>(
                        "{\"status\":\"error\",\"message\":\"Insufficient units\"}", HttpStatus.OK);
                return response;
            }
        };
        MobiWaveSmsAdapter adapter = new MobiWaveSmsAdapter(
                "https://sms.mobiwave.co.ke/api/v3", "test-token", "VycePay",
                rt, new ObjectMapper(), null, null);

        SmsSendResult result = adapter.send(new SmsSendRequest("254712345678", "Hello"));

        assertEquals(SmsSendResult.FAILED, result.status());
        assertTrue(result.errorMessage().contains("Insufficient"));
    }

    @Test
    void send_blankRecipient_fails() {
        MobiWaveSmsAdapter adapter = new MobiWaveSmsAdapter(
                "https://sms.mobiwave.co.ke/api/v3", "test-token", "VycePay",
                new RestTemplate(), new ObjectMapper(), null, null);
        SmsSendResult result = adapter.send(new SmsSendRequest(" ", "Hello"));
        assertEquals(SmsSendResult.FAILED, result.status());
    }
}
