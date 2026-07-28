package com.vycepay.auth.application.service;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vycepay.auth.domain.model.CustomerDevice;
import com.vycepay.auth.infrastructure.persistence.CustomerDeviceRepository;
import com.vycepay.common.exception.BusinessException;

/**
 * Enforces one bound device (IMEI fingerprint) per customer.
 * Re-bind replaces the previous IMEI.
 */
@Service
public class CustomerDeviceService {

    private final CustomerDeviceRepository deviceRepository;
    private final AuthAuditService authAuditService;
    private final AuthMetricsService authMetricsService;

    public CustomerDeviceService(CustomerDeviceRepository deviceRepository,
                                 AuthAuditService authAuditService,
                                 AuthMetricsService authMetricsService) {
        this.deviceRepository = deviceRepository;
        this.authAuditService = authAuditService;
        this.authMetricsService = authMetricsService;
    }

    public void requireImei(String imei) {
        if (imei == null || imei.isBlank()) {
            throw new BusinessException("IMEI_REQUIRED", "Device IMEI is required", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * @return true if customer has a bound device and IMEI matches
     */
    public boolean isDeviceBound(Long customerId, String imei) {
        return deviceRepository.findByCustomerId(customerId)
                .map(d -> d.getImei().equals(imei))
                .orElse(false);
    }

    public boolean hasDevice(Long customerId) {
        return deviceRepository.findByCustomerId(customerId).isPresent();
    }

    /**
     * Creates or replaces the single device row for the customer.
     */
    @Transactional
    public void bindOrReplace(Long customerId, String imei, String platform) {
        requireImei(imei);
        CustomerDevice device = deviceRepository.findByCustomerId(customerId).orElseGet(CustomerDevice::new);
        boolean isNew = device.getId() == null;
        device.setCustomerId(customerId);
        device.setImei(imei.trim());
        if (platform != null && !platform.isBlank()) {
            device.setPlatform(platform.trim().toUpperCase());
        }
        device.setBoundAt(Instant.now());
        deviceRepository.save(device);
        authMetricsService.incrementDeviceBind();
        authAuditService.record(customerId, "DEVICE_BIND", "SUCCESS",
                null, isNew ? "first_bind" : "rebind");
    }
}
