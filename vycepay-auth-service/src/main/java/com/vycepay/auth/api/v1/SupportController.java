package com.vycepay.auth.api.v1;

import com.vycepay.auth.api.v1.dto.support.AboutVycePayResponse;
import com.vycepay.auth.api.v1.dto.support.HelpCentreResponse;
import com.vycepay.auth.application.content.SupportContentProvider;
import com.vycepay.common.api.ApiSuccessResponse;
import com.vycepay.common.api.ApiSuccessResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * In-app support content: help centre FAQs and About VycePay screen copy.
 * Served via BFF at /api/v1/auth/support/* (JWT required at BFF).
 */
@RestController
@RequestMapping("/api/v1/auth/support")
@Tag(name = "Support", description = "Help centre and About VycePay content for mobile profile")
public class SupportController {

    private final SupportContentProvider contentProvider;

    public SupportController(SupportContentProvider contentProvider) {
        this.contentProvider = contentProvider;
    }

    /**
     * Returns grouped FAQs, contact channels, and legal links for the Help centre screen.
     */
    @GetMapping("/help-centre")
    @Operation(summary = "Get help centre content")
    public ResponseEntity<ApiSuccessResponse<HelpCentreResponse>> helpCentre() {
        HelpCentreResponse data = contentProvider.helpCentre();
        return ResponseEntity.ok(ApiSuccessResponses.ok(
                "HELP_CENTRE_OK", "Help centre content retrieved.", data));
    }

    /**
     * Returns brand, mission, highlights, and legal links for the About VycePay screen.
     */
    @GetMapping("/about")
    @Operation(summary = "Get About VycePay content")
    public ResponseEntity<ApiSuccessResponse<AboutVycePayResponse>> aboutVycePay() {
        AboutVycePayResponse data = contentProvider.aboutVycePay();
        return ResponseEntity.ok(ApiSuccessResponses.ok(
                "ABOUT_VYCEPAY_OK", "About VycePay content retrieved.", data));
    }
}
