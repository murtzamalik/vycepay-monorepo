package com.vycepay.auth.application.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

/**
 * Micrometer counters for auth security monitoring.
 */
@Service
public class AuthMetricsService {

    private final Counter loginSuccess;
    private final Counter loginFailure;
    private final Counter deviceOtpRequired;
    private final Counter pinLockout;
    private final Counter deviceBind;
    private final Counter pinReset;
    private final Counter credentialsSet;

    public AuthMetricsService(MeterRegistry registry) {
        this.loginSuccess = Counter.builder("auth.login.success").register(registry);
        this.loginFailure = Counter.builder("auth.login.failure").register(registry);
        this.deviceOtpRequired = Counter.builder("auth.login.device_otp_required").register(registry);
        this.pinLockout = Counter.builder("auth.pin.lockout").register(registry);
        this.deviceBind = Counter.builder("auth.device.bind").register(registry);
        this.pinReset = Counter.builder("auth.pin.reset").register(registry);
        this.credentialsSet = Counter.builder("auth.credentials.set").register(registry);
    }

    public void incrementLoginSuccess() {
        loginSuccess.increment();
    }

    public void incrementLoginFailure() {
        loginFailure.increment();
    }

    public void incrementDeviceOtpRequired() {
        deviceOtpRequired.increment();
    }

    public void incrementPinLockout() {
        pinLockout.increment();
    }

    public void incrementDeviceBind() {
        deviceBind.increment();
    }

    public void incrementPinReset() {
        pinReset.increment();
    }

    public void incrementCredentialsSet() {
        credentialsSet.increment();
    }
}
