package com.vycepay.common.media;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vycepay.common.media.port.MediaStoragePort;

/**
 * Registers {@link MediaStoragePort}: local by default, S3 when {@code vycepay.media.type=s3}.
 */
@Configuration
@EnableConfigurationProperties(MediaProperties.class)
public class MediaStorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MediaStoragePort.class)
    @ConditionalOnProperty(prefix = "vycepay.media", name = "type", havingValue = "local", matchIfMissing = true)
    public MediaStoragePort localMediaStoragePort(MediaProperties properties) {
        return new LocalMediaStorageAdapter(properties.getLocalBasePath());
    }

    @Bean
    @ConditionalOnMissingBean(MediaStoragePort.class)
    @ConditionalOnProperty(prefix = "vycepay.media", name = "type", havingValue = "s3")
    public MediaStoragePort s3MediaStoragePort(MediaProperties properties) {
        return new S3MediaStorageAdapter(properties);
    }
}
