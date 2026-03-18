package com.vycepay.auth.application.facade;

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
 * Orchestrates registration, OTP verification, and login flows.
 */
@Service
public class AuthFacade {

    private static final Logger log = LoggerFactory.getLogger(AuthFacade.class);
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final CustomerRepository customerRepository;
    private final OtpService otpService;
    private final JwtService jwtService;

    public AuthFacade(CustomerRepository customerRepository, OtpService otpService, JwtService jwtService) {
        this.customerRepository = customerRepository;
        this.otpService = otpService;
        this.jwtService = jwtService;
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
     * Creates customer if new; returns JWT.
     *
     * @param mobileCountryCode Country code
     * @param mobile            Mobile number
     * @param otpCode           OTP from user
     * @return JWT if verification succeeds, null otherwise
     */
    @Transactional
    public String verifyOtpAndGetToken(String mobileCountryCode, String mobile, String otpCode) {
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

    private Customer createCustomer(String mobileCountryCode, String mobile) {
        Customer c = new Customer();
        c.setExternalId(UUID.randomUUID().toString());
        c.setMobileCountryCode(mobileCountryCode);
        c.setMobile(mobile);
        c.setStatus(STATUS_ACTIVE);
        return customerRepository.save(c);
    }
}
