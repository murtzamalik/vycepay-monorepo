package com.vycepay.admin;

import com.vycepay.common.config.SmsClientConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * Boots the VycePay backoffice admin service.
 * This service owns admin identity/RBAC tables and exposes controlled read and mutation APIs.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ComponentScan(basePackages = {"com.vycepay.admin", "com.vycepay.common.exception"})
@Import(SmsClientConfig.class)
public class AdminServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}
