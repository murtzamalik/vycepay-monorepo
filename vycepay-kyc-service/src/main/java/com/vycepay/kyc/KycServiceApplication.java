package com.vycepay.kyc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * VycePay KYC Service - onboarding, Choice Bank submit, KYC status.
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.vycepay")
public class KycServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KycServiceApplication.class, args);
    }
}
