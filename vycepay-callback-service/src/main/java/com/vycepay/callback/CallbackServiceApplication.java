package com.vycepay.callback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * VycePay Callback Service - receives Choice Bank webhooks.
 */
@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = "com.vycepay")
public class CallbackServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CallbackServiceApplication.class, args);
    }
}
