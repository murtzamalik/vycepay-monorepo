package com.vycepay.bff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Outbound HTTP for BFF → backend proxy.
 * Uses JDK {@link HttpClient} so PATCH/DELETE are supported
 * (default {@code HttpURLConnection} rejects PATCH with ProtocolException).
 */
@Configuration
public class BffHttpClientConfig {

    @Bean
    public RestTemplate bffProxyRestTemplate() {
        // NORMAL matches prior HttpURLConnection redirect behaviour (JDK HttpClient defaults to NEVER).
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        return new RestTemplate(requestFactory);
    }
}
