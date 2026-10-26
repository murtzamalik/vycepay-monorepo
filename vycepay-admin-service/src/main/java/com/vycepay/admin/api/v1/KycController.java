package com.vycepay.admin.api.v1;

import java.util.Map;

import com.vycepay.admin.application.service.AdminKycBiometricService;
import com.vycepay.admin.application.service.AdminReadService;
import com.vycepay.admin.application.service.AdminSecurityContext;
import com.vycepay.common.api.ApiSuccessResponse;
import com.vycepay.common.api.ApiSuccessResponses;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.common.media.port.MediaStoragePort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** KYC backoffice list and detail APIs. */
@RestController
@RequestMapping("/api/admin/v1/kyc")
@PreAuthorize("hasAuthority('PERM_kyc:view')")
public class KycController {
    private final AdminReadService s;
    private final AdminKycBiometricService biometricService;
    private final MediaStoragePort mediaStoragePort;
    private final AdminSecurityContext securityContext;

    public KycController(AdminReadService s,
                         AdminKycBiometricService biometricService,
                         MediaStoragePort mediaStoragePort,
                         AdminSecurityContext securityContext) {
        this.s = s;
        this.biometricService = biometricService;
        this.mediaStoragePort = mediaStoragePort;
        this.securityContext = securityContext;
    }

    @GetMapping
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order,
            @RequestParam(required = false) String biometricStatus,
            @RequestParam(required = false) String failCode) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("KYC_OK", "KYC",
                s.kyc(page, size, status, search, fromDate, toDate, sort, order, biometricStatus, failCode)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiSuccessResponses.ok("KYC_DETAIL_OK", "KYC detail", s.kycDetail(id)));
    }

    /**
     * Streams stored KYC document bytes (requires kyc:view_documents).
     */
    @GetMapping("/{id}/documents/{docType}")
    @PreAuthorize("hasAuthority('PERM_kyc:view_documents')")
    public ResponseEntity<byte[]> document(@PathVariable Long id, @PathVariable String docType) {
        if (!securityContext.hasPermission("kyc:view_documents")) {
            throw new BusinessException("FORBIDDEN", "Document access denied", HttpStatus.FORBIDDEN);
        }
        String key = biometricService.resolveObjectKey(id, docType);
        if (key == null || key.isBlank()) {
            throw new BusinessException("MEDIA_NOT_FOUND", "Document not found", HttpStatus.NOT_FOUND);
        }
        byte[] bytes = mediaStoragePort.load(key);
        MediaType mediaType = docType.contains("video") ? MediaType.parseMediaType("video/mp4") : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=60")
                .contentType(mediaType)
                .body(bytes);
    }
}
