package com.vycepay.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * VycePay Transaction Service - transfer, deposit, history.
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.vycepay")
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
