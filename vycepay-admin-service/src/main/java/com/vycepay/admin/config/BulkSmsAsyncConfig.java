package com.vycepay.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Bounded executor for all-customers bulk SMS dispatch (keeps HTTP path non-blocking).
 * {@link EnableAsync} is declared here as the admin-service async entry point.
 */
@Configuration
@EnableAsync
public class BulkSmsAsyncConfig {

    public static final String BULK_SMS_EXECUTOR = "bulkSmsExecutor";

    @Bean(name = BULK_SMS_EXECUTOR)
    public Executor bulkSmsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("bulk-sms-");
        // Prefer running on the caller than dropping a batch after rows were enqueued.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
