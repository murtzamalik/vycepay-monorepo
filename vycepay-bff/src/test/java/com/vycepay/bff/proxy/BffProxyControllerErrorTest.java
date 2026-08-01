package com.vycepay.bff.proxy;

import com.vycepay.bff.config.BffBackendProperties;
import com.vycepay.common.exception.VyceErrorCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class BffProxyControllerErrorTest {

    private BffProxyController controller;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() throws Exception {
        VyceErrorCatalog catalog = new VyceErrorCatalog();
        catalog.loadFromClasspath();
        BffBackendProperties backend = new BffBackendProperties();
        backend.setAuthUrl("http://localhost:8082");
        controller = new BffProxyController(backend, catalog);
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        ReflectionTestUtils.setField(controller, "restTemplate", restTemplate);
    }

    @Test
    void emptyUpstreamBody_returnsUpstreamErrorEnvelope() {
        server.expect(requestTo("http://localhost:8082/api/v1/auth/login"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(""));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        ResponseEntity<byte[]> response = controller.proxy(request, "{}".getBytes(StandardCharsets.UTF_8));

        String body = new String(response.getBody(), StandardCharsets.UTF_8);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body).contains("\"code\":\"UPSTREAM_ERROR\"");
        assertThat(body).contains("\"message\":");
        assertThat(body).contains("\"requestId\":");
        assertThat(body).doesNotContain("BACKEND_ERROR");
        server.verify();
    }

    @Test
    void unknownPrefix_returnsNotFoundEnvelope() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/unknown/thing");
        ResponseEntity<byte[]> response = controller.proxy(request, null);

        String body = new String(response.getBody(), StandardCharsets.UTF_8);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body).contains("\"code\":\"NOT_FOUND\"");
        assertThat(body).contains("\"requestId\":");
    }
}
