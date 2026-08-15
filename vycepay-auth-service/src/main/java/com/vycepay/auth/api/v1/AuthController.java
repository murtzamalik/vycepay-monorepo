package com.vycepay.auth.api.v1;

import com.vycepay.auth.api.v1.dto.AuthResponse;
import com.vycepay.auth.api.v1.dto.ChangePinRequest;
import com.vycepay.auth.api.v1.dto.CustomerProfileResponse;
import com.vycepay.auth.api.v1.dto.ForgotPinConfirmRequest;
import com.vycepay.auth.api.v1.dto.ForgotPinRequest;
import com.vycepay.auth.api.v1.dto.LoginRequest;
import com.vycepay.auth.api.v1.dto.RegisterDeviceRequest;
import com.vycepay.auth.api.v1.dto.RegisterDeviceResponse;
import com.vycepay.auth.api.v1.dto.RegisterRequest;
import com.vycepay.auth.api.v1.dto.SetCredentialsRequest;
import com.vycepay.auth.api.v1.dto.VerifyContactsRequest;
import com.vycepay.auth.api.v1.dto.VerifyContactsResponse;
import com.vycepay.auth.api.v1.dto.VerifyDeviceOtpRequest;
import com.vycepay.auth.api.v1.dto.VerifyMigrateOtpRequest;
import com.vycepay.auth.api.v1.dto.VerifyOtpRequest;
import com.vycepay.auth.application.facade.AuthFacade;
import com.vycepay.auth.application.service.ContactsVerifyService;
import com.vycepay.auth.application.service.JwtService;
import com.vycepay.auth.domain.model.Customer;
import com.vycepay.auth.domain.model.DeviceToken;
import com.vycepay.auth.infrastructure.persistence.CustomerRepository;
import com.vycepay.auth.infrastructure.persistence.DeviceTokenRepository;
import com.vycepay.common.api.ApiSuccessResponse;
import com.vycepay.common.api.ApiSuccessResponses;
import com.vycepay.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Auth API: signup OTP, PIN login, device bind, credentials, forgot-PIN, profile, logout.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthFacade authFacade;
    private final JwtService jwtService;
    private final CustomerRepository customerRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final ContactsVerifyService contactsVerifyService;

    public AuthController(AuthFacade authFacade, JwtService jwtService,
                          CustomerRepository customerRepository,
                          DeviceTokenRepository deviceTokenRepository,
                          ContactsVerifyService contactsVerifyService) {
        this.authFacade = authFacade;
        this.jwtService = jwtService;
        this.customerRepository = customerRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        this.contactsVerifyService = contactsVerifyService;
    }

    /**
     * Sends SIGNUP OTP to mobile for registration.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiSuccessResponse<Void>> register(@RequestBody RegisterRequest request) {
        authFacade.sendSignupOtp(request.getMobileCountryCode(), request.getMobile());
        return ResponseEntity.ok(ApiSuccessResponses.ok("AUTH_OTP_SENT", "OTP sent successfully."));
    }

    /**
     * Verifies SIGNUP OTP, binds IMEI, returns JWT. Creates customer if new.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiSuccessResponse<AuthResponse>> verifyOtp(@RequestBody VerifyOtpRequest request) {
        AuthResponse data = authFacade.verifySignupOtp(
                request.getMobileCountryCode(),
                request.getMobile(),
                request.getOtpCode(),
                request.getImei(),
                request.getFcmToken(),
                request.getPlatform());
        return ResponseEntity.ok(ApiSuccessResponses.ok("AUTH_OTP_VERIFIED", "OTP verified.", data));
    }

    /**
     * PIN login with username or mobile + IMEI. May return deviceOtpRequired or mustSetCredentials.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiSuccessResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
        AuthResponse data = authFacade.loginWithPin(
                request.getUsername(),
                request.getMobileCountryCode(),
                request.getMobile(),
                request.getPin(),
                request.getImei(),
                request.getFcmToken(),
                request.getPlatform());
        String code = data.isMustSetCredentials() ? "AUTH_MUST_SET_CREDENTIALS"
                : data.isDeviceOtpRequired() ? "AUTH_DEVICE_OTP_REQUIRED"
                : "AUTH_LOGIN_OK";
        String message = data.isMustSetCredentials() ? "Credentials setup required. OTP sent."
                : data.isDeviceOtpRequired() ? "New device detected. OTP sent."
                : "Login successful.";
        return ResponseEntity.ok(ApiSuccessResponses.ok(code, message, data));
    }

    /**
     * Binds device after DEVICE_BIND OTP. No JWT — client returns to login.
     */
    @PostMapping("/verify-device-otp")
    public ResponseEntity<ApiSuccessResponse<AuthResponse>> verifyDeviceOtp(
            @RequestBody VerifyDeviceOtpRequest request) {
        AuthResponse data = authFacade.verifyDeviceOtp(
                request.getMobileCountryCode(),
                request.getMobile(),
                request.getOtpCode(),
                request.getImei(),
                request.getPlatform());
        return ResponseEntity.ok(ApiSuccessResponses.ok("AUTH_DEVICE_BOUND",
                "Device verified. Please log in again.", data));
    }

    /**
     * Verifies CREDENTIALS_MIGRATE OTP and returns JWT for /credentials.
     */
    @PostMapping("/verify-migrate-otp")
    public ResponseEntity<ApiSuccessResponse<AuthResponse>> verifyMigrateOtp(
            @RequestBody VerifyMigrateOtpRequest request) {
        AuthResponse data = authFacade.verifyMigrateOtp(
                request.getMobileCountryCode(),
                request.getMobile(),
                request.getOtpCode());
        return ResponseEntity.ok(ApiSuccessResponses.ok("AUTH_MIGRATE_OTP_VERIFIED",
                "Migration OTP verified.", data));
    }

    /**
     * Sets username + PIN once for existing-user migrate (after verify-migrate-otp).
     * New onboarding must pass username/PIN on {@code POST /kyc/submit} instead.
     */
    @PostMapping("/credentials")
    public ResponseEntity<ApiSuccessResponse<Void>> setCredentials(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestBody SetCredentialsRequest request) {
        authFacade.setCredentials(externalId, request.getUsername(), request.getPin(),
                request.getImei(), request.getPlatform());
        return ResponseEntity.ok(ApiSuccessResponses.ok("AUTH_CREDENTIALS_SET", "Credentials set successfully."));
    }

    /**
     * Changes PIN while authenticated.
     */
    @PostMapping("/change-pin")
    public ResponseEntity<ApiSuccessResponse<Void>> changePin(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestBody ChangePinRequest request) {
        authFacade.changePin(externalId, request.getOldPin(), request.getNewPin());
        return ResponseEntity.ok(ApiSuccessResponses.ok("AUTH_PIN_CHANGED", "PIN changed successfully."));
    }

    /**
     * Sends PIN_RESET OTP to registered mobile.
     */
    @PostMapping("/forgot-pin/request")
    public ResponseEntity<ApiSuccessResponse<Void>> forgotPinRequest(@RequestBody ForgotPinRequest request) {
        authFacade.requestForgotPin(request.getMobileCountryCode(), request.getMobile());
        return ResponseEntity.ok(ApiSuccessResponses.ok("AUTH_PIN_RESET_OTP_SENT", "PIN reset OTP sent."));
    }

    /**
     * Confirms PIN reset with OTP and new PIN.
     */
    @PostMapping("/forgot-pin/confirm")
    public ResponseEntity<ApiSuccessResponse<Void>> forgotPinConfirm(@RequestBody ForgotPinConfirmRequest request) {
        authFacade.confirmForgotPin(
                request.getMobileCountryCode(),
                request.getMobile(),
                request.getOtpCode(),
                request.getNewPin(),
                request.getImei(),
                request.getPlatform());
        return ResponseEntity.ok(ApiSuccessResponses.ok("AUTH_PIN_RESET_OK", "PIN reset successfully."));
    }

    /**
     * Matches device contact mobiles to VycePay users with ACTIVE wallets.
     * Returns username + account title (+ payeeAccountId for later Choice transfer).
     */
    @Operation(summary = "Verify contact mobiles against VycePay customers")
    @PostMapping("/contacts/verify")
    public ResponseEntity<ApiSuccessResponse<VerifyContactsResponse>> verifyContacts(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestBody VerifyContactsRequest request) {
        VerifyContactsResponse data = contactsVerifyService.verify(externalId, request);
        return ResponseEntity.ok(ApiSuccessResponses.ok(
                "AUTH_CONTACTS_VERIFIED", "Contacts verified.", data));
    }

    @GetMapping("/me")
    public ResponseEntity<CustomerProfileResponse> getProfile(
            @RequestHeader("X-Customer-Id") String externalId) {
        Customer customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(new CustomerProfileResponse(
                customer.getExternalId(),
                customer.getMobileCountryCode(),
                customer.getMobile(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getStatus()));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestHeader("X-Customer-Id") String externalId) {
        Customer customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        String newToken = jwtService.createToken(customer.getId(), customer.getExternalId());
        return ResponseEntity.ok(AuthResponse.token(newToken, customer.getExternalId(),
                jwtService.getValiditySeconds(),
                customer.getUsername(), customer.getMobileCountryCode(), customer.getMobile()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiSuccessResponse<Void>> logout(@RequestHeader("X-Customer-Id") String externalId) {
        authFacade.logout(externalId);
        return ResponseEntity.ok(ApiSuccessResponses.ok("AUTH_LOGOUT_OK", "Logout acknowledged."));
    }

    @PostMapping("/devices")
    public ResponseEntity<ApiSuccessResponse<RegisterDeviceResponse>> registerDevice(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestBody RegisterDeviceRequest request) {
        Customer customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        DeviceToken token = deviceTokenRepository.findByCustomerIdAndFcmToken(customer.getId(), request.getFcmToken())
                .map(existing -> {
                    if (request.getPlatform() != null && !request.getPlatform().isBlank()) {
                        existing.setPlatform(request.getPlatform());
                    }
                    return deviceTokenRepository.save(existing);
                })
                .orElseGet(() -> {
                    DeviceToken created = new DeviceToken();
                    created.setCustomerId(customer.getId());
                    created.setFcmToken(request.getFcmToken());
                    created.setPlatform(request.getPlatform());
                    return deviceTokenRepository.save(created);
                });
        RegisterDeviceResponse data = new RegisterDeviceResponse(token.getId(), token.getPlatform());
        return ResponseEntity.ok(ApiSuccessResponses.ok(
                "DEVICE_REGISTERED", "Device registered successfully.", data));
    }

    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<ApiSuccessResponse<Void>> unregisterDevice(
            @RequestHeader("X-Customer-Id") String externalId,
            @PathVariable Long deviceId) {
        Customer customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        deviceTokenRepository.findByIdAndCustomerId(deviceId, customer.getId())
                .ifPresent(deviceTokenRepository::delete);
        return ResponseEntity.ok(ApiSuccessResponses.ok("DEVICE_UNREGISTERED", "Device unregistered successfully."));
    }
}
