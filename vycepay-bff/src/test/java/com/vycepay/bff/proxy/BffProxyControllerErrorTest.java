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
    void emptyUpstream401_returnsAuthFailed() {
        server.expect(requestTo("http://localhost:8082/api/v1/auth/login"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body(""));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        ResponseEntity<byte[]> response = controller.proxy(request, "{}".getBytes(StandardCharsets.UTF_8));

        String body = new String(response.getBody(), StandardCharsets.UTF_8);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body).contains("\"code\":\"AUTH_FAILED\"");
        assertThat(body).contains("sign you in");
        server.verify();
    }

    @Test
    void emptyUpstream404OnAuth_returnsCustomerNotRegistered() {
        server.expect(requestTo("http://localhost:8082/api/v1/auth/login"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body(""));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        ResponseEntity<byte[]> response = controller.proxy(request, "{}".getBytes(StandardCharsets.UTF_8));

        String body = new String(response.getBody(), StandardCharsets.UTF_8);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body).contains("\"code\":\"CUSTOMER_NOT_REGISTERED\"");
        server.verify();
    }

    @Test
    void mapEmptyUpstreamCode_statusMatrix() {
        assertThat(BffProxyController.mapEmptyUpstreamCode(HttpStatus.UNAUTHORIZED, "auth/login"))
                .isEqualTo("AUTH_FAILED");
        assertThat(BffProxyController.mapEmptyUpstreamCode(HttpStatus.NOT_FOUND, "auth/login"))
                .isEqualTo("CUSTOMER_NOT_REGISTERED");
        assertThat(BffProxyController.mapEmptyUpstreamCode(HttpStatus.NOT_FOUND, "wallets/me"))
                .isEqualTo("NOT_FOUND");
        assertThat(BffProxyController.mapEmptyUpstreamCode(HttpStatus.LOCKED, "auth/login"))
                .isEqualTo("ACCOUNT_LOCKED");
        assertThat(BffProxyController.mapEmptyUpstreamCode(HttpStatus.TOO_MANY_REQUESTS, "auth/login"))
                .isEqualTo("RATE_LIMITED");
        assertThat(BffProxyController.mapEmptyUpstreamCode(HttpStatus.BAD_GATEWAY, "auth/login"))
                .isEqualTo("BAD_GATEWAY");
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
