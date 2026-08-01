package com.vycepay.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone MVC test: BusinessException maps to catalog userMessage envelope.
 */
class GlobalExceptionHandlerWebMvcTest {

    @RestController
    static class ProbeController {
        @GetMapping("/probe/otp")
        public void otp() {
            throw new BusinessException("INVALID_OTP", "raw should be replaced", HttpStatus.BAD_REQUEST);
        }

        @GetMapping("/probe/boom")
        public void boom() {
            throw new IllegalStateException("secret internal detail");
        }
    }

    @Test
    void businessExceptionUsesCatalogMessage() throws Exception {
        VyceErrorCatalog catalog = new VyceErrorCatalog();
        catalog.loadFromClasspath();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler(catalog))
                .build();

        mockMvc.perform(get("/probe/otp").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OTP"))
                .andExpect(jsonPath("$.message").value(catalog.userMessage("INVALID_OTP")))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void conflictDoesNotLeakInternalDetail() throws Exception {
        VyceErrorCatalog catalog = new VyceErrorCatalog();
        catalog.loadFromClasspath();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler(catalog))
                .build();

        mockMvc.perform(get("/probe/boom").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value(catalog.userMessage("CONFLICT")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))));
    }
}
