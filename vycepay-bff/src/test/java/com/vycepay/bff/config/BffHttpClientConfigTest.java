package com.vycepay.bff.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against regressing to HttpURLConnection-based RestTemplate (no PATCH).
 */
class BffHttpClientConfigTest {

    @Test
    void bffProxyRestTemplate_usesJdkClientFactory() {
        RestTemplate restTemplate = new BffHttpClientConfig().bffProxyRestTemplate();
        assertThat(restTemplate.getRequestFactory()).isInstanceOf(JdkClientHttpRequestFactory.class);
    }
}
