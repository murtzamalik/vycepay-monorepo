package com.vycepay.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * VycePay Wallet Service - account mapping, balance cache.
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.vycepay")
public class WalletServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletServiceApplication.class, args);
    }
}
