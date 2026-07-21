package com.vycepay.auth.application.facade;

import com.vycepay.auth.application.service.DeviceTokenService;
import com.vycepay.auth.application.service.JwtService;
import com.vycepay.auth.application.service.OtpService;
import com.vycepay.auth.domain.model.Customer;
import com.vycepay.common.exception.BusinessException;
import com.vycepay.auth.infrastructure.persistence.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Orchestrates registration, OTP verification, login, and FCM token binding.
 */
@Service
public class AuthFacade {

    private static final Logger log = LoggerFactory.getLogger(AuthFacade.class);
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final CustomerRepository customerRepository;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final DeviceTokenService deviceTokenService;

    public AuthFacade(CustomerRepository customerRepository, OtpService otpService, JwtService jwtService,
                      DeviceTokenService deviceTokenService) {
        this.customerRepository = customerRepository;
        this.otpService = otpService;
        this.jwtService = jwtService;
        this.deviceTokenService = deviceTokenService;
    }

    /**
     * Sends OTP to mobile for registration. If customer exists, sends for login.
     *
     * @param mobileCountryCode Country code (e.g. 254)
     * @param mobile            Mobile number
     */
    @Transactional
    public void sendOtp(String mobileCountryCode, String mobile) {
        otpService.sendOtp(mobileCountryCode, mobile);
    }

    /**
     * Verifies OTP and completes registration or login.
     * Creates customer if new; optionally binds FCM token (one device per customer); returns JWT.
     *
     * @param mobileCountryCode Country code
     * @param mobile            Mobile number
     * @param otpCode           OTP from user
     * @param fcmToken          Optional FCM token from Firebase SDK
     * @param platform          Optional ANDROID/IOS; defaults to ANDROID when fcmToken set
     * @return JWT if verification succeeds, null otherwise
     */
    @Transactional
    public String verifyOtpAndGetToken(String mobileCountryCode, String mobile, String otpCode,
                                       String fcmToken, String platform) {
        if (!otpService.verifyOtp(mobileCountryCode, mobile, otpCode)) {
            log.warn("Invalid OTP for {} {}", mobileCountryCode, mobile);
            return null;
        }

        Customer customer = customerRepository.findByMobileCountryCodeAndMobile(mobileCountryCode, mobile)
                .orElseGet(() -> createCustomer(mobileCountryCode, mobile));

        if (customer.getStatus() == null || !STATUS_ACTIVE.equals(customer.getStatus())) {
            customer.setStatus(STATUS_ACTIVE);
            customerRepository.save(customer);
        }

        deviceTokenService.replaceTokenForCustomer(customer.getId(), fcmToken, platform);

        return jwtService.createToken(customer.getId(), customer.getExternalId());
    }

    /**
     * Login: sends OTP. Client then calls verify-otp to get JWT.
     */
    @Transactional
    public void login(String mobileCountryCode, String mobile) {
        if (customerRepository.findByMobileCountryCodeAndMobile(mobileCountryCode, mobile).isEmpty()) {
            throw new BusinessException("CUSTOMER_NOT_REGISTERED", "Customer not registered", HttpStatus.NOT_FOUND);
        }
        otpService.sendOtp(mobileCountryCode, mobile);
    }

    /**
     * Clears all FCM tokens for the customer on logout.
     */
    @Transactional
    public void logout(String externalId) {
        Customer customer = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND));
        deviceTokenService.clearTokensForCustomer(customer.getId());
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
