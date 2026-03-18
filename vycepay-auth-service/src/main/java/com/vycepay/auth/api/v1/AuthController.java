package com.vycepay.auth.api.v1;

import com.vycepay.auth.api.v1.dto.AuthResponse;
import com.vycepay.auth.api.v1.dto.CustomerProfileResponse;
import com.vycepay.auth.api.v1.dto.RegisterDeviceRequest;
import com.vycepay.auth.api.v1.dto.RegisterRequest;
import com.vycepay.auth.api.v1.dto.VerifyOtpRequest;
import com.vycepay.auth.application.facade.AuthFacade;
import com.vycepay.auth.application.service.JwtService;
import com.vycepay.auth.domain.model.Customer;
import com.vycepay.auth.domain.model.DeviceToken;
import com.vycepay.auth.infrastructure.persistence.CustomerRepository;
import com.vycepay.auth.infrastructure.persistence.DeviceTokenRepository;
import com.vycepay.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Auth API: registration, OTP verification, login, profile, logout, token refresh.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthFacade authFacade;
    private final JwtService jwtService;
    private final CustomerRepository customerRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    public AuthController(AuthFacade authFacade, JwtService jwtService,
                          CustomerRepository customerRepository,
                          DeviceTokenRepository deviceTokenRepository) {
        this.authFacade = authFacade;
        this.jwtService = jwtService;
        this.customerRepository = customerRepository;
        this.deviceTokenRepository = deviceTokenRepository;
    }

    /**
     * Sends OTP to mobile for registration.
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest request) {
        authFacade.sendOtp(request.getMobileCountryCode(), request.getMobile());
        return ResponseEntity.ok().build();
    }

    /**
     * Verifies OTP and returns JWT. Creates customer if new.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
        String token = authFacade.verifyOtpAndGetToken(
                request.getMobileCountryCode(),
                request.getMobile(),
                request.getOtpCode());
        if (token == null) {
            return ResponseEntity.badRequest().build();
        }
        Customer customer = customerRepository.findByMobileCountryCodeAndMobile(
                request.getMobileCountryCode(), request.getMobile()).orElseThrow();
        return ResponseEntity.ok(new AuthResponse(token, customer.getExternalId(), jwtService.getValiditySeconds()));
    }

    /**
     * Sends OTP for login. Customer must already be registered.
     */
    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody RegisterRequest request) {
        authFacade.login(request.getMobileCountryCode(), request.getMobile());
        return ResponseEntity.ok().build();
    }

    /**
     * Returns the current customer's profile.
     * Requires X-Customer-Id header (set by BFF from JWT).
     */
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

    /**
     * Issues a new JWT for the current customer (token refresh).
     * The BFF has already validated the old token and set X-Customer-Id.
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestHeader("X-Customer-Id") String externalId) {
        Customer customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        String newToken = jwtService.createToken(customer.getId(), customer.getExternalId());
        return ResponseEntity.ok(new AuthResponse(newToken, customer.getExternalId(), jwtService.getValiditySeconds()));
    }

    /**
     * Logout. JWT is stateless; client must discard the token.
     * Returns 200 to acknowledge the logout request.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("X-Customer-Id") String externalId) {
        return ResponseEntity.ok().build();
    }

    /**
     * Registers an FCM device token for push notifications.
     * If the same token already exists for this customer, returns 200 without duplication.
     */
    @PostMapping("/devices")
    public ResponseEntity<Void> registerDevice(
            @RequestHeader("X-Customer-Id") String externalId,
            @RequestBody RegisterDeviceRequest request) {
        Customer customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        deviceTokenRepository.findByCustomerIdAndFcmToken(customer.getId(), request.getFcmToken())
                .orElseGet(() -> {
                    DeviceToken token = new DeviceToken();
                    token.setCustomerId(customer.getId());
                    token.setFcmToken(request.getFcmToken());
                    token.setPlatform(request.getPlatform());
                    return deviceTokenRepository.save(token);
                });
        return ResponseEntity.ok().build();
    }

    /**
     * Unregisters a device FCM token (e.g. on logout or device change).
     */
    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<Void> unregisterDevice(
            @RequestHeader("X-Customer-Id") String externalId,
            @PathVariable Long deviceId) {
        Customer customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        deviceTokenRepository.findByIdAndCustomerId(deviceId, customer.getId())
                .ifPresent(deviceTokenRepository::delete);
        return ResponseEntity.ok().build();
    }
}
