package com.vycepay.callback.api.v1;

import com.vycepay.callback.application.service.CallbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Receives Choice Bank webhook callbacks. Must always return HTTP 200 and body "ok"
 * to prevent Choice from retrying. Processing is asynchronous.
 */
@RestController
@RequestMapping("/api/v1/choice-bank")
public class ChoiceBankCallbackController {

    private final CallbackService callbackService;

    public ChoiceBankCallbackController(CallbackService callbackService) {
        this.callbackService = callbackService;
    }

    /**
     * Accepts raw JSON payload from Choice Bank. Persists and routes asynchronously.
     *
     * @param rawPayload Raw JSON body from webhook
     * @return "ok" to acknowledge receipt
     */
    @PostMapping("/callback")
    public ResponseEntity<String> receive(@RequestBody String rawPayload) {
        callbackService.receiveAndProcess(rawPayload);
        return ResponseEntity.ok("ok");
    }
}
