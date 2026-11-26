package pe.sga.functional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

@Tag("sistema")
@TestPropertySource(properties = {
        "management.server.port=0",
        "management.server.address=127.0.0.1",
        "management.endpoints.web.exposure.include=health,prometheus",
        "management.metrics.tags.service=sga-backend",
        "management.metrics.distribution.percentiles-histogram.http.server.requests=true"
})
class MonitoringEndpointsTest extends FullStackTestBase {
    @Value("${local.management.port}") int monitoringPort;

    HttpResponse<String> monitoring(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + monitoringPort + path))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void prometheusExposesRealJvmAndHttpMetrics() throws Exception {
        assertEquals(200, call("GET", "/api/v1/catalogos/puertos", null, null).statusCode());
        var r = monitoring("/actuator/prometheus");
        assertEquals(200, r.statusCode(), r.body());
        assertTrue(r.body().contains("jvm_memory_used_bytes"));
        assertTrue(r.body().contains("http_server_requests_seconds_count"));
        assertTrue(r.body().contains("service=\"sga-backend\""));
        assertTrue(r.body().contains("http_server_requests_seconds_bucket"));
    }

    @Test void applicationEndpointsStillRequireAuthentication() throws Exception {
        assertEquals(401, call("GET", "/api/v1/clientes", null, null).statusCode());
        assertEquals(401, call("GET", "/actuator/prometheus", null, null).statusCode());
    }

    @Test void managementDoesNotExposeEnvironmentOrConfiguration() throws Exception {
        var r = monitoring("/actuator/env");
        assertTrue(r.statusCode() == 403 || r.statusCode() == 404, r.body());
        assertFalse(r.body().contains("DB_PASSWORD"));
        assertFalse(r.body().contains("JWT_SECRET"));
    }
}
