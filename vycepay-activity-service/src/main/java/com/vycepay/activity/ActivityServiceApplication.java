package com.vycepay.activity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * VycePay Activity Service - audit logging, compliance trail.
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.vycepay")
public class ActivityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivityServiceApplication.class, args);
    }
}
