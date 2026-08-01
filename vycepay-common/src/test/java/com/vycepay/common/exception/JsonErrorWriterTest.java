package com.vycepay.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class JsonErrorWriterTest {

    @Test
    void writeProducesEnvelopeWithRequestId() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        JsonErrorWriter.write(response, 401, "UNAUTHORIZED", "Please sign in again to continue.");
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString())
                .contains("\"code\":\"UNAUTHORIZED\"")
                .contains("\"message\":\"Please sign in again to continue.\"")
                .contains("\"requestId\":");
    }

    @Test
    void toBytesNeverEmitsBackendError() {
        String json = new String(JsonErrorWriter.toBytes("UPSTREAM_ERROR", "safe", "rid-1"));
        assertThat(json).contains("UPSTREAM_ERROR").contains("rid-1").doesNotContain("BACKEND_ERROR");
    }
}
