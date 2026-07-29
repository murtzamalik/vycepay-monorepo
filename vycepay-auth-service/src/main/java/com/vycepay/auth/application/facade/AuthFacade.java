package com.vycepay.auth.application.facade;

import com.vycepay.auth.api.v1.dto.AuthResponse;
import com.vycepay.auth.application.service.AuthAuditService;
import com.vycepay.auth.application.service.AuthMetricsService;
import com.vycepay.auth.application.service.AuthRateLimitService;
import com.vycepay.auth.application.service.CustomerDeviceService;
import com.vycepay.auth.application.service.DeviceTokenService;
import com.vycepay.auth.application.service.JwtService;
import com.vycepay.auth.application.service.OtpService;
import com.vycepay.auth.application.service.PinCredentialsService;
import com.vycepay.auth.domain.model.Customer;
import com.vycepay.auth.domain.model.OtpPurpose;
import com.vycepay.auth.infrastructure.persistence.CustomerRepository;
import com.vycepay.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Orchestrates signup OTP, PIN login, device binding, credentials, and forgot-PIN.
 */
@Service
public class AuthFacade {

    private static final Logger log = LoggerFactory.getLogger(AuthFacade.class);
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final CustomerRepository customerRepository;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final DeviceTokenService deviceTokenService;
    private final PinCredentialsService pinCredentialsService;
    private final CustomerDeviceService customerDeviceService;
    private final AuthRateLimitService rateLimitService;
    private final AuthAuditService authAuditService;
    private final AuthMetricsService authMetricsService;

    public AuthFacade(CustomerRepository customerRepository,
                      OtpService otpService,
                      JwtService jwtService,
                      DeviceTokenService deviceTokenService,
                      PinCredentialsService pinCredentialsService,
                      CustomerDeviceService customerDeviceService,
                      AuthRateLimitService rateLimitService,
                      AuthAuditService authAuditService,
                      AuthMetricsService authMetricsService) {
        this.customerRepository = customerRepository;
        this.otpService = otpService;
        this.jwtService = jwtService;
        this.deviceTokenService = deviceTokenService;
        this.pinCredentialsService = pinCredentialsService;
        this.customerDeviceService = customerDeviceService;
        this.rateLimitService = rateLimitService;
        this.authAuditService = authAuditService;
        this.authMetricsService = authMetricsService;
    }

    /**
     * Sends SIGNUP OTP to mobile (registration identity step).
     */
    @Transactional
    public void sendSignupOtp(String mobileCountryCode, String mobile) {
        rateLimitService.check("otp_send", mobileCountryCode + mobile);
        otpService.sendOtp(mobileCountryCode, mobile, OtpPurpose.SIGNUP);
    }

    /**
     * Verifies SIGNUP OTP, creates customer if needed, binds IMEI, optional FCM, returns JWT.
     */
    @Transactional
    public AuthResponse verifySignupOtp(String mobileCountryCode, String mobile, String otpCode,
                                        String imei, String fcmToken, String platform) {
        rateLimitService.check("otp_verify", mobileCountryCode + mobile);
        customerDeviceService.requireImei(imei);
        otpService.verifyOtpOrThrow(mobileCountryCode, mobile, otpCode, OtpPurpose.SIGNUP);

        Customer customer = customerRepository.findByMobileCountryCodeAndMobile(mobileCountryCode, mobile)
                .orElseGet(() -> createCustomer(mobileCountryCode, mobile));

        if (customer.getStatus() == null || !STATUS_ACTIVE.equals(customer.getStatus())) {
            customer.setStatus(STATUS_ACTIVE);
            customerRepository.save(customer);
        }

        customerDeviceService.bindOrReplace(customer.getId(), imei, platform);
        deviceTokenService.replaceTokenForCustomer(customer.getId(), fcmToken, platform);

        String token = jwtService.createToken(customer.getId(), customer.getExternalId());
        log.info("Signup OTP verified externalId={}", customer.getExternalId());
        return AuthResponse.token(token, customer.getExternalId(), jwtService.getValiditySeconds());
    }

    /**
     * PIN login with username or mobile. Returns JWT if device bound; otherwise deviceOtpRequired.
     * If credentials not set, starts migrate OTP flow.
     */
    @Transactional
    public AuthResponse loginWithPin(String username, String mobileCountryCode, String mobile,
                                     String pin, String imei, String fcmToken, String platform) {
        customerDeviceService.requireImei(imei);
        Customer customer = resolveCustomerForLogin(username, mobileCountryCode, mobile);
        String rateKey = customer.getId().toString();
        rateLimitService.check("login", rateKey);

        if (!customer.hasCredentials()) {
            if (username != null && !username.isBlank()
                    && (mobile == null || mobile.isBlank())) {
                throw new BusinessException("CREDENTIALS_NOT_SET_USE_MOBILE",
                        "Credentials not set. Log in with mobile number to complete setup.",
                        HttpStatus.CONFLICT);
            }
            rateLimitService.check("otp_send", customer.getMobileCountryCode() + customer.getMobile());
            otpService.sendOtp(customer.getMobileCountryCode(), customer.getMobile(), OtpPurpose.CREDENTIALS_MIGRATE);
            authAuditService.record(customer.getId(), "CREDENTIALS_MIGRATE", "OTP_SENT",
                    OtpService.maskMobile(customer.getMobile()), null);
            return AuthResponse.mustSetCredentials(customer.getExternalId(),
                    customer.getMobileCountryCode(), customer.getMobile(),
                    OtpService.maskMobile(customer.getMobile()));
        }

        pinCredentialsService.verifyPinOrThrow(customer, pin);

        if (customerDeviceService.isDeviceBound(customer.getId(), imei)) {
            deviceTokenService.replaceTokenForCustomer(customer.getId(), fcmToken, platform);
            String token = jwtService.createToken(customer.getId(), customer.getExternalId());
            authMetricsService.incrementLoginSuccess();
            authAuditService.record(customer.getId(), "LOGIN", "SUCCESS",
                    customer.getUsernameNormalized(), "device_matched");
            return AuthResponse.token(token, customer.getExternalId(), jwtService.getValiditySeconds());
        }

        rateLimitService.check("otp_send", customer.getMobileCountryCode() + customer.getMobile());
        otpService.sendOtp(customer.getMobileCountryCode(), customer.getMobile(), OtpPurpose.DEVICE_BIND);
        authMetricsService.incrementDeviceOtpRequired();
        authAuditService.record(customer.getId(), "LOGIN", "DEVICE_OTP_REQUIRED",
                customer.getUsernameNormalized(), null);
        return AuthResponse.deviceOtpRequired(customer.getExternalId(),
                customer.getMobileCountryCode(), customer.getMobile(),
                OtpService.maskMobile(customer.getMobile()));
    }

    /**
     * Verifies DEVICE_BIND OTP and replaces bound IMEI. No JWT — client returns to login.
     */
    @Transactional
    public AuthResponse verifyDeviceOtp(String mobileCountryCode, String mobile, String otpCode,
                                        String imei, String platform) {
        rateLimitService.check("otp_verify", mobileCountryCode + mobile);
        customerDeviceService.requireImei(imei);
        Customer customer = customerRepository.findByMobileCountryCodeAndMobile(mobileCountryCode, mobile)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_REGISTERED",
                        "Customer not registered", HttpStatus.NOT_FOUND));
        otpService.verifyOtpOrThrow(mobileCountryCode, mobile, otpCode, OtpPurpose.DEVICE_BIND);
        customerDeviceService.bindOrReplace(customer.getId(), imei, platform);
        return AuthResponse.deviceBound();
    }

    /**
     * Verifies CREDENTIALS_MIGRATE OTP and returns JWT so client can call /credentials.
     */
    @Transactional
    public AuthResponse verifyMigrateOtp(String mobileCountryCode, String mobile, String otpCode) {
        rateLimitService.check("otp_verify", mobileCountryCode + mobile);
        Customer customer = customerRepository.findByMobileCountryCodeAndMobile(mobileCountryCode, mobile)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_REGISTERED",
                        "Customer not registered", HttpStatus.NOT_FOUND));
        if (customer.hasCredentials()) {
            throw new BusinessException("CREDENTIALS_ALREADY_SET", "Credentials already set", HttpStatus.CONFLICT);
        }
        otpService.verifyOtpOrThrow(mobileCountryCode, mobile, otpCode, OtpPurpose.CREDENTIALS_MIGRATE);
        String token = jwtService.createToken(customer.getId(), customer.getExternalId());
        return AuthResponse.token(token, customer.getExternalId(), jwtService.getValiditySeconds());
    }

    /**
     * Sets username + PIN once for migrate gate. Same username when already set is a no-op.
     * Onboarding credentials are set via KYC submit.
     * Optionally binds IMEI if provided and unbound.
     */
    @Transactional
    public void setCredentials(String externalId, String username, String pin, String imei, String platform) {
        Customer customer = requireCustomer(externalId);
        pinCredentialsService.setCredentials(customer, username, pin);
        if (imei != null && !imei.isBlank() && !customerDeviceService.hasDevice(customer.getId())) {
            customerDeviceService.bindOrReplace(customer.getId(), imei, platform);
        }
    }

    @Transactional
    public void changePin(String externalId, String oldPin, String newPin) {
        Customer customer = requireCustomer(externalId);
        pinCredentialsService.changePin(customer, oldPin, newPin);
    }

    @Transactional
    public void requestForgotPin(String mobileCountryCode, String mobile) {
        rateLimitService.check("forgot_pin", mobileCountryCode + mobile);
        Customer customer = customerRepository.findByMobileCountryCodeAndMobile(mobileCountryCode, mobile)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_REGISTERED",
                        "Customer not registered", HttpStatus.NOT_FOUND));
        if (!customer.hasCredentials()) {
            throw new BusinessException("CREDENTIALS_NOT_SET",
                    "Credentials not set. Complete setup first.", HttpStatus.CONFLICT);
        }
        otpService.sendOtp(mobileCountryCode, mobile, OtpPurpose.PIN_RESET);
        authAuditService.record(customer.getId(), "PIN_RESET", "OTP_SENT",
                OtpService.maskMobile(mobile), null);
    }

    @Transactional
    public void confirmForgotPin(String mobileCountryCode, String mobile, String otpCode,
                                 String newPin, String imei, String platform) {
        rateLimitService.check("otp_verify", mobileCountryCode + mobile);
        Customer customer = customerRepository.findByMobileCountryCodeAndMobile(mobileCountryCode, mobile)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_REGISTERED",
                        "Customer not registered", HttpStatus.NOT_FOUND));
        otpService.verifyOtpOrThrow(mobileCountryCode, mobile, otpCode, OtpPurpose.PIN_RESET);
        pinCredentialsService.resetPin(customer, newPin);
        if (imei != null && !imei.isBlank() && !customerDeviceService.hasDevice(customer.getId())) {
            customerDeviceService.bindOrReplace(customer.getId(), imei, platform);
        }
    }

    @Transactional
    public void logout(String externalId) {
        Customer customer = requireCustomer(externalId);
        deviceTokenService.clearTokensForCustomer(customer.getId());
    }

    private Customer resolveCustomerForLogin(String username, String mobileCountryCode, String mobile) {
        boolean hasUsername = username != null && !username.isBlank();
        boolean hasMobile = mobile != null && !mobile.isBlank()
                && mobileCountryCode != null && !mobileCountryCode.isBlank();
        if (hasUsername) {
            return customerRepository.findByUsernameNormalized(username.trim().toLowerCase())
                    .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS",
                            "Invalid credentials", HttpStatus.UNAUTHORIZED));
        }
        if (hasMobile) {
            return customerRepository.findByMobileCountryCodeAndMobile(mobileCountryCode, mobile)
                    .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_REGISTERED",
                            "Customer not registered", HttpStatus.NOT_FOUND));
        }
        throw new BusinessException("INVALID_CREDENTIALS",
                "Username or mobile is required", HttpStatus.BAD_REQUEST);
    }

    private Customer requireCustomer(String externalId) {
        return customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
    }

    private Customer createCustomer(String mobileCountryCode, String mobile) {
        Customer c = new Customer();
        c.setExternalId(UUID.randomUUID().toString());
        c.setMobileCountryCode(mobileCountryCode);
        c.setMobile(mobile);
        c.setStatus(STATUS_ACTIVE);
        return customerRepository.save(c);
    }
}
